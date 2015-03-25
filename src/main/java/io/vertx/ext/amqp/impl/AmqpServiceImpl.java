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

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.DeliveryState;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.ServiceOptions;

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

    private final Map<String, Message<JsonObject>> _vertxReplyTo = new ConcurrentHashMap<String, Message<JsonObject>>();

    private final AmqpServiceConfig _config;

    private final String _replyToAddressPrefix;

    private final List<MessageConsumer<JsonObject>> _consumers = new ArrayList<MessageConsumer<JsonObject>>();

    private Map<String, IncomingMsgRef> _inboundMsgRefs = new HashMap<String, IncomingMsgRef>();

    private Map<String, IncomingLinkRef> _incomingLinkRefs = new HashMap<String, IncomingLinkRef>();

    private Map<String, OutgoingLinkRef> _outgoingLinkRefs = new HashMap<String, OutgoingLinkRef>();

    private final Router _router;

    private final LinkManager _linkManager;

    private final MessageTranslator _msgTranslator;

    private final Verticle _parent;

    public AmqpServiceImpl(Vertx vertx, AmqpServiceConfig config, Verticle parent) throws MessagingException
    {
        _vertx = vertx;
        _parent = parent;
        _eb = _vertx.eventBus();
        _config = config;
        _msgTranslator = new MessageTranslator();
        _router = new Router(_config);
        _linkManager = new LinkManager(vertx, _config, this);
        _replyToAddressPrefix = "amqp://" + _config.getInboundHost() + ":" + _config.getInboundPort();

        _eb.consumer(config.getDefaultHandlerAddress(), this);
        for (String handlerAddress : config.getHandlerAddressList())
        {
            _consumers.add(_eb.consumer(handlerAddress, this));
        }

        if (_logger.isInfoEnabled())
        {
            StringBuilder b = new StringBuilder();
            b.append("Service Config \n[\n");
            b.append("Message factory : ").append(config.getDefaultHandlerAddress()).append("\n");
            b.append("Address translator : ").append(config.getDefaultInboundAddress()).append("\n");
            b.append("]\n");
            // _logger.info(b.toString());
        }
    }

    void stopInternal()
    {
        try
        {
            _parent.stop(null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // ------------- AmqpService -----------------
    @Override
    public void start()
    {
        // _linkManager.stop();
    }

    @Override
    public void stop()
    {
        _linkManager.stop();
    }

    @Override
    public AmqpService establishIncommingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
            IncomingLinkOptions options, Handler<AsyncResult<String>> result)
    {
        String source = null;
        try
        {
            if (_logger.isInfoEnabled())
            {
                _logger.info(String
                        .format("Service method establishIncommingLink called with amqpAddress=%s, eventbusAddress=%s, notificationAddress=%s, options=%s",
                                amqpAddress, eventbusAddress, notificationAddress, options));
            }
            source = _linkManager.getConnectionSettings(amqpAddress).getNode();
            IncomingLinkImpl link = _linkManager.getInboundLink(amqpAddress);
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
                _router.addInboundRoute(source, eventbusAddress);
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
                _incomingLinkRefs.put(id, new IncomingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress,
                        result));
                _logger.info(String
                        .format("Created incoming link from AMQP-message-source '%s' to event-bus-address '%s'. The link ref is '%s'",
                                amqpAddress, eventbusAddress, id));
                _router.addInboundRoute(source, eventbusAddress);

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
            if (_logger.isInfoEnabled())
            {
                _logger.info(String.format("Service method fetch called with incomingLinkRef=%s, messages=%s",
                        incomingLinkRef, messages));
            }
            if (_incomingLinkRefs.containsKey(incomingLinkRef))
            {
                IncomingLinkImpl link = _linkManager.getInboundLink(_incomingLinkRefs.get(incomingLinkRef)._amqpAddr);
                link.setCredits(messages);
                result.handle(DefaultAsyncResult.VOID_SUCCESS);
            }
            else
            {
                result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                        "IncomingLinkRef '%s' doesn't match any incoming links.", incomingLinkRef),
                        ErrorCode.INVALID_LINK_REF)));
            }

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
            if (_logger.isInfoEnabled())
            {
                _logger.info(String.format("Service method cancelIncommingLink called with incomingLinkRef=%s",
                        incomingLinkRef));
            }
            if (_incomingLinkRefs.containsKey(incomingLinkRef))
            {
                IncomingLinkImpl link = _linkManager.getInboundLink(_incomingLinkRefs.get(incomingLinkRef)._amqpAddr);
                link.close();
                result.handle(DefaultAsyncResult.VOID_SUCCESS);
            }
            else
            {
                result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                        "IncomingLinkRef '%s' doesn't match any incoming links.", incomingLinkRef),
                        ErrorCode.INVALID_LINK_REF)));
            }
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
        if (_logger.isInfoEnabled())
        {
            _logger.info(String.format("Service method accept called with msgRef=%s", msgRef));
        }
        return updateDelivery(msgRef, MessageDisposition.ACCEPTED, result);
    }

    @Override
    public AmqpService reject(String msgRef, Handler<AsyncResult<Void>> result)
    {
        if (_logger.isInfoEnabled())
        {
            _logger.info(String.format("Service method reject called with msgRef=%s", msgRef));
        }
        return updateDelivery(msgRef, MessageDisposition.REJECTED, result);
    }

    @Override
    public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result)
    {
        if (_logger.isInfoEnabled())
        {
            _logger.info(String.format("Service method release called with msgRef=%s", msgRef));
        }
        return updateDelivery(msgRef, MessageDisposition.RELEASED, result);
    }

    AmqpService updateDelivery(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result)
    {
        System.out.println("################   update delivery");
        if (_inboundMsgRefs.containsKey(msgRef))
        {
            System.out.println("################   Found message ref to ack");
            IncomingMsgRef ref = _inboundMsgRefs.remove(msgRef);
            if (_incomingLinkRefs.containsKey(ref._linkRef))
            {
                System.out.println("################   Found message ref to ack -- found the corresponding link ref");
                String linkAddr = _incomingLinkRefs.get(ref._linkRef)._amqpAddr;
                IncomingLinkImpl link;
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
                    ssn.settle(ref._sequence, false, true);
                    System.out.println("################   Found message ref to ack -- called settled with write true");
                    result.handle(DefaultAsyncResult.VOID_SUCCESS);
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
            _logger.info(String.format("Invalid msg ref '%s'. Notifying handler", msgRef));
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
        if (_logger.isInfoEnabled())
        {
            _logger.info(String
                    .format("Service method establishOutgoingLink called with amqpAddress=%s, eventbusAddress=%s, notificationAddress=%s, options=%s",
                            amqpAddress, eventbusAddress, notificationAddress, options));
        }
        if (_linkManager.isOutboundLinkExist(amqpAddress))
        {
            try
            {
                OutgoingLinkImpl link = _linkManager.getOutboundLink(amqpAddress);
                // Link already exists, send the existing ref.
                _router.addOutboundRoute(eventbusAddress, amqpAddress);
                _eb.consumer(eventbusAddress, this);
                result.handle(new DefaultAsyncResult<String>((String) link.getContext()));
            }
            catch (MessagingException e)
            {
                // Link exists, but some other error occurred. Notify user.
                result.handle(new DefaultAsyncResult<String>(e));
            }
        }
        else
        {
            String id = UUID.randomUUID().toString();
            try
            {
                _linkManager.createOutboundLink(id, amqpAddress, options);
                _router.addOutboundRoute(eventbusAddress, amqpAddress);
                _eb.consumer(eventbusAddress, this);
            }
            catch (MessagingException e1)
            {
                result.handle(new DefaultAsyncResult<String>(e1));
                return this;
            }
            _outgoingLinkRefs.put(id,
                    new OutgoingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress, result));
            _logger.info(String
                    .format("Created outgoing link to AMQP-message-sink '%s' from event-bus-address '%s'. The link ref is '%s'",
                            amqpAddress, eventbusAddress, id));
        }
        return this;
    }

    @Override
    public AmqpService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result)
    {
        try
        {
            if (_logger.isInfoEnabled())
            {
                _logger.info(String.format("Service method cancelOutgoingLink called with outgoingLinkRef=%s",
                        outgoingLinkRef));
            }
            if (_outgoingLinkRefs.containsKey(outgoingLinkRef))
            {
                OutgoingLinkImpl link = _linkManager.getOutboundLink(_outgoingLinkRefs.get(outgoingLinkRef)._amqpAddr);
                link.close();
                result.handle(DefaultAsyncResult.VOID_SUCCESS);
            }
            else
            {
                result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                        "OutgoingLinkRef '%s' doesn't match any outgoing links.", outgoingLinkRef),
                        ErrorCode.INVALID_LINK_REF)));
            }
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Error {%s}, when cancelling outgoing link : %s.", e.getMessage(), outgoingLinkRef), e
                    .getErrorCode())));
        }
        return this;
    }

    public AmqpService registerService(String eventbusAddress, ServiceOptions options, Handler<AsyncResult<Void>> result)
    {
        return this;
    }

    @Fluent
    public AmqpService unregisterService(String eventbusAddress, Handler<AsyncResult<Void>> result)
    {
        return this;
    }// ------------\ AmqpService -----------------

    // -- Handler method for receiving messages from the event-bus -----------
    @Override
    public void handle(Message<JsonObject> vertxMsg)
    {
        try
        {
            _logger.info(String.format("Received msg : {address : %s, reply-to : %s, body : %s} ", vertxMsg.address(),
                    vertxMsg.replyAddress(), vertxMsg.body() == null ? "" : vertxMsg.body().encodePrettily()));
            org.apache.qpid.proton.message.Message msg = _msgTranslator.convert(vertxMsg.body());
            if (msg.getReplyTo() == null && vertxMsg.replyAddress() != null)
            {
                msg.setReplyTo(_replyToAddressPrefix + "/" + vertxMsg.replyAddress());
            }

            String routingKey = _router.extractOutboundRoutingKey(vertxMsg);

            List<String> amqpAddressList = _router.routeOutbound(routingKey);
            if (amqpAddressList.size() == 0)
            {
                try
                {
                    _linkManager.send(_config.getDefaultOutboundAddress() , msg, vertxMsg.body());
                    _logger.info("No matching address, sending to default outbound address");
                }
                catch (MessagingException e)
                {
                    _logger.error(String.format("Error {code=%s, msg='%s'} sending to default outbound address %s",
                            e.getErrorCode(), e.getMessage(), _config.getDefaultOutboundAddress()));
                }
            }

            if (vertxMsg.replyAddress() != null)
            {
                _vertxReplyTo.put(vertxMsg.replyAddress(), vertxMsg);
            }

            if (_logger.isDebugEnabled())
            {
                _logger.debug("\n============= Outbound Routing ============");
                _logger.debug(String.format("Received msg from vertx [to=%s, reply-to=%s, body=%s] ", vertxMsg.address(),
                        vertxMsg.replyAddress(), vertxMsg.body().encode(), vertxMsg.replyAddress()));
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
                // Grab the link-id before an error occurs (if there is one).
                String id = _linkManager.getOutboundLinkId(amqpAddress);
                try
                {
                    _linkManager.send(amqpAddress, msg, vertxMsg.body());
                }
                catch (MessagingException e)
                {
                    _logger.error(String.format("Error {code=%s, msg='%s'} sending to AMQP address %s",
                            e.getErrorCode(), e.getMessage(), amqpAddress));
                    sendNotificatonMessage(id, NotificationMessageFactory.error(id, e.getErrorCode(), e.getMessage()));
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
    public void incomingLinkReady(String id, String linkName, String address, boolean isFromInboundConnection)
    {
        if (!isFromInboundConnection)
        {
            if (_incomingLinkRefs.containsKey(id))
            {
                _incomingLinkRefs.get(id)._resultHandler.handle(new DefaultAsyncResult<String>(id));
                _logger.info(String.format("Incoming Link '%s' is ready. Notifying handler", id));
            }
        }
    }

    @Override
    public void incomingLinkFinal(String id, String linkName, String address, boolean isFromInboundConnection)
    {
    }

    @Override
    public void outgoingLinkReady(String id, String linkName, String address, boolean isFromInboundConnection)
    {
        if (isFromInboundConnection)
        {
            _router.addOutboundRoute(linkName, address);
        }
        else
        {
            if (_outgoingLinkRefs.containsKey(id))
            {
                _outgoingLinkRefs.get(id)._resultHandler.handle(new DefaultAsyncResult<String>(id));
                _logger.info(String.format("Outgoing Link '%s' is ready. Notifying handler", id));
            }
        }
    }

    @Override
    public void outgoingLinkFinal(String id, String linkName, String address, boolean isFromInboundConnection)
    {
        if (isFromInboundConnection)
        {
            _router.removeOutboundRoute(linkName, address);
        }
    }

    @Override
    public void deliveryUpdate(String id, String msgRef, DeliveryState state, MessageDisposition disp)
    {
        System.out.println(String.format("Delivery update called id=%s, msg-ref=%s, state=%s, disp=%s", id, msgRef,
                state, disp));
        if (_outgoingLinkRefs.containsKey(id))
        {
            sendNotificatonMessage(id, NotificationMessageFactory.deliveryState(msgRef, state, disp));
        }
        else
        {
            _logger.warn(String
                    .format("Error : Delivery update received for link not in map. Details [msg-ref : '%s' tied to link-ref : '%s']",
                            id, msgRef));
        }
    }

    @Override
    public void message(String id, String linkName, String peerAddress, ReliabilityMode reliability, InboundMessage msg)
    {
        JsonObject out = _msgTranslator.convert(msg.getProtocolMessage());
        out.put("vertx.amqp.from", peerAddress);
        out.put(INCOMING_MSG_REF, msg.getMsgRef());
        if (reliability == ReliabilityMode.AT_LEAST_ONCE)
        {
            _inboundMsgRefs.put(msg.getMsgRef(), new IncomingMsgRef(msg.getSequence(), id));
        }

        // Handle replyTo
        if (msg.getAddress() != null)
        {
            try
            {
                ConnectionSettings settings = _linkManager.getConnectionSettings(msg.getAddress());
                String replyToKey = settings.getNode() != null ? settings.getNode() : settings.getHost();
                if (_vertxReplyTo.containsKey(replyToKey))
                {
                    if (_logger.isInfoEnabled())
                    {
                        _logger.info("\n============= Inbound Routing (Reply-to) ============");
                        _logger.info(String.format(
                                "Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s'", msg.getAddress(),
                                msg.getReplyTo(), msg.getContent(), peerAddress));
                        _logger.info("It's a reply to vertx message with reply-to=" + replyToKey);
                        _logger.info("============= /Inbound Routing (Reply-to) ============\n");
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
        if (_logger.isInfoEnabled())
        {
            _logger.info("\n============= Inbound Routing ============");
            _logger.info(String.format("Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s'",
                    msg.getAddress(), msg.getReplyTo(), msg.getContent(), peerAddress));
            _logger.info(String.format("Inbound routing info [key=%s, value=%s]",
                    _config.getInboundRoutingPropertyType(), key));
            _logger.info("Matched the following vertx address list : " + addressList);
            _logger.info("============= /Inbound Routing ============\n");
        }
    }

    @Override
    public void outgoingLinkCreditGiven(String id, int credits)
    {
        System.out.println(String.format("Credit received for id=%s, credit=%s", id, credits));
        if (_outgoingLinkRefs.containsKey(id))
        {
            sendNotificatonMessage(id, NotificationMessageFactory.credit(id, credits));
        }
        else
        {
            _logger.warn(String.format("Error : Credit received for link not in map. Details [link-ref : '%s']", id));
        }
    }// ----------- \ LinkEventListener ------------

    private void sendNotificatonMessage(String id, JsonObject msg)
    {
        String notificationAddr = _outgoingLinkRefs.get(id)._notificationAddr;
        if (notificationAddr != null && !notificationAddr.trim().isEmpty())
        {
            _eb.send(notificationAddr, msg);
        }
        else
        {
            _logger.warn(String.format("Error : Notification-address is null or empty. Unable to send message='%s']",
                    msg));
        }
    }

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
                OutgoingLinkImpl link = _linkManager.getOutboundLink(_protocolMsg.getReplyTo());
                link.send(_msgTranslator.convert(msg.body()));
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

        final Handler<AsyncResult<String>> _resultHandler;

        IncomingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr,
                Handler<AsyncResult<String>> resultHandler)
        {
            _id = id;
            _amqpAddr = amqpAddr;
            _ebAddr = ebAddr;
            _notificationAddr = notificationAddr;
            _resultHandler = resultHandler;
        }
    }

    class OutgoingLinkRef
    {
        final String _id;

        final String _amqpAddr;

        final String _ebAddr;

        final String _notificationAddr;

        final Handler<AsyncResult<String>> _resultHandler;

        OutgoingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr,
                Handler<AsyncResult<String>> resultHandler)
        {
            _id = id;
            _amqpAddr = amqpAddr;
            _ebAddr = ebAddr;
            _notificationAddr = notificationAddr;
            _resultHandler = resultHandler;
        }
    }
}