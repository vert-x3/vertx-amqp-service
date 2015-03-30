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

import static io.vertx.ext.amqp.impl.util.Functions.format;
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
import io.vertx.ext.amqp.MessageFormatException;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.ServiceOptions;
import io.vertx.ext.amqp.impl.protocol.InboundMessage;
import io.vertx.ext.amqp.impl.protocol.LinkEventListener;
import io.vertx.ext.amqp.impl.protocol.LinkManager;
import io.vertx.ext.amqp.impl.protocol.MessageDisposition;
import io.vertx.ext.amqp.impl.routing.InboundRoutingPropertyType;
import io.vertx.ext.amqp.impl.routing.Router;
import io.vertx.ext.amqp.impl.translators.MessageTranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void stopInternal()
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

            String id = _linkManager.getIncomingLinkId(amqpAddress);
            if (id != null)
            {
                // Link already exists, check for exclusiveness
                if (options.isExclusive())
                {
                    String error = "Cannot create an exclusive subscription. Other active subscriptions already exists";
                    result.handle(new DefaultAsyncResult<String>(error, new MessagingException(error,
                            ErrorCode.ALREADY_EXISTS)));
                }
                else
                {
                    _logger.info(format(
                            "Incoming link to AMQP-message-source '%s' already exists. Returning link ref '%s'",
                            amqpAddress, id));
                    // TODO handle the else here
                    if (_incomingLinkRefs.containsKey(id))
                    {
                        IncomingLinkRef ref = _incomingLinkRefs.get(id);
                        ref.addEventBusAddress(eventbusAddress);
                        if (notificationAddress != null && !notificationAddress.trim().isEmpty())
                        {
                            ref.addNotificationAddress(notificationAddress);
                        }
                    }
                }
            }
            else
            {
                id = _linkManager.createIncomingLink(amqpAddress, options);

                _incomingLinkRefs.put(id, new IncomingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress,
                        result));
                _logger.info(String
                        .format("Created incoming link from AMQP-message-soure to vertx-amqp-bridge '%s'. The link ref is '%s'",
                                amqpAddress, id));
            }
            _router.addInboundRoute(source, eventbusAddress);
            result.handle(new DefaultAsyncResult<String>(id));
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<String>(e));
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
                _logger.info(format("Service method fetch called with incomingLinkRef=%s, messages=%s",
                        incomingLinkRef, messages));
            }
            if (_incomingLinkRefs.containsKey(incomingLinkRef))
            {
                _linkManager.setCredits(_incomingLinkRefs.get(incomingLinkRef)._amqpAddr, messages);
                result.handle(DefaultAsyncResult.VOID_SUCCESS);
            }
            else
            {
                result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
                        "IncomingLinkRef '%s' doesn't match any incoming links.", incomingLinkRef),
                        ErrorCode.INVALID_LINK_REF)));
            }

        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
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
                _logger.info(format("Service method cancelIncommingLink called with incomingLinkRef=%s",
                        incomingLinkRef));
            }
            if (_incomingLinkRefs.containsKey(incomingLinkRef))
            {
                _linkManager.closeIncomingLink(_incomingLinkRefs.get(incomingLinkRef)._amqpAddr);
                result.handle(DefaultAsyncResult.VOID_SUCCESS);
            }
            else
            {
                result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
                        "IncomingLinkRef '%s' doesn't match any incoming links.", incomingLinkRef),
                        ErrorCode.INVALID_LINK_REF)));
            }

        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
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
            _logger.info(format("Service method accept called with msgRef=%s", msgRef));
        }
        return updateDelivery(msgRef, MessageDisposition.ACCEPTED, result);
    }

    @Override
    public AmqpService reject(String msgRef, Handler<AsyncResult<Void>> result)
    {
        if (_logger.isInfoEnabled())
        {
            _logger.info(format("Service method reject called with msgRef=%s", msgRef));
        }
        return updateDelivery(msgRef, MessageDisposition.REJECTED, result);
    }

    @Override
    public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result)
    {

        return updateDelivery(msgRef, MessageDisposition.RELEASED, result);
    }

    AmqpService updateDelivery(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result)
    {
        try
        {
            _linkManager.settleDelivery(msgRef, disposition);
            result.handle(DefaultAsyncResult.VOID_SUCCESS);
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
                    "Error {%s}, when marking message={ref: %s} as %s", e.getMessage(), msgRef, disposition), e
                    .getErrorCode())));
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

        try
        {
            String id = _linkManager.getOutgoingLinkId(amqpAddress);
            if (id != null)
            {
                // Link already exists, send the existing ref.
                _logger.info(format("Outgoing link to AMQP-message-sink '%s' already exists. Returning link ref '%s'",
                        amqpAddress, id));
                // TODO handle the else here
                if (_outgoingLinkRefs.containsKey(id))
                {
                    OutgoingLinkRef ref = _outgoingLinkRefs.get(id);
                    ref.addEventBusAddress(eventbusAddress);
                    if (notificationAddress != null && !notificationAddress.trim().isEmpty())
                    {
                        ref.addNotificationAddress(notificationAddress);
                    }
                }
            }
            else
            {
                id = _linkManager.createOutgoingLink(amqpAddress, options);
                _outgoingLinkRefs.put(id, new OutgoingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress,
                        result));
                _logger.info(format(
                        "Created outgoing link from vertx-amqp-bridge to AMQP-message-sink '%s'. The link ref is '%s'",
                        amqpAddress, id));
            }
            _router.addOutboundRoute(eventbusAddress, amqpAddress);
            _eb.consumer(eventbusAddress, this);
            result.handle(new DefaultAsyncResult<String>(id));
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<String>(e));
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
                _logger.info(format("Service method cancelOutgoingLink called with outgoingLinkRef=%s", outgoingLinkRef));
            }
            if (_outgoingLinkRefs.containsKey(outgoingLinkRef))
            {
                _linkManager.closeIncomingLink(_incomingLinkRefs.get(outgoingLinkRef)._amqpAddr);
                result.handle(DefaultAsyncResult.VOID_SUCCESS);
            }
            else
            {
                result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
                        "OutgoingLinkRef '%s' doesn't match any incoming links.", outgoingLinkRef),
                        ErrorCode.INVALID_LINK_REF)));
            }

        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
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
            _logger.info(format("Received msg : {address : %s, reply-to : %s, body : %s} ", vertxMsg.address(),
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
                    _linkManager.send(_config.getDefaultOutboundAddress(), msg, vertxMsg.body());
                    _logger.info("No matching address, sending to default outbound address");
                }
                catch (MessagingException e)
                {
                    _logger.error(format("Error {code=%s, msg='%s'} sending to default outbound address %s",
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
                _logger.debug(format("Received msg from vertx [to=%s, reply-to=%s, body=%s] ", vertxMsg.address(),
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
                try
                {
                    _linkManager.send(amqpAddress, msg, vertxMsg.body());
                }
                catch (MessagingException e)
                {
                    _logger.error(format("Error {code=%s, msg='%s'} sending to AMQP address %s", e.getErrorCode(),
                            e.getMessage(), amqpAddress));
                }
            }
        }
        catch (MessagingException e)
        {
            _logger.error(format("Error {code=%s, msg='%s'} routing outbound", e.getErrorCode(), e.getMessage()));
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
                _logger.info(format("Incoming Link '%s' is ready. Notifying handler", id));
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
                _logger.info(format("Outgoing Link '%s' is ready. Notifying handler", id));
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
    public void message(String id, String linkTarget, String peerAddress, ReliabilityMode reliability,
            InboundMessage inMsg)
    {
        JsonObject outMsg;
        try
        {
            outMsg = _msgTranslator.convert(inMsg.getProtocolMessage());
        }
        catch (MessageFormatException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        outMsg.put("vertx.amqp.from", peerAddress);
        outMsg.put(INCOMING_MSG_REF, inMsg.getMsgRef());

        // Handle replyTo
        if (handleReplyTo(linkTarget, inMsg, outMsg))
        {
            // it was a reply-to and has been handled. No further routing
            // required.
            return;
        }

        String key = null;
        switch (_config.getInboundRoutingPropertyType())
        {
        case LINK_TARGET:
            key = linkTarget;
            break;
        case SUBJECT:
            key = inMsg.getSubject();
            break;
        case MESSAGE_ID:
            key = inMsg.getMessageId().toString();
            break;
        case CORRELATION_ID:
            key = inMsg.getCorrelationId().toString();
            break;
        case ADDRESS:
            key = inMsg.getAddress();
            break;
        case REPLY_TO:
            key = inMsg.getReplyTo();
            break;
        case CUSTOM:
            key = (String) inMsg.getApplicationProperties().get(_config.getInboundRoutingPropertyName());
            break;
        }

        // Fallback
        if (key == null)
        {
            _logger.warn(String
                    .format("Inboud routing property type '%s' specified in config is not present in the message. Using %s instead",
                            _config.getInboundRoutingPropertyType(), InboundRoutingPropertyType.LINK_TARGET));
            key = linkTarget;
        }

        List<String> addressList = _router.routeInbound(key);
        for (String address : addressList)
        {
            if (inMsg.getReplyTo() != null)
            {
                _eb.send(address, outMsg, new ReplyHandler(inMsg.getReplyTo()));
            }
            else
            {
                _eb.send(address, outMsg);
            }
        }
        if (_logger.isInfoEnabled())
        {
            _logger.info("\n============= Inbound Routing ============");
            _logger.info(format("Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s'",
                    inMsg.getAddress(), inMsg.getReplyTo(), inMsg.getContent(), peerAddress));
            _logger.info(format("Inbound routing info [key=%s, value=%s]", _config.getInboundRoutingPropertyType(), key));
            _logger.info("Matched the following vertx address list : " + addressList);
            _logger.info("============= /Inbound Routing ============\n");
        }
    }

    private boolean handleReplyTo(String linkTarget, InboundMessage inMsg, JsonObject outMsg)
    {
        String replyToKey = null;
        if (inMsg.getAddress() == null)
        {
            replyToKey = linkTarget;
        }
        else
        {
            try
            {
                ConnectionSettings settings = _linkManager.getConnectionSettings(inMsg.getAddress());
                replyToKey = settings.getNode() != null ? settings.getNode() : settings.getHost();

            }
            catch (MessagingException e)
            {
                _logger.error(format("Error {code=%s, msg='%s'} parsing address field in AMQP message",
                        e.getErrorCode(), e.getMessage()));
            }

        }

        if (_vertxReplyTo.containsKey(replyToKey))
        {
            if (_logger.isInfoEnabled())
            {
                _logger.info("\n============= Inbound Routing (Reply-to) ============");
                _logger.info(format("Received reply-message [address=%s, body=%s] from AMQP peer", replyToKey,
                        inMsg.getContent()));
                _logger.info("It's a reply to vertx message with reply-to=" + replyToKey);
                _logger.info("============= /Inbound Routing (Reply-to) ============\n");
            }
            try
            {
                Message<JsonObject> request = _vertxReplyTo.get(replyToKey);
                request.reply(outMsg);
                _vertxReplyTo.remove(replyToKey);
                request = null;
                return true;
            }
            catch (Exception e)
            {
                _logger.error(format("Error {msg='%s'} replying to vertx msg", e.getMessage()));
            }
        }
        return false;
    }

    @Override
    public void outgoingLinkCreditGiven(String id, int credits)
    {
        if (_outgoingLinkRefs.containsKey(id))
        {
            for (int i=0; i < credits; i++)
            {
                _eb.publish(_outgoingLinkRefs.get(id).nextCreditRecipeintAddr(), NotificationMessageFactory.credit(id, 1));
            }
        }
        else
        {
            _logger.warn(format("Error : Credit received for link not in map. Details [link-ref : '%s']", id));
        }
    }// ----------- \ LinkEventListener ------------

    private void sendNotificatonMessage(String id, JsonObject msg)
    {
        for (String notificationAddr : _outgoingLinkRefs.get(id)._notificationAddrList)
        {
            _eb.publish(notificationAddr, msg);
        }
    }

    // ---------- Helper classes
    class ReplyHandler implements Handler<AsyncResult<Message<JsonObject>>>
    {
        String _replyTo;

        ReplyHandler(String replyTo)
        {
            _replyTo = replyTo;
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
                _linkManager.send(_replyTo, _msgTranslator.convert(msg.body()), msg.body());
            }
            catch (MessagingException e)
            {
                _logger.error(format("Error {code=%s, msg='%s'} handling reply", e.getErrorCode(), e.getMessage()));
            }
        }
    }

    class IncomingLinkRef
    {
        final String _id;

        final String _amqpAddr;

        final List<String> _ebAddrList = new ArrayList<String>();

        final List<String> _notificationAddrList = new ArrayList<String>();

        final Handler<AsyncResult<String>> _resultHandler;

        IncomingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr,
                Handler<AsyncResult<String>> resultHandler)
        {
            _id = id;
            _amqpAddr = amqpAddr;
            _ebAddrList.add(ebAddr);
            _notificationAddrList.add(notificationAddr);
            _resultHandler = resultHandler;
        }

        void addEventBusAddress(String addr)
        {
            _ebAddrList.add(addr);
        }

        void removeEventBusAddress(String addr)
        {
            _ebAddrList.remove(addr);
        }

        void addNotificationAddress(String addr)
        {
            _notificationAddrList.add(addr);
        }

        void removeNotificationAddress(String addr)
        {
            _notificationAddrList.remove(addr);
        }
    }

    class OutgoingLinkRef
    {
        final String _id;

        final String _amqpAddr;

        final List<String> _ebAddrList = new ArrayList<String>();

        final List<String> _notificationAddrList = new ArrayList<String>();

        final Handler<AsyncResult<String>> _resultHandler;

        int _nextCreditRecipient = 0;
 
        OutgoingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr,
                Handler<AsyncResult<String>> resultHandler)
        {
            _id = id;
            _amqpAddr = amqpAddr;
            _ebAddrList.add(ebAddr);
            _notificationAddrList.add(notificationAddr);
            _resultHandler = resultHandler;
        }

        void addEventBusAddress(String addr)
        {
            _ebAddrList.add(addr);
        }

        void removeEventBusAddress(String addr)
        {
            _ebAddrList.remove(addr);
        }

        void addNotificationAddress(String addr)
        {
            _notificationAddrList.add(addr);
        }

        void removeNotificationAddress(String addr)
        {
            _notificationAddrList.remove(addr);
        }
        
        String nextCreditRecipeintAddr()
        {
            if (_nextCreditRecipient + 1 == _notificationAddrList.size())
            {
                _nextCreditRecipient = 0;
            }
            else
            {
                _nextCreditRecipient++;
            }
            return _notificationAddrList.get(_nextCreditRecipient);    
        }
    }
}