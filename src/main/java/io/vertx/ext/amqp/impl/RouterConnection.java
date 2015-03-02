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

import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.InboundLink;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutboundLink;
import io.vertx.ext.amqp.SenderMode;
import io.vertx.ext.amqp.Session;

import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;

/**
 * A Connection coupled with a session to simplify the RouterImpl.
 * 
 */
class RouterConnection extends ConnectionImpl
{
    private final org.apache.qpid.proton.engine.Session _protonSession;

    private SessionImpl _session;

    private EventHandler _eventHandler;

    RouterConnection(ConnectionSettings settings, EventHandler handler, boolean inbound)
    {
        super(settings, null, inbound);
        _eventHandler = handler;
        _protonSession = protonConnection.session();
        _session = new SessionImpl(this, _protonSession);
    }

    @Override
    public void open()
    {
        protonConnection.open();
        _protonSession.open();
        write();
    }

    @Override
    protected void processEvents()
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
                    ssn = new SessionImpl(this, amqpSsn);
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
                    InboundLinkImpl inboundLink;
                    if (link.getContext() != null)
                    {
                        inboundLink = (InboundLinkImpl) link.getContext();
                    }
                    else
                    {
                        inboundLink = new InboundLinkImpl(_session, link.getRemoteTarget().getAddress(), link,
                                CreditMode.AUTO);
                        link.setContext(inboundLink);
                        inboundLink.init();
                    }
                    _eventHandler.onInboundLinkOpen(inboundLink);
                }
                else
                {
                    OutboundLinkImpl outboundLink;
                    if (link.getContext() != null)
                    {
                        outboundLink = (OutboundLinkImpl) link.getContext();
                    }
                    else
                    {
                        outboundLink = new OutboundLinkImpl(_session, link.getRemoteSource().getAddress(), link);
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
                    OutboundLinkImpl outboundLink = (OutboundLinkImpl) link.getContext();
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

    public OutboundLinkImpl createOutBoundLink(String address) throws MessagingException
    {
        OutboundLinkImpl link = (OutboundLinkImpl) _session.createOutboundLink(address, SenderMode.AT_LEAST_ONCE);
        link.init();
        write();
        return link;
    }
}