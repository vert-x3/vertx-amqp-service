/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.ext.amqp.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.amqp.AmqpEvent;
import io.vertx.ext.amqp.Connection;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.EventType;
import io.vertx.ext.amqp.IncomingLink;
import io.vertx.ext.amqp.OutgoingLink;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.Session;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.message.Message;

class ConnectionImpl implements Connection
{
    enum State
    {
        NEW, CONNECTED, CLOSED, FAILED, RETRY_IN_PROGRESS
    };

    private static final Logger _logger = LoggerFactory.getLogger(ConnectionImpl.class);

    protected final org.apache.qpid.proton.engine.Connection protonConnection;

    private final Transport _transport;

    protected final Collector _collector;

    private final ArrayList<Handler<ConnectionImpl>> disconnectHandlers = new ArrayList<Handler<ConnectionImpl>>();

    private final Object _lock = new Object();

    private final Handler<AmqpEvent> _eventHandler;

    private final ConnectionSettings _settings;

    private final boolean _isInbound;

    private final String _toString;

    private NetSocket _socket;

    private State _state = State.NEW;

    ConnectionImpl(ConnectionSettings settings, Handler<AmqpEvent> handler, boolean inbound)
    {
        _toString = "amqp://" + settings.getHost() + ":" + settings.getPort();
        _isInbound = inbound;
        _settings = settings;
        _eventHandler = handler;

        protonConnection = org.apache.qpid.proton.engine.Connection.Factory.create();
        _transport = org.apache.qpid.proton.engine.Transport.Factory.create();
        _collector = Collector.Factory.create();

        protonConnection.setContext(this);
        protonConnection.collect(_collector);
        Sasl sasl = _transport.sasl();
        if (inbound)
        {
            sasl.server();
            sasl.setMechanisms(new String[] { "ANONYMOUS" });
            sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
        }
        else
        {
            sasl.client();
            sasl.setMechanisms(new String[] { "ANONYMOUS" });
        }
        _transport.bind(protonConnection);
    }

    ConnectionSettings getSettings()
    {
        return _settings;
    }

    void addDisconnectHandler(Handler<ConnectionImpl> handler)
    {
        disconnectHandlers.add(handler);
    }

    void setNetSocket(NetSocket s)
    {
        synchronized (_lock)
        {
            _socket = s;
            _socket.handler(data -> {
                byte[] bytes = data.getBytes();
                int start = 0;
                while (start < bytes.length)
                {
                    int count = Math.min(_transport.getInputBuffer().remaining(), bytes.length - start);
                    _transport.getInputBuffer().put(bytes, start, count);
                    start += count;
                    _transport.processInput();
                    processEvents();
                }
                write();
            });

            _socket.drainHandler(v -> {
                write();
            });

            _socket.endHandler(v -> {
                if (getState() != State.CLOSED)
                {
                    _logger.info(String.format(
                            "Received EOF for connection {%s:%s}, prev-state = %s. Setting state to FAILED",
                            _settings.getHost(), _settings.getPort(), getState()));
                    setState(State.FAILED);
                }

                for (Handler<ConnectionImpl> h : disconnectHandlers)
                {
                    h.handle(this);
                }
            });
            _state = State.CONNECTED;
        }
        write();
    }

    void setState(State state)
    {
        synchronized (_lock)
        {
            _state = state;
        }
    }

    @Override
    public void open()
    {
        protonConnection.open();
        write();
    }

    @Override
    public boolean isOpen()
    {
        synchronized (_lock)
        {
            return _state == State.CONNECTED && protonConnection.getLocalState() == EndpointState.ACTIVE
                    && protonConnection.getRemoteState() == EndpointState.ACTIVE;
        }
    }

    State getState()
    {
        synchronized (_lock)
        {
            return _state;
        }
    }

    @Override
    public boolean isInbound()
    {
        return _isInbound;
    }

    @Override
    public void close()
    {
        setState(State.CLOSED);
        protonConnection.close();
    }

    protected void processEvents()
    {
        protonConnection.collect(_collector);
        Event event = _collector.peek();
        while (event != null)
        {
            switch (event.getType())
            {
            case CONNECTION_REMOTE_OPEN:
                AmqpEventImpl amqpEvent = new AmqpEventImpl(EventType.CONNECTION_READY);
                amqpEvent.setConnection(this);
                _eventHandler.handle(amqpEvent);
                break;
            case CONNECTION_FINAL:
                amqpEvent = new AmqpEventImpl(EventType.CONNECTION_FINAL);
                amqpEvent.setConnection(this);
                _eventHandler.handle(amqpEvent);
                break;
            case SESSION_REMOTE_OPEN:
                Session ssn;
                org.apache.qpid.proton.engine.Session amqpSsn = event.getSession();
                if (amqpSsn.getContext() != null)
                {
                    ssn = (Session) amqpSsn.getContext();
                }
                else
                {
                    ssn = new SessionImpl(this, amqpSsn);
                    amqpSsn.setContext(ssn);
                    event.getSession().open();
                }
                amqpEvent = new AmqpEventImpl(EventType.SESSION_READY);
                amqpEvent.setConnection(this);
                amqpEvent.setSession(ssn);
                _eventHandler.handle(amqpEvent);
                break;
            case SESSION_FINAL:
                ssn = (Session) event.getSession().getContext();
                amqpEvent = new AmqpEventImpl(EventType.SESSION_FINAL);
                amqpEvent.setConnection(this);
                amqpEvent.setSession(ssn);
                _eventHandler.handle(amqpEvent);
                break;
            case LINK_REMOTE_OPEN:
                Link link = event.getLink();
                if (link instanceof Receiver)
                {
                    IncomingLinkImpl inboundLink;
                    SessionImpl session = (SessionImpl) link.getSession().getContext();
                    if (link.getContext() != null)
                    {
                        inboundLink = (IncomingLinkImpl) link.getContext();
                    }
                    else
                    {
                        inboundLink = new IncomingLinkImpl(session, link.getRemoteTarget().getAddress(), link,
                                ReliabilityMode.AT_LEAST_ONCE, CreditMode.AUTO);
                        link.setContext(inboundLink);
                        inboundLink.init();
                    }
                    amqpEvent = new AmqpEventImpl(EventType.INCOMING_LINK_READY);
                    amqpEvent.setConnection(this);
                    amqpEvent.setSession(session);
                    amqpEvent.setLink(inboundLink);
                    _eventHandler.handle(amqpEvent);
                }
                else
                {
                    OutgoingLinkImpl outboundLink;
                    SessionImpl session = (SessionImpl) link.getSession().getContext();
                    if (link.getContext() != null)
                    {
                        outboundLink = (OutgoingLinkImpl) link.getContext();
                    }
                    else
                    {
                        outboundLink = new OutgoingLinkImpl(session, link.getRemoteSource().getAddress(), link);
                        link.setContext(outboundLink);
                        outboundLink.init();
                    }
                    amqpEvent = new AmqpEventImpl(EventType.OUTGOING_LINK_READY);
                    amqpEvent.setConnection(this);
                    amqpEvent.setSession(session);
                    amqpEvent.setLink(outboundLink);
                    _eventHandler.handle(amqpEvent);
                }
                break;
            case LINK_FLOW:
                link = event.getLink();
                if (link instanceof Sender)
                {
                    OutgoingLink outboundLink = (OutgoingLink) link.getContext();
                    amqpEvent = new AmqpEventImpl(EventType.OUTGOING_LINK_CREDIT);
                    amqpEvent.setConnection(this);
                    amqpEvent.setSession((SessionImpl) link.getSession().getContext());
                    amqpEvent.setLink(outboundLink);
                    _eventHandler.handle(amqpEvent);
                }
                break;
            case LINK_FINAL:
                link = event.getLink();
                if (link instanceof Receiver)
                {
                    IncomingLink inboundLink = (IncomingLink) link.getContext();
                    amqpEvent = new AmqpEventImpl(EventType.INCOMING_LINK_FINAL);
                    amqpEvent.setConnection(this);
                    amqpEvent.setSession((SessionImpl) link.getSession().getContext());
                    amqpEvent.setLink(inboundLink);
                    _eventHandler.handle(amqpEvent);
                }
                else
                {
                    OutgoingLinkImpl outboundLink = (OutgoingLinkImpl) link.getContext();
                    amqpEvent = new AmqpEventImpl(EventType.OUTGOING_LINK_FINAL);
                    amqpEvent.setConnection(this);
                    amqpEvent.setSession((SessionImpl) link.getSession().getContext());
                    amqpEvent.setLink(outboundLink);
                    _eventHandler.handle(amqpEvent);
                }
                break;
            case TRANSPORT:
                // TODO
                break;
            case DELIVERY:
                onDelivery(event.getDelivery());
                break;
            default:
                break;
            }
            _collector.pop();
            event = _collector.peek();
        }
    }

    void onDelivery(Delivery d)
    {
        Link link = d.getLink();
        if (link instanceof Receiver)
        {
            if (d.isPartial())
            {
                return;
            }

            Receiver receiver = (Receiver) link;
            byte[] bytes = new byte[d.pending()];
            int read = receiver.recv(bytes, 0, bytes.length);
            Message pMsg = Proton.message();
            pMsg.decode(bytes, 0, read);
            receiver.advance();

            IncomingLinkImpl inLink = (IncomingLinkImpl) link.getContext();
            SessionImpl ssn = inLink.getSession();
            AmqpMessageImpl msg = new InboundMessage(ssn.getID(), d.getTag(), ssn.getNextIncommingSequence(),
                    d.isSettled(), pMsg);
            try
            {
                AmqpEventImpl amqpEvent = new AmqpEventImpl(EventType.MESSAGE_RECEIVED);
                amqpEvent.setConnection(this);
                amqpEvent.setSession((SessionImpl) link.getSession().getContext());
                amqpEvent.setLink(inLink);
                amqpEvent.setMessage(msg);
                _eventHandler.handle(amqpEvent);
            }
            catch (Exception e)
            {
                _logger.warn("Unexpected error while routing inbound message into event bus", e);
            }
        }
        else
        {
            if (d.remotelySettled())
            {
                TrackerImpl tracker = (TrackerImpl) d.getContext();
                tracker.setDisposition(d.getRemoteState());
                tracker.markSettled();
                AmqpEventImpl amqpEvent = new AmqpEventImpl(EventType.MESSAGE_SETTLED);
                amqpEvent.setConnection(this);
                amqpEvent.setSession((SessionImpl) d.getLink().getSession().getContext());
                amqpEvent.setLink((io.vertx.ext.amqp.Link) d.getLink());
                amqpEvent.setTracker(tracker);
                _eventHandler.handle(amqpEvent);
            }
        }
    }

    void write()
    {
        synchronized (_lock)
        {
            if (_state != State.CONNECTED)
            {
                if (_logger.isDebugEnabled())
                {
                    _logger.debug(String.format("Connection {%s:%s}, state = %s. Returning without writing",
                            _settings.getHost(), _settings.getPort(), _state));
                }
                return;
            }
        }

        if (_socket.writeQueueFull())
        {
            System.out.println("Socket buffers are full for " + this);
        }
        else
        {
            ByteBuffer b = _transport.getOutputBuffer();
            while (b.remaining() > 0)
            {
                // TODO: what is the optimal solution here?
                byte[] data = new byte[b.remaining()];
                b.get(data);
                _socket.write(Buffer.buffer(data));
                _transport.outputConsumed();
                b = _transport.getOutputBuffer();
            }
        }
    }

    @Override
    public String toString()
    {
        return _toString;
    }
}