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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.AmqpServiceConfig;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutboundLink;
import io.vertx.ext.amqp.ReceiverMode;
import io.vertx.ext.amqp.impl.ConnectionImpl.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The service impl makes use of the LinkManager for AMQP connection/link
 * management and the Router for routing logic.
 * 
 * @author rajith
 * 
 */
public class AmqpServiceImpl implements Handler<Message<JsonObject>>, LinkEventListener, AmqpService
{
    private static final Logger _logger = LoggerFactory.getLogger(AmqpServiceImpl.class);

    private final Vertx _vertx;

    private final EventBus _eb;

    private OutboundLinkImpl _defaultOutboundLink;

    private final Map<String, Message<JsonObject>> _vertxReplyTo = new ConcurrentHashMap<String, Message<JsonObject>>();

    private final AmqpServiceConfig _config;

    private final String _replyToAddressPrefix;

    private final MessageConsumer<JsonObject> _defaultConsumer;

    private final List<MessageConsumer<JsonObject>> _consumers = new ArrayList<MessageConsumer<JsonObject>>();

    private Map<String, IncomingMsgRef> _inboundMsgRefs = new HashMap<String, IncomingMsgRef>();

    private Map<String, String> _consumerRefToEBAddr = new HashMap<String, String>();

    private final Router _router;

    private final LinkManager _linkManager;

    private final MessageFactory _msgFactory;

    public AmqpServiceImpl(Vertx vertx, AmqpServiceConfig config) throws MessagingException
    {
        _vertx = vertx;
        _eb = _vertx.eventBus();
        _config = config;
        _msgFactory = new MessageFactory();
        _router = new Router(_config);
        _linkManager = new LinkManager(vertx, _config, this);
        _replyToAddressPrefix = "amqp://" + _config.getInboundHost() + ":" + _config.getInboundPort();

        _defaultConsumer = _eb.consumer(config.getDefaultHandlerAddress(), this);
        for (String handlerAddress : config.getHandlerAddressList())
        {
            _consumers.add(_eb.consumer(handlerAddress, this));
        }

        _defaultOutboundLink = _linkManager.findOutboundLink(config.getDefaultOutboundAddress());

        if (_logger.isInfoEnabled())
        {
            StringBuilder b = new StringBuilder();
            b.append("Router Config \n[\n");
            b.append("Default vertx handler address : ").append(config.getDefaultHandlerAddress()).append("\n");
            b.append("Default vertx address : ").append(config.getDefaultInboundAddress()).append("\n");
            b.append("Default outbound address : ").append(config.getDefaultOutboundAddress()).append("\n");
            b.append("Handler address list : ").append(config.getHandlerAddressList()).append("\n");
            b.append("]\n");
            _logger.info(b.toString());
        }
    }

    // ------------- AmqpService -----------------
    @Override
    public void start()
    {
        _linkManager.stop();
    }

    @Override
    public void stop()
    {
        _linkManager.stop();
    }

    @Override
    public AmqpService consume(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode,
            Handler<AsyncResult<String>> result)
    {
        final ConnectionSettings settings = _linkManager.getConnectionSettings(amqpAddress);
        try
        {
            ManagedConnection con = _linkManager.findConnection(settings);
            InboundLinkImpl link = con.createInboundLink(settings.getTarget(), receiverMode, creditMode);
            String id = UUID.randomUUID().toString();

            link.setContext(id);
            _inboundLinks.put(id, link);
            _consumerRefToEBAddr.put(id, ebAddress);
            _logger.info(String.format("Created inbound link {%s @ %s:%s} for ref '%s'", settings.getTarget(),
                    settings.getHost(), settings.getPort(), id));
            result.handle(new DefaultAsyncResult<String>(id));
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<String>(e));
        }

        return this;
    }

    @Override
    public AmqpService issueCredit(String consumerRef, int credits, Handler<AsyncResult<Void>> result)
    {
        if (_inboundLinks.containsKey(consumerRef))
        {
            try
            {
                _inboundLinks.get(consumerRef).setCredits(credits);
            }
            catch (MessagingException e)
            {
                result.handle(new DefaultAsyncResult<Void>(e));
            }
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid consumer reference : %s. Unable to find a matching AMQP link", consumerRef))));
        }
        return this;
    }

    @Override
    public AmqpService unregisterConsume(String consumerRef, Handler<AsyncResult<Void>> result)
    {
        if (_inboundLinks.containsKey(consumerRef))
        {
            _inboundLinks.get(consumerRef).close();
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid consumer reference : %s. Unable to find a matching AMQP link", consumerRef))));
        }
        return this;
    }

    @Override
    public AmqpService acknowledge(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result)
    {
        if (_inboundMsgRefs.containsKey(msgRef))
        {
            IncomingMsgRef ref = _inboundMsgRefs.remove(msgRef);
            if (_inboundLinks.containsKey(ref._linkRef))
            {
                SessionImpl ssn = _inboundLinks.get(ref._linkRef).getSession();
                try
                {
                    ssn.disposition(ref._sequence, disposition);
                }
                catch (MessagingException e)
                {
                    result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                            "Exception when setting disposition for message reference : %s.", msgRef))));
                }
            }
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid message reference : %s. Unable to find a matching AMQP message", msgRef))));
        }
        return this;
    }

    @Override
    public AmqpService publish(String address, JsonObject msg, Handler<AsyncResult<JsonObject>> result)
    {
        try
        {
            OutboundLinkImpl link = _router.findOutboundLink(address);
            Message m = _msgFactory.convert(msg);
            TrackerImpl tracker = (TrackerImpl) link.send(m);
            tracker.setContext(result);
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<JsonObject>(e));
        }

        return this;
    }

    // ------------\ AmqpService -----------------

    // ------------- LinkEventListener -----------
    @Override
    public void handle(Message<JsonObject> m)
    {
        try
        {
            org.apache.qpid.proton.message.Message msg = _msgFactory.convert(m.body());
            if (msg.getReplyTo() == null && m.replyAddress() != null)
            {
                msg.setReplyTo(_replyToAddressPrefix + "/" + m.replyAddress());
            }

            String routingKey = _router.extractOutboundRoutingKey(m);

            List<OutboundLinkImpl> links = _linkManager.getOutboundLinks(_router.routeOutbound(routingKey));
            if (links.size() == 0)
            {
                _logger.info("No matching address, sending default outbound address");
                if (_defaultOutboundLink.getConnection().getState() == State.FAILED)
                {
                    _logger.info("Reconnecting to default outbound link");
                    _defaultOutboundLink = _linkManager.findOutboundLink(_config.getDefaultOutboundAddress());
                }
                links.add(_defaultOutboundLink);
            }

            if (m.replyAddress() != null)
            {
                _vertxReplyTo.put(m.replyAddress(), m);
            }

            if (_logger.isInfoEnabled())
            {
                _logger.info("\n============= Outbound Routing ============");
                _logger.info(String.format("Received msg from vertx [to=%s, reply-to=%s, body=%s] ", m.address(),
                        m.replyAddress(), m.body().encode(), m.replyAddress()));
                StringBuilder b = new StringBuilder("Matched the following outbound links [");
                for (OutboundLink link : links)
                {
                    b.append(link).append(" ");
                }
                b.append("].");
                _logger.info(b.toString());
                _logger.info("============= /Outbound Routing ============\n");
            }

            for (OutboundLinkImpl link : links)
            {
                link.send(msg);
            }
        }
        catch (MessagingException e)
        {
            System.out.println("Exception routing message " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void inboundLinkReady(String linkName, String address, boolean isFromInboundConnection)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void inboundLinkFinal(String linkName, String address, boolean isFromInboundConnection)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void outboundLinkReady(String linkName, String address, boolean isFromInboundConnection)
    {
        if (isFromInboundConnection)
        {
            _router.addOutboundRoute(linkName, address);
        }
    }

    @Override
    public void outboundLinkFinal(String linkName, String address, boolean isFromInboundConnection)
    {
        if (isFromInboundConnection)
        {
            _router.removeOutboundRoute(linkName, address);
        }
    }

    @Override
    public void message(String linkName, InboundMessage msg)
    {
        JsonObject out = _msgFactory.convert(msg.getProtocolMessage());

        // Handle replyTo
        if (msg.getAddress() != null)
        {
            try
            {
                ConnectionSettings settings = _linkManager.getConnectionSettings(msg.getAddress());
                if (_vertxReplyTo.containsKey(settings.getTarget()))
                {
                    if (_logger.isDebugEnabled())
                    {
                        _logger.info("\n============= Inbound Routing (Reply-to) ============");
                        _logger.info(String.format(
                                "Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s:%s/%s'",
                                msg.getAddress(), msg.getReplyTo(), msg.getContent(), settings.getHost(),
                                settings.getPort(), settings.getTarget()));
                        _logger.info("It's a reply to vertx message with reply-to=" + settings.getTarget());
                        _logger.info("============= /Inbound Routing (Reply-to) ============\n");
                    }
                    Message<JsonObject> request = _vertxReplyTo.get(settings.getTarget());
                    request.reply(out);
                    _vertxReplyTo.remove(settings.getTarget());
                    request = null;
                    return;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                // ignore
            }
        }

        String key = null;
        switch (_config.getInboundRoutingPropertyType())
        {
        case LINK_NAME:
            key = linkName;
            break;
        case SUBJECT:
            key = msg.getSubject();
            break;
        case MESSAGE_ID:
            key = msg.getMessageId().toString();
            break;
        case CORRELATION_ID:
            key = msg.getCorrelationId().toString();
            break;
        case ADDRESS:
            key = msg.getAddress();
            break;
        case REPLY_TO:
            key = msg.getReplyTo();
            break;
        case CUSTOM:
            key = (String) msg.getApplicationProperties().get(_config.getInboundRoutingPropertyName());
            break;
        }

        List<String> addressList = _router.routeInbound(key);
        for (String address : addressList)
        {
            if (msg.getReplyTo() != null)
            {
                _eb.send(address, out, new ReplyHandler(msg.getProtocolMessage()));
            }
            else
            {
                _eb.send(address, out);
            }
        }
        if (_logger.isDebugEnabled())
        {
            ConnectionSettings settings = _linkManager.getConnectionSettings(msg.getAddress());
            _logger.info("\n============= Inbound Routing ============");
            _logger.info(String.format("Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s:%s/%s'",
                    msg.getAddress(), msg.getReplyTo(), msg.getContent(), settings.getPort(), settings.getTarget()));
            _logger.info(String.format("Inbound routing info [key=%s, value=%s]",
                    _config.getInboundRoutingPropertyType(), key));
            _logger.info("Matched the following vertx address list : " + addressList);
            _logger.info("============= /Inbound Routing ============\n");
        }
    }

    @Override
    public void outboundLinkCreditGiven(String linkName, String address, int credits)
    {
        // TODO Auto-generated method stub

    }

    // ----------- \ LinkEventListener ------------

    // ---------- Helper classes
    class ReplyHandler implements Handler<AsyncResult<Message<JsonObject>>>
    {
        org.apache.qpid.proton.message.Message _protocolMsg;

        ReplyHandler(org.apache.qpid.proton.message.Message m)
        {
            _protocolMsg = m;
        }

        @Override
        public void handle(AsyncResult<Message<JsonObject>> result)
        {
            Message<JsonObject> msg = result.result();
            try
            {
                if (_logger.isInfoEnabled())
                {
                    _logger.info("\n============= Outbound Routing (Reply To) ============");
                    _logger.info("Routing vertx reply to AMQP space");
                    _logger.info("Reply msg : " + msg.body());
                    _logger.info("============= /Outbound Routing (Reply To) ============\n");
                }
                OutboundLinkImpl link = _linkManager.findOutboundLink(_protocolMsg.getReplyTo());
                link.send(_msgFactory.convert(msg.body()));
            }
            catch (MessagingException e)
            {
                _logger.error("Error handling reply", e);
            }
        }
    }

    private class IncomingMsgRef
    {
        long _sequence;

        String _linkRef;
    }
}