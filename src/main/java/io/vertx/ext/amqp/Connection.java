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
package io.vertx.ext.amqp;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetSocket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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

public class Connection
{
    enum State
    {
        NEW, CONNECTED, FAILED
    };

    private static final Logger _logger = LoggerFactory.getLogger(Connection.class);

    private final org.apache.qpid.proton.engine.Connection protonConnection;

    private final Transport _transport;

    private final org.apache.qpid.proton.engine.Session _protonSession;

    private final Collector _collector;

    private final ArrayList<Handler<Void>> disconnectHandlers = new ArrayList<Handler<Void>>();

    private final Object _lock = new Object();

    private final EventHandler _eventHandler;

    private final ConnectionSettings _settings;

    private final boolean _isInbound;

    private final String _toString;

    private Session _session;

    private NetSocket _socket;

    private State _state = State.NEW;

    public Connection(ConnectionSettings settings, EventHandler handler, boolean inbound)
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
        _protonSession = protonConnection.session();
        _session = new Session(this, _protonSession);
    }

    ConnectionSettings getSettings()
    {
        return _settings;
    }

    void addDisconnectHandler(Handler<Void> handler)
    {
        disconnectHandlers.add(handler);
    }

    void setNetSocket(NetSocket s)
    {
        synchronized (_lock)
        {
            _socket = s;
            _socket.handler(new DataHandler());
            _socket.drainHandler(new DrainHandler());
            _socket.endHandler(new EosHandler());
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

    public void open()
    {
        protonConnection.open();
        _protonSession.open();
        write();
    }

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

    public boolean isInbound()
    {
        return _isInbound;
    }

    public void close()
    {
        protonConnection.close();
    }

    public OutboundLink createOutBoundLink(String address) throws MessagingException
    {
        OutboundLink link = _session.createOutboundLink(address, OutboundLinkMode.AT_LEAST_ONCE);
        link.init();
        write();
        return link;
    }

    private void processEvents()
    {
        protonConnection.collect(_collector);
        Event event = _collector.peek();
        while (event != null)
        {
            switch (event.getType())
            {
            case CONNECTION_REMOTE_OPEN:
                _eventHandler.onConnectionOpen(this);
                break;
            case CONNECTION_FINAL:
                _eventHandler.onConnectionClosed(this);
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
                    ssn = new Session(this, amqpSsn);
                    amqpSsn.setContext(ssn);
                    event.getSession().open();
                }
                _eventHandler.onSessionOpen(ssn);
                break;
            case SESSION_FINAL:
                ssn = (Session) event.getSession().getContext();
                _eventHandler.onSessionClosed(ssn);
                break;
            case LINK_REMOTE_OPEN:
                Link link = event.getLink();
                if (link instanceof Receiver)
                {
                    InboundLink inboundLink;
                    if (link.getContext() != null)
                    {
                        inboundLink = (InboundLink) link.getContext();
                    }
                    else
                    {
                        inboundLink = new InboundLink(_session, link.getRemoteTarget().getAddress(), link,
                                CreditMode.AUTO);
                        link.setContext(inboundLink);
                        inboundLink.init();
                    }
                    _eventHandler.onInboundLinkOpen(inboundLink);
                }
                else
                {
                    OutboundLink outboundLink;
                    if (link.getContext() != null)
                    {
                        outboundLink = (OutboundLink) link.getContext();
                    }
                    else
                    {
                        outboundLink = new OutboundLink(_session, link.getRemoteSource().getAddress(), link);
                        link.setContext(outboundLink);
                        outboundLink.init();
                    }
                    _eventHandler.onOutboundLinkOpen(outboundLink);
                }
                break;
            case LINK_FLOW:
                link = event.getLink();
                if (link instanceof Sender)
                {
                    OutboundLink outboundLink = (OutboundLink) link.getContext();
                    _eventHandler.onOutboundLinkCredit(outboundLink, link.getCredit());
                }
                break;
            case LINK_FINAL:
                link = event.getLink();
                if (link instanceof Receiver)
                {
                    InboundLink inboundLink = (InboundLink) link.getContext();
                    _eventHandler.onInboundLinkClosed(inboundLink);
                }
                else
                {
                    OutboundLink outboundLink = (OutboundLink) link.getContext();
                    _eventHandler.onOutboundLinkClosed(outboundLink);
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

            InboundLink inLink = (InboundLink) link.getContext();
            Session ssn = inLink.getSession();
            AmqpMessage msg = new InboundMessage(ssn.getID(), d.getTag(), ssn.getNextIncommingSequence(),
                    d.isSettled(), pMsg);
            try
            {
                _eventHandler.onMessage(inLink, msg);
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
                Tracker tracker = (Tracker) d.getContext();
                tracker.setDisposition(d.getRemoteState());
                tracker.markSettled();
                _eventHandler.onSettled(tracker);
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
                    _logger.debug(String.format("Connection s%:s%, state = %s. Returning without writing",
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

    private class DataHandler implements Handler<Buffer>
    {
        public void handle(Buffer data)
        {
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
        }
    }

    private class DrainHandler implements Handler<Void>
    {
        public void handle(Void v)
        {
            write();
        }
    }

    private class EosHandler implements Handler<Void>
    {
        public void handle(Void v)
        {
            setState(State.FAILED);
            _logger.info("Received EOF. Setting state to FAILED");
            for (Handler<Void> h : disconnectHandlers)
            {
                h.handle(v);
            }
        }
    }

    @Override
    public String toString()
    {
        return _toString;
    }
}