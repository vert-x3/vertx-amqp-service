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
import io.vertx.ext.amqp.*;
import io.vertx.ext.amqp.impl.protocol.InboundMessage;
import io.vertx.ext.amqp.impl.protocol.LinkEventListener;
import io.vertx.ext.amqp.impl.protocol.LinkManager;
import io.vertx.ext.amqp.impl.protocol.MessageDisposition;
import io.vertx.ext.amqp.impl.routing.LinkRouter;
import io.vertx.ext.amqp.impl.routing.MessageRouter;
import io.vertx.ext.amqp.impl.translators.MessageTranslator;
import io.vertx.ext.amqp.impl.util.Functions;
import io.vertx.ext.amqp.impl.util.LogManager;
import io.vertx.ext.amqp.impl.util.LogMsgHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.vertx.ext.amqp.impl.util.Functions.format;
import static io.vertx.ext.amqp.impl.util.Functions.print;

/**
 * The service impl makes use of the LinkManager for AMQP connection/link
 * management and the Router for routing logic.
 *
 * @author rajith
 */
public class AmqpServiceImpl implements Handler<Message<JsonObject>>, LinkEventListener, AmqpService {
  private static final LogManager LOG = LogManager.get("AMQP-VERTX-BRIDGE:", AmqpServiceImpl.class);

  private final Vertx _vertx;

  private final EventBus _eb;

  private final Map<String, Message<JsonObject>> _vertxReplyTo = new ConcurrentHashMap<String, Message<JsonObject>>();

  private final AmqpServiceConfig _config;

  private final String _replyToAddressPrefix;

  private final List<MessageConsumer<JsonObject>> _consumers = new ArrayList<MessageConsumer<JsonObject>>();

  private Map<String, IncomingLinkRef> _incomingLinkRefs = new HashMap<String, IncomingLinkRef>();

  private Map<String, OutgoingLinkRef> _outgoingLinkRefs = new HashMap<String, OutgoingLinkRef>();

  private Map<String, ServiceRef> _serviceRefs = new HashMap<String, ServiceRef>();

  private Map<String, String> _replyToNotices = new HashMap<String, String>();

  private final LinkRouter _linkBasedRouter;

  private final MessageRouter _msgBasedRouter;

  private final LinkManager _linkManager;

  private final MessageTranslator _msgTranslator;

  private final Verticle _parent;

  public AmqpServiceImpl(Vertx vertx, AmqpServiceConfig config, Verticle parent) throws MessagingException {
    _vertx = vertx;
    _parent = parent;
    _eb = _vertx.eventBus();
    _config = config;
    _msgTranslator = new MessageTranslator();
    _msgBasedRouter = new MessageRouter(_config);
    _linkBasedRouter = new LinkRouter();
    _linkManager = new LinkManager(vertx, _config, this);
    _replyToAddressPrefix = "amqp://" + _config.getInboundHost() + ":" + _config.getInboundPort();

    _eb.consumer(config.getDefaultHandlerAddress(), this);
    for (String handlerAddress : config.getHandlerAddressList()) {
      _consumers.add(_eb.consumer(handlerAddress, this));
    }
    // TODO _config.print() // prints the current config at start time.
  }

  public void stopInternal() {
    try {
      _parent.stop(null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ------------- AmqpService -----------------
  @Override
  public void start() {
    // _linkManager.stop();
  }

  @Override
  public void stop() {
    _linkManager.stop();
  }

  @Override
  public AmqpService establishIncomingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
                                           IncomingLinkOptions options, Handler<AsyncResult<String>> result) {
    try {
      LOG.info(
        "Service method establishIncommingLink called with amqpAddress=%s, eventbusAddress=%s, notificationAddress=%s, options=%s",
        amqpAddress, eventbusAddress, notificationAddress, options);
      String id = _linkManager.createIncomingLink(amqpAddress, options);
      _linkBasedRouter.addIncomingRoute(id, eventbusAddress);
      _incomingLinkRefs.put(id,
        new IncomingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress, result));
      LOG.info("Created incoming link from AMQP-message-soure to vertx-amqp-bridge '%s'. The link ref is '%s'",
        amqpAddress, id);
      result.handle(new DefaultAsyncResult<String>(id));
    } catch (MessagingException e) {
      result.handle(new DefaultAsyncResult<String>(e));
    }
    return this;
  }

  @Override
  public AmqpService fetch(String incomingLinkRef, int messages, Handler<AsyncResult<Void>> result) {
    try {
      LOG.info("Service method fetch called with incomingLinkRef=%s, messages=%s", incomingLinkRef, messages);
      _linkManager.setCredits(incomingLinkRef, messages);
      result.handle(DefaultAsyncResult.VOID_SUCCESS);
    } catch (MessagingException e) {
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Error {%s}, when fetching messages from incoming link : %s.", e.getMessage(), incomingLinkRef), e
        .getErrorCode())));
    }
    return this;
  }

  @Override
  public AmqpService cancelIncomingLink(String incomingLinkRef, Handler<AsyncResult<Void>> result) {
    try {
      LOG.info("Service method cancelIncommingLink called with incomingLinkRef=%s", incomingLinkRef);
      _linkManager.closeIncomingLink(incomingLinkRef);
      result.handle(DefaultAsyncResult.VOID_SUCCESS);

    } catch (MessagingException e) {
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Error {%s}, when cancelling incoming link : %s.", e.getMessage(), incomingLinkRef), e
        .getErrorCode())));
    }
    return this;
  }

  @Override
  public AmqpService accept(String msgRef, Handler<AsyncResult<Void>> result) {
    LOG.info("Service method accept called with msgRef=%s", msgRef);
    return updateDelivery(msgRef, MessageDisposition.ACCEPTED, result);
  }

  @Override
  public AmqpService reject(String msgRef, Handler<AsyncResult<Void>> result) {
    LOG.info("Service method reject called with msgRef=%s", msgRef);
    return updateDelivery(msgRef, MessageDisposition.REJECTED, result);
  }

  @Override
  public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result) {
    LOG.info("Service method release called with msgRef=%s", msgRef);
    return updateDelivery(msgRef, MessageDisposition.RELEASED, result);
  }

  AmqpService updateDelivery(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result) {
    try {
      _linkManager.settleDelivery(msgRef, disposition);
      result.handle(DefaultAsyncResult.VOID_SUCCESS);
    } catch (MessagingException e) {
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Error {%s}, when marking message={ref: %s} as %s", e.getMessage(), msgRef, disposition), e
        .getErrorCode())));
    }
    return this;
  }

  @Override
  public AmqpService establishOutgoingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
                                           OutgoingLinkOptions options, Handler<AsyncResult<String>> result) {
    LOG.info(
      "Service method establishOutgoingLink called with amqpAddress=%s, eventbusAddress=%s, notificationAddress=%s, options=%s",
      amqpAddress, eventbusAddress, notificationAddress, options);

    try {
      String id = _linkManager.createOutgoingLink(amqpAddress, options);
      _linkBasedRouter.addOutgoingRoute(eventbusAddress, id);
      _eb.consumer(eventbusAddress, this);

      _outgoingLinkRefs.put(id,
        new OutgoingLinkRef(id, amqpAddress, eventbusAddress, notificationAddress, result));
      LOG.info("Created outgoing link from vertx-amqp-bridge to AMQP-message-sink '%s'. The link ref is '%s'",
        amqpAddress, id);
      result.handle(new DefaultAsyncResult<String>(id));
    } catch (MessagingException e) {
      result.handle(new DefaultAsyncResult<String>(e));
    }
    return this;
  }

  @Override
  public AmqpService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result) {
    try {
      LOG.info("Service method cancelOutgoingLink called with outgoingLinkRef=%s", outgoingLinkRef);
      _linkManager.closeIncomingLink(outgoingLinkRef);
      result.handle(DefaultAsyncResult.VOID_SUCCESS);
    } catch (MessagingException e) {
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Error {%s}, when cancelling outgoing link : %s.", e.getMessage(), outgoingLinkRef), e
        .getErrorCode())));
    }
    return this;
  }

  public AmqpService registerService(String eventbusAddress, String notificationAddress, ServiceOptions options,
                                     Handler<AsyncResult<Void>> result) {
    LOG.info("Service method registerService called with eventbusAddress=%s, options=%s", eventbusAddress, options);

    if (_serviceRefs.containsKey(eventbusAddress)) {
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Address '%s' is already in use by another service", eventbusAddress), ErrorCode.ALREADY_EXISTS)));
    } else {
      _serviceRefs.put(eventbusAddress,
        new ServiceRef(eventbusAddress, notificationAddress, options.getInitialCapacity()));
      result.handle(DefaultAsyncResult.VOID_SUCCESS);
      LOG.info(format("Registered Service at address=%s with options=%s", eventbusAddress, options));
    }
    return this;
  }

  public AmqpService issueCredits(String linkId, int credits, Handler<AsyncResult<Void>> result) {
    LOG.info("Service method issueCredits called with linkId=%s, credits=%s", linkId, credits);
    try {
      _linkManager.setCredits(linkId, credits);
      result.handle(DefaultAsyncResult.VOID_SUCCESS);
    } catch (MessagingException e) {
      LOG.warn(e, "Error issueing credits for link %s", linkId);
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Service : '%s',  Error issueing credits for link %s", linkId), ErrorCode.INVALID_LINK_REF)));
    }
    return this;
  }

  @Fluent
  public AmqpService unregisterService(String eventbusAddress, Handler<AsyncResult<Void>> result) {
    LOG.info("Service method unregisterService called with eventbusAddress=%s", eventbusAddress);
    if (_serviceRefs.containsKey(eventbusAddress)) {
      ServiceRef service = _serviceRefs.remove(eventbusAddress);
      for (String id : service._inLinks) {
        try {
          _linkManager.closeIncomingLink(id);
        } catch (MessagingException e) {
          LOG.warn(e, "UnregisterService : '%s',  Error closing incoming link %s", eventbusAddress, id);
        }
      }
      for (String id : service._outLinks) {
        try {
          _linkManager.closeOutgoingLink(id);
        } catch (MessagingException e) {
          LOG.warn(e, "UnregisterService : '%s', Error closing outgoing link %s", eventbusAddress, id);
        }
      }
      result.handle(DefaultAsyncResult.VOID_SUCCESS);
    } else {
      result.handle(new DefaultAsyncResult<Void>(new MessagingException(format(
        "Address '%s' doesn't match any registered service", eventbusAddress), ErrorCode.ADDRESS_NOT_FOUND)));
    }
    return this;
  }

  public AmqpService addInboundRoute(String pattern, String eventbusAddress) {
    _msgBasedRouter.addInboundRoute(pattern, eventbusAddress);
    return this;
  }

  public AmqpService removeInboundRoute(String pattern, String eventbusAddress) {
    _msgBasedRouter.removeInboundRoute(pattern, eventbusAddress);
    return this;
  }

  public AmqpService addOutboundRoute(String pattern, String amqpAddress) {
    _msgBasedRouter.addOutboundRoute(pattern, amqpAddress);
    return this;
  }

  public AmqpService removeOutboundRoute(String pattern, String amqpAddress) {
    _msgBasedRouter.addOutboundRoute(pattern, amqpAddress);
    return this;
  }// ------------\ AmqpService -----------------

  // -- Handler method for receiving messages from the event-bus -----------
  @Override
  public void handle(Message<JsonObject> vertxMsg) {
    try {
      LOG.debug(format("Received msg from Vert.x event bus : {address : %s, reply-to : %s, body : %s} ", vertxMsg.address(),
        vertxMsg.replyAddress(), vertxMsg.body() == null ? "" : vertxMsg.body().encodePrettily()));
      org.apache.qpid.proton.message.Message outMsg = _msgTranslator.convert(vertxMsg.body());
      JsonObject inMsg = vertxMsg.body();

      if (outMsg.getReplyTo() == null && vertxMsg.replyAddress() != null) {
        outMsg.setReplyTo(_replyToAddressPrefix + "/" + vertxMsg.replyAddress());
        _vertxReplyTo.put(vertxMsg.replyAddress(), vertxMsg);
      }

      // First attempt link routing (covers links created via Service API)
      String linkId = _linkBasedRouter.routeOutgoing(vertxMsg.address());
      if (linkId != null) {
        try {
          _linkManager.sendViaLink(linkId, outMsg, inMsg);
          LogMsgHelper.logVertxMsgForLinkBasedRouting(LOG, vertxMsg, linkId);
        } catch (MessagingException e) {
          LOG.warn(e, "Error {code=%s, msg='%s'} sending to link %s", e.getErrorCode(), e.getMessage(),
            linkId);
        }
      } else {
        // Message based routing (routes added through static or dynamic
        // config)
        List<String> amqpAddressList = _msgBasedRouter.routeOutgoing(vertxMsg);
        for (String amqpAddress : amqpAddressList) {
          try {
            _linkManager.sendViaAddress(amqpAddress, outMsg, inMsg);
          } catch (MessagingException e) {
            LOG.warn(e, "Error {code=%s, msg='%s'} sending to AMQP address %s", e.getErrorCode(),
              e.getMessage(), amqpAddress);
          }
        }
        LogMsgHelper.logVertxMsgForMsgBasedRouting(LOG, vertxMsg, amqpAddressList);
      }
    } catch (MessagingException e) {
      LOG.warn(e, "Error {code=%s, msg='%s'} routing outbound", e.getErrorCode(), e.getMessage());
    }
  }// ------------- \ Event bus handler -----------

  // ------------- LinkEventListener -----------
  @Override
  public void incomingLinkReady(String id, String address, boolean isFromInboundConnection) {
    print("incomingLinkReady inbound=%s, id=%s , address=%s", isFromInboundConnection, id, address);
    if (isFromInboundConnection) {
      if (_serviceRefs.containsKey(address)) {
        LOG.info("Mapping service address %s to incoming-link %s", address, id);
        ServiceRef service = _serviceRefs.get(address);
        String notificationAddress = service._notificationAddr;
        _incomingLinkRefs.put(id, new IncomingLinkRef(id, null, null, notificationAddress, null));
        _linkBasedRouter.addIncomingRoute(id, address);
        sendNotificatonMessage(notificationAddress, NotificationMessageFactory.incomingLinkOpened(id));
      } else {
        try {
          _linkManager.setCredits(id, 1);
        } catch (MessagingException e) {
          LOG.warn(e, "Error setting credits for link %s", id);
        }
      }
    } else {
      if (_incomingLinkRefs.containsKey(id)) {
        _incomingLinkRefs.get(id)._resultHandler.handle(new DefaultAsyncResult<String>(id));
        LOG.info("Incoming Link '%s' is ready. Notifying handler", id);
      }
    }
  }

  @Override
  public void incomingLinkFinal(String id, String address, boolean isFromInboundConnection) {
    print("Incoming Link closed inbound=%s", isFromInboundConnection);
    if (isFromInboundConnection) {
      if (_serviceRefs.containsKey(address)) {
        LOG.info("Notifying service %s incoming-link %s is closed", address, id);
        ServiceRef service = _serviceRefs.get(address);
        String notificationAddress = service._notificationAddr;
        _incomingLinkRefs.remove(id);
        sendNotificatonMessage(notificationAddress, NotificationMessageFactory.incomingLinkClosed(id));
      }
    } else {
      if (_incomingLinkRefs.containsKey(id)) {
        IncomingLinkRef linkRef = _incomingLinkRefs.get(id);
        String notificationAddress = linkRef._notificationAddr;
        _incomingLinkRefs.remove(id);
        LOG.info("Sending notification msg to '%s' : incoming-link %s closed", notificationAddress, id);
        sendNotificatonMessage(notificationAddress, NotificationMessageFactory.incomingLinkClosed(id));
      }
    }

  }

  @Override
  public void outgoingLinkReady(String id, String address, boolean isFromInboundConnection) {
    print("outgoingLinkReady inbound=%s, id=%s , address=%s", isFromInboundConnection, id, address);
    if (isFromInboundConnection) {
      if (_serviceRefs.containsKey(address)) {
        LOG.info("Mapping service address %s to outgoing-link %s", address, id);
        ServiceRef service = _serviceRefs.get(address);
        String notificationAddress = service._notificationAddr;
        _outgoingLinkRefs.put(id, new OutgoingLinkRef(id, null, null, notificationAddress, null));
        sendNotificatonMessage(notificationAddress, NotificationMessageFactory.outgoingLinkOpened(id));
      } else {
        LOG.info("Mapping address %s to outgoing-link %s", address, id);
        _linkBasedRouter.addOutgoingRoute(address, id);
        _eb.consumer(address, this);
        _outgoingLinkRefs.put(id, new OutgoingLinkRef(id, null, null, null, null));
      }
    } else {
      if (_outgoingLinkRefs.containsKey(id)) {
        _outgoingLinkRefs.get(id)._resultHandler.handle(new DefaultAsyncResult<String>(id));
        LOG.info("Outgoing Link '%s' is ready. Notifying handler", id);
      }
    }
  }

  @Override
  public void outgoingLinkFinal(String id, String address, boolean isFromInboundConnection) {
    print("Outgoing Link closed outbound=%s", isFromInboundConnection);
    if (isFromInboundConnection) {
      if (_serviceRefs.containsKey(address)) {
        LOG.info("Notifying service %s outgoing-link %s closed", address, id);
        ServiceRef service = _serviceRefs.get(address);
        String notificationAddress = service._notificationAddr;
        _outgoingLinkRefs.remove(id);
        sendNotificatonMessage(notificationAddress, NotificationMessageFactory.outgoingLinkClosed(id));
      } else {
        _linkBasedRouter.removeOutgoingRoute(id);
        // TODO cancel event-bus consume.
      }
    } else {
      if (_outgoingLinkRefs.containsKey(id)) {
        OutgoingLinkRef linkRef = _outgoingLinkRefs.get(id);
        String notificationAddress = linkRef._notificationAddr;
        _outgoingLinkRefs.remove(id);
        LOG.info("Sending notification msg to '%s' : outgoing-link %s closed", notificationAddress, id);
        sendNotificatonMessage(notificationAddress, NotificationMessageFactory.outgoingLinkClosed(id));
      }
    }
  }

  @Override
  public void deliveryUpdate(String id, String msgRef, DeliveryState state, MessageDisposition disp) {
    print("Delivery update received for link=%s and msg-ref=%s", id, msgRef);
    //print("_replyToNotices : %s", _replyToNotices);
    if (_replyToNotices.containsKey(msgRef)) {
      sendNotificatonMessage(_replyToNotices.remove(msgRef),
        NotificationMessageFactory.deliveryState(msgRef, state, disp));
    } else if (_outgoingLinkRefs.containsKey(id)) {
      sendNotificatonMessage(_outgoingLinkRefs.get(id)._notificationAddr,
        NotificationMessageFactory.deliveryState(msgRef, state, disp));
    } else {
      LOG.warn(
        "Error : Delivery update received for link not in map. Details [msg-ref : '%s' tied to link-ref : '%s']",
        msgRef, id);
    }
  }

  @Override
  public void message(String linkId, String linkAddress, ReliabilityMode reliability, InboundMessage inMsg) {
    JsonObject outMsg;
    try {
      outMsg = _msgTranslator.convert(inMsg.getProtocolMessage());
    } catch (MessageFormatException e) {
      LOG.warn(e, "Error translating AMQP message %s ", inMsg);
      return;
    }
    outMsg.put(INCOMING_MSG_REF, inMsg.getMsgRef());
    outMsg.put(INCOMING_MSG_LINK_REF, linkId);

    // Handle replyTo
    if (handleReplyTo(linkAddress, inMsg, outMsg)) {
      // it was a reply-to and has been handled. No further routing
      // required.
      return;
    }

    print("Received AMQP msg with reply-to : %s", inMsg.getReplyTo());
    String vertxAddress = _linkBasedRouter.routeIncoming(linkId);
    if (vertxAddress != null) {
      print("xxxxxxxxxxx doing link based routing %s", _serviceRefs);
      if (inMsg.getReplyTo() != null) {
        String notificaitonAddress = null;
        print("zzzzzzzzzzzzzzzzzzzzzzzzz service-refs %s", _serviceRefs);
        if (_serviceRefs.containsKey(vertxAddress)) {
          notificaitonAddress = _serviceRefs.get(vertxAddress)._notificationAddr;
        } else if (_incomingLinkRefs.containsKey(linkId)) {
          notificaitonAddress = _incomingLinkRefs.get(linkId)._notificationAddr;
        }
        _eb.send(vertxAddress, outMsg, new ReplyHandler(inMsg.getReplyTo(), notificaitonAddress));
      } else {
        _eb.send(vertxAddress, outMsg);
      }
      LogMsgHelper.logAmqpMsgForLinkBasedRouting(LOG, inMsg, linkId, vertxAddress);
    } else {
      List<String> addressList = _msgBasedRouter.routeIncoming(inMsg, linkAddress);
      for (String address : addressList) {
        if (inMsg.getReplyTo() != null) {
          _eb.send(address, outMsg, new ReplyHandler(inMsg.getReplyTo(), null));
        } else {
          _eb.send(address, outMsg);
        }
      }
      LogMsgHelper.logAmqpMsgForMsgBasedRouting(LOG, inMsg, linkId, addressList);
    }
  }

  private boolean handleReplyTo(String linkAddress, InboundMessage inMsg, JsonObject outMsg) {
    String replyToKey = null;
    if (inMsg.getAddress() == null) {
      replyToKey = linkAddress;
    } else {
      try {
        ConnectionSettings settings = _linkManager.getConnectionSettings(inMsg.getAddress());
        replyToKey = settings.getNode() != null ? settings.getNode() : settings.getHost();

      } catch (MessagingException e) {
        LOG.warn(e, "Error {code=%s, msg='%s'} parsing address field in AMQP message", e.getErrorCode(),
          e.getMessage());
      }
    }

    if (_vertxReplyTo.containsKey(replyToKey)) {
      LogMsgHelper.logInboundReplyTo(LOG, inMsg, replyToKey);
      try {
        Message<JsonObject> request = _vertxReplyTo.remove(replyToKey);
        request.reply(outMsg);
        request = null;
        return true;
      } catch (Exception e) {
        LOG.warn(e, "Error {msg='%s'} replying to vertx msg", e.getMessage());
      }
    }
    return false;
  }

  @Override
  public void outgoingLinkCreditGiven(String id, int credits) {
    if (_outgoingLinkRefs.containsKey(id)) {
      if (_outgoingLinkRefs.get(id)._notificationAddr != null) {
        sendNotificatonMessage(_outgoingLinkRefs.get(id)._notificationAddr,
          NotificationMessageFactory.credit(id, credits));
      }
    } else {
      LOG.warn("Error : Credit received for link not in map. Details [link-ref : '%s']", id);
    }
  }// ----------- \ LinkEventListener ------------

  private void sendNotificatonMessage(String address, JsonObject msg) {
    if (address != null) {
      _eb.send(address, msg);
    }
  }

  // ---------- Helper classes
  // TODO need to handle replyTo more efficiently.
  class ReplyHandler implements Handler<AsyncResult<Message<JsonObject>>> {
    String _replyTo;
    String _notificationAddr;

    ReplyHandler(String replyTo, String notificationAddr) {
      _replyTo = replyTo;
      _notificationAddr = notificationAddr;
    }

    @Override
    public void handle(AsyncResult<Message<JsonObject>> result) {
      Message<JsonObject> msg = result.result();
      try {
        print("Reply received from Vert.x event-bus %s ", msg.body() == null ? "" : msg.body().encodePrettily());
        LogMsgHelper.logOutboundReplyTo(LOG, msg, _replyTo);
        org.apache.qpid.proton.message.Message out = _msgTranslator.convert(msg.body());
        String linkId = _linkBasedRouter.routeOutgoing(Functions.extractDestination(_replyTo));
        print("_replyTo=%s, linkId=%s", _replyTo, linkId);
        if (linkId != null) {
          try {
            _linkManager.sendViaLink(linkId, out, msg.body());
            LOG.info(" vertx-amqp-service is hosting reply-to destination '%s'", _replyTo);
          } catch (MessagingException e) {
            LOG.warn(e, "Error {code=%s, msg='%s'} sending to link %s", e.getErrorCode(), e.getMessage(),
              linkId);
          }
        } else {
          _linkManager.sendViaAddress(_replyTo, out, msg.body());
        }
        if (_notificationAddr != null && msg.body().containsKey(AmqpService.OUTGOING_MSG_REF)) {
          print("Adding the reply-to:notification pair {%s : %s}", msg.body().getString(AmqpService.OUTGOING_MSG_REF), _notificationAddr);
          _replyToNotices.put(msg.body().getString(AmqpService.OUTGOING_MSG_REF), _notificationAddr);
        }
      } catch (MessagingException e) {
        LOG.warn(e, "Error {code=%s, msg='%s'} handling reply", e.getErrorCode(), e.getMessage());
      }
    }
  }

  class IncomingLinkRef {
    final String _id;

    final String _amqpAddr;

    final String _ebAddr;

    final String _notificationAddr;

    final Handler<AsyncResult<String>> _resultHandler;

    IncomingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr,
                    Handler<AsyncResult<String>> resultHandler) {
      _id = id;
      _amqpAddr = amqpAddr;
      _ebAddr = ebAddr;
      _notificationAddr = notificationAddr;
      _resultHandler = resultHandler;
    }
  }

  class OutgoingLinkRef {
    final String _id;

    final String _amqpAddr;

    final String _ebAddr;

    final String _notificationAddr;

    final Handler<AsyncResult<String>> _resultHandler;

    OutgoingLinkRef(String id, String amqpAddr, String ebAddr, String notificationAddr,
                    Handler<AsyncResult<String>> resultHandler) {
      _id = id;
      _amqpAddr = amqpAddr;
      _ebAddr = ebAddr;
      _notificationAddr = notificationAddr;
      _resultHandler = resultHandler;
    }
  }

  class ServiceRef {
    final String _serviceAddr;

    final String _notificationAddr;

    final List<String> _inLinks = new ArrayList<String>();

    final List<String> _outLinks = new ArrayList<String>();

    ServiceRef(String serviceAddr, String notificationAddress, int initialCapacity) {
      _serviceAddr = serviceAddr;
      _notificationAddr = notificationAddress;
    }

    void addIncomingLink(String linkId) {
      _inLinks.add(linkId);
    }

    void removeIncomingLink(String linkId) {
      _inLinks.remove(linkId);
    }

    void addOutgoingLink(String linkId) {
      _outLinks.add(linkId);
    }

    void removeOutgoingLink(String linkId) {
      _outLinks.remove(linkId);
    }
  }
}