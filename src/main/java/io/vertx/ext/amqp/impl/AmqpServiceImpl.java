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
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutboundLink;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;
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

    static final OutgoingLinkOptions DEFAULT_OUTGOING_LINK_OPTIONS = new OutgoingLinkOptions();

    private final Vertx _vertx;

    private final EventBus _eb;

    private final Map<String, Message<JsonObject>> _vertxReplyTo = new ConcurrentHashMap<String, Message<JsonObject>>();

    private final AmqpServiceConfig _config;

    private final String _replyToAddressPrefix;

    private final MessageConsumer<JsonObject> _defaultConsumer;

    private final List<MessageConsumer<JsonObject>> _consumers = new ArrayList<MessageConsumer<JsonObject>>();

    private Map<String, IncomingMsgRef> _inboundMsgRefs = new HashMap<String, IncomingMsgRef>();

    private Map<String, IncomingLinkRef> _incomingLinkRefs = new HashMap<String, IncomingLinkRef>();

    private Map<String, OutgoingLinkRef> _outgoingLinkRefs = new HashMap<String, OutgoingLinkRef>();

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

        _linkManager.createOutboundLink("default", config.getDefaultOutboundAddress(), DEFAULT_OUTGOING_LINK_OPTIONS);

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
        _defaultConsumer.unregister();
        _linkManager.stop();
    }

    @Override
    public AmqpService establishIncommingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
            IncomingLinkOptions options, Handler<AsyncResult<String>> result)
    {
        try
        {
            InboundLinkImpl link = _linkManager.getInboundLink(amqpAddress);
            // Link already exists, check for exclusivity. If not send the
            // existing ref.
            if (options.isExclusive())
            {
                String error = "Cannot create an exclusive subscription. Other active subscriptions already exists";
                result.handle(new DefaultAsyncResult<String>(error, new MessagingException(error,
                        ErrorCode.ALREADY_EXISTS)));
            }
            else
            {
                _router.addInboundRoute(amqpAddress, eventbusAddress);
                result.handle(new DefaultAsyncResult<String>((String) link.getContext()));
            }
        }
        catch (MessagingException e)
        {
            // Link doesn't exist. Creating one.
            if (e.getErrorCode() == ErrorCode.INVALID_LINK_REF)
            {
                String id = UUID.randomUUID().toString();
                try
                {
                    _linkManager.createInboundLink(id, amqpAddress, options);
                }
                catch (MessagingException e1)
                {
                    result.handle(new DefaultAsyncResult<String>(e1));
                    return this;
                }
                _incomingLinkRefs.put(id, new IncomingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress));
                _logger.info(String
                        .format("Created incoming link from AMQP-message-source @ {%s} to event-bus-address %s. The link ref is '%s'",
                                amqpAddress, eventbusAddress, id));
                _router.addInboundRoute(amqpAddress, eventbusAddress);
                result.handle(new DefaultAsyncResult<String>(id));

            }
            else
            {
                // Link exists, but some other error occurred. Notify user.
                result.handle(new DefaultAsyncResult<String>(e));
            }
        }
        return this;
    }

    @Override
    public AmqpService fetch(String incomingLinkRef, int messages, Handler<AsyncResult<Void>> result)
    {
        try
        {
            InboundLinkImpl link = _linkManager.getInboundLink(_incomingLinkRefs.get(incomingLinkRef)._amqpAddr);
            link.setCredits(messages);
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Error {%s}, when fetching messages from incoming link : %s.", e.getMessage(), incomingLinkRef), e
                    .getErrorCode())));
        }
        return this;
    }

    @Override
    public AmqpService cancelIncommingLink(String incomingLinkRef, Handler<AsyncResult<Void>> result)
    {
        try
        {
            InboundLinkImpl link = _linkManager.getInboundLink(_incomingLinkRefs.get(incomingLinkRef)._amqpAddr);
            link.close();
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Error {%s}, when cancelling incoming link : %s.", e.getMessage(), incomingLinkRef), e
                    .getErrorCode())));
        }
        return this;
    }

    @Override
    public AmqpService accept(String msgRef, Handler<AsyncResult<Void>> result)
    {
        return updateDelivery(msgRef, MessageDisposition.ACCEPTED, result);
    }

    @Override
    public AmqpService reject(String msgRef, Handler<AsyncResult<Void>> result)
    {
        return updateDelivery(msgRef, MessageDisposition.REJECTED, result);
    }

    @Override
    public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result)
    {
        return updateDelivery(msgRef, MessageDisposition.RELEASED, result);
    }

    AmqpService updateDelivery(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result)
    {
        if (_inboundMsgRefs.containsKey(msgRef))
        {
            IncomingMsgRef ref = _inboundMsgRefs.remove(msgRef);
            if (_incomingLinkRefs.containsKey(ref._linkRef))
            {
                String linkAddr = _incomingLinkRefs.get(ref._linkRef)._amqpAddr;
                InboundLinkImpl link;
                try
                {
                    link = _linkManager.getInboundLink(linkAddr);
                }
                catch (MessagingException e)
                {
                    result.handle(new DefaultAsyncResult<Void>(e));
                    return this;
                }
                SessionImpl ssn = link.getSession();
                try
                {
                    ssn.checkClosed();
                    ssn.disposition(ref._sequence, disposition);
                }
                catch (MessagingException e)
                {
                    result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                            "Error {%s}, when %s message reference : %s.", e.getMessage(), disposition, msgRef), e
                            .getErrorCode())));
                }
            }
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid message reference : %s. Unable to find a matching AMQP message", msgRef),
                    ErrorCode.INVALID_MSG_REF)));
        }
        return this;
    }

    @Override
    public AmqpService establishOutgoingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
            OutgoingLinkOptions options, Handler<AsyncResult<String>> result)
    {
        try
        {
            OutboundLinkImpl link = _linkManager.getOutboundLink(amqpAddress);
            // Link already exists, send the existing ref.
            _router.addOutboundRoute(eventbusAddress, amqpAddress);
            result.handle(new DefaultAsyncResult<String>((String) link.getContext()));
        }
        catch (MessagingException e)
        {
            // Link doesn't exist. Creating one.
            if (e.getErrorCode() == ErrorCode.INVALID_LINK_REF)
            {
                String id = UUID.randomUUID().toString();
                try
                {
                    _linkManager.createOutboundLink(id, amqpAddress, options);
                }
                catch (MessagingException e1)
                {
                    result.handle(new DefaultAsyncResult<String>(e1));
                    return this;
                }
                _outgoingLinkRefs.put(id, new OutgoingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress));
                _logger.info(String
                        .format("Created outgoing link to AMQP-message-sink @ {%s} from event-bus-address %s. The link ref is '%s'",
                                amqpAddress, eventbusAddress, id));
                _router.addInboundRoute(amqpAddress, eventbusAddress);
                result.handle(new DefaultAsyncResult<String>(id));

            }
            else
            {
                // Link exists, but some other error occurred. Notify user.
                result.handle(new DefaultAsyncResult<String>(e));
            }
        }
        return this;
    }

    @Override
    public AmqpService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result)
    {
        try
        {
            OutboundLinkImpl link = _linkManager.getOutboundLink(_outgoingLinkRefs.get(outgoingLinkRef)._amqpAddr);
            link.close();
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Error {%s}, when cancelling outgoing link : %s.", e.getMessage(), outgoingLinkRef), e
                    .getErrorCode())));
        }
        return this;
    }// ------------\ AmqpService -----------------

    // -- Handler method for receiving messages from the event-bus -----------
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

            List<String> amqpAddressList = _router.routeOutbound(routingKey);
            if (amqpAddressList.size() == 0)
            {
                try
                {
                    OutboundLinkImpl link = _linkManager.getOutboundLink(_config.getDefaultOutboundAddress());
                    link.send(msg);
                    _logger.info("No matching address, sending to default outbound address");
                }
                catch (MessagingException e)
                {
                    _logger.error(String.format("Error {code=%s, msg='%s'} sending to default outbound address %s",
                            e.getErrorCode(), e.getMessage(), _config.getDefaultOutboundAddress()));
                }
            }

            if (m.replyAddress() != null)
            {
                _vertxReplyTo.put(m.replyAddress(), m);
            }

            if (_logger.isDebugEnabled())
            {
                _logger.debug("\n============= Outbound Routing ============");
                _logger.debug(String.format("Received msg from vertx [to=%s, reply-to=%s, body=%s] ", m.address(),
                        m.replyAddress(), m.body().encode(), m.replyAddress()));
                StringBuilder b = new StringBuilder("Matched the following AMQP addresses [");
                for (String amqpAddress : amqpAddressList)
                {
                    b.append(amqpAddress).append(" ");
                }
                b.append("].");
                _logger.debug(b.toString());
                _logger.debug("============= /Outbound Routing ============\n");
            }

            for (String amqpAddress : amqpAddressList)
            {
                try
                {
                    OutboundLinkImpl link = _linkManager.getOutboundLink(amqpAddress);
                    link.send(msg);
                }
                catch (MessagingException e)
                {
                    _logger.error(String.format("Error {code=%s, msg='%s'} sending to AMQP address %s",
                            e.getErrorCode(), e.getMessage(), amqpAddress));
                }
            }
        }
        catch (MessagingException e)
        {
            _logger.error(String.format("Error {code=%s, msg='%s'} routing outbound", e.getErrorCode(), e.getMessage()));
        }
    }// ------------- \ Event bus handler -----------

    // ------------- LinkEventListener -----------
    @Override
    public void inboundLinkReady(String linkName, String address, boolean isFromInboundConnection)
    {
    }

    @Override
    public void inboundLinkFinal(String linkName, String address, boolean isFromInboundConnection)
    {
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
                String replyToKey = settings.getTarget() != null ? settings.getTarget() : settings.getHost();
                if (_vertxReplyTo.containsKey(replyToKey))
                {
                    if (_logger.isDebugEnabled())
                    {
                        _logger.debug("\n============= Inbound Routing (Reply-to) ============");
                        _logger.debug(String.format(
                                "Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s:%s/%s'",
                                msg.getAddress(), msg.getReplyTo(), msg.getContent(), settings.getHost(),
                                settings.getPort(), settings.getTarget()));
                        _logger.debug("It's a reply to vertx message with reply-to=" + replyToKey);
                        _logger.debug("============= /Inbound Routing (Reply-to) ============\n");
                    }
                    try
                    {
                        Message<JsonObject> request = _vertxReplyTo.get(replyToKey);
                        request.reply(out);
                        _vertxReplyTo.remove(replyToKey);
                        request = null;
                        return;
                    }
                    catch (Exception e)
                    {
                        _logger.error(String.format("Error {msg='%s'} replying to vertx msg", e.getMessage()));
                    }
                }

            }
            catch (MessagingException e)
            {
                _logger.error(String.format("Error {code=%s, msg='%s'} parsing address field in AMQP message",
                        e.getErrorCode(), e.getMessage()));
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
            try
            {
                ConnectionSettings settings = _linkManager.getConnectionSettings(msg.getAddress());
                _logger.debug("\n============= Inbound Routing ============");
                _logger.debug(String.format("Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s:%s/%s'",
                        msg.getAddress(), msg.getReplyTo(), msg.getContent(), settings.getPort(), settings.getTarget()));
                _logger.debug(String.format("Inbound routing info [key=%s, value=%s]",
                        _config.getInboundRoutingPropertyType(), key));
                _logger.debug("Matched the following vertx address list : " + addressList);
                _logger.debug("============= /Inbound Routing ============\n");
            }
            catch (MessagingException e)
            {
                // ignore
            }
        }
    }

    @Override
    public void outboundLinkCreditGiven(String linkName, String address, int credits)
    {
        // TODO Auto-generated method stub

    }// ----------- \ LinkEventListener ------------

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
                if (_logger.isDebugEnabled())
                {
                    _logger.debug("\n============= Outbound Routing (Reply To) ============");
                    _logger.debug("Routing vertx reply to AMQP space");
                    _logger.debug("Reply msg : " + msg.body());
                    _logger.debug("============= /Outbound Routing (Reply To) ============\n");
                }
                OutboundLinkImpl link = _linkManager.getOutboundLink(_protocolMsg.getReplyTo());
                link.send(_msgFactory.convert(msg.body()));
            }
            catch (MessagingException e)
            {
                _logger.error(String.format("Error {code=%s, msg='%s'} handling reply", e.getErrorCode(),
                        e.getMessage()));
            }
        }
    }

    class IncomingMsgRef
    {
        long _sequence;

        String _linkRef;

        IncomingMsgRef(long seq, String ref)
        {
            _sequence = seq;
            _linkRef = ref;
        }
    }

    class IncomingLinkRef
    {
        final String _id;

        final String _amqpAddr;

        final String _ebAddr;

        final String _notificationAddr;

        IncomingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr)
        {
            _id = id;
            _amqpAddr = amqpAddr;
            _ebAddr = ebAddr;
            _notificationAddr = notificationAddr;
        }
    }

    class OutgoingLinkRef
    {
        final String _id;

        final String _amqpAddr;

        final String _ebAddr;

        final String _notificationAddr;

        OutgoingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr)
        {
            _id = id;
            _amqpAddr = amqpAddr;
            _ebAddr = ebAddr;
            _notificationAddr = notificationAddr;
        }
    }
}