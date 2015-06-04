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
package io.vertx.ext.amqp.impl.util;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.impl.protocol.InboundMessage;

import java.util.List;

public class LogMsgHelper {
  private final static String OUTBOUND_MSG_BASED_ROUTING_LOG_FORMAT = outboundMsgBasedRoutingLogFormat();

  private final static String OUTBOUND_LINK_BASED_ROUTING_LOG_FORMAT = outboundLinkBasedRoutingLogFormat();

  private final static String INBOUND_LINK_BASED_ROUTING_LOG_FORMAT = inboundLinkBasedRoutingLogFormat();

  private final static String INBOUND_MSG_BASED_ROUTING_LOG_FORMAT = inboundMsgBasedRoutingLogFormat();

  private final static String INBOUND_REPLYTO_LOG_FORMAT = inboundReplyToLogFormat();

  private final static String OUTBOUND_REPLYTO_LOG_FORMAT = outboundReplyToLogFormat();

  public static String outboundLinkBasedRoutingLogFormat() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("{\n============= Outbound Routing ============");
    strBuilder.append("\nReceived msg from vertx [to=%s, reply-to=%s, body=%s] ");
    strBuilder.append("\nUsing link based routing");
    strBuilder.append("\nMatched the following AMQP address : %s");
    strBuilder.append("\n============= /Outbound Routing ============\n}");

    return strBuilder.toString();
  }

  public static String outboundMsgBasedRoutingLogFormat() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("{\n============= Outbound Routing ============");
    strBuilder.append("\nReceived msg from vertx [to=%s, reply-to=%s, body=%s] ");
    strBuilder.append("\nUsing message based routing");
    strBuilder.append("\nMatched the following AMQP address list : %s");
    strBuilder.append("\n============= /Outbound Routing ============\n}");

    return strBuilder.toString();
  }

  public static String inboundLinkBasedRoutingLogFormat() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("{\n============= Inbound Routing ============");
    strBuilder.append("\nReceived message [to=%s, reply-to=%s, body=%s] from AMQP link '%s'");
    strBuilder.append("\nUsing link based routing");
    strBuilder.append("\nMatched the following vertx address : %s");
    strBuilder.append("\n============= /Inbound Routing ============\n}");

    return strBuilder.toString();
  }

  public static String inboundMsgBasedRoutingLogFormat() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("{\n============= Inbound Routing ============");
    strBuilder.append("\nReceived message [to=%s, reply-to=%s, body=%s] from AMQP link '%s'");
    strBuilder.append("\nUsing message based routing");
    strBuilder.append("\nMatched the following vertx address list : %s");
    strBuilder.append("\n============= /Inbound Routing ============\n}");

    return strBuilder.toString();
  }

  public static String inboundReplyToLogFormat() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("{\n============= Inbound Routing (Reply-to) ============");
    strBuilder.append("\nReceived reply-message from AMQP peer [address=%s, body=%s]");
    strBuilder.append("\nIt's a reply to vertx message with reply-to=%s");
    strBuilder.append("\n============= /Inbound Routing (Reply-to) ============\n}");
    return strBuilder.toString();
  }

  public static String outboundReplyToLogFormat() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("{\n============= Outbound Routing (Reply-to) ============");
    strBuilder.append("\nReceived reply-message from Vert.x event-bus [address=%s, body=%s]");
    strBuilder.append("\nIt's a reply to AMQP message with reply-to=%s");
    strBuilder.append("\n============= /Outbound Routing (Reply-to) ============\n}");
    return strBuilder.toString();
  }

  public static void logVertxMsgForMsgBasedRouting(LogManager log, Message<JsonObject> vertxMsg,
                                                   List<String> amqpAddressList) {
    if (log.isDebugEnabled()) {
      log.debug(OUTBOUND_MSG_BASED_ROUTING_LOG_FORMAT, vertxMsg.address(), vertxMsg.replyAddress(), vertxMsg
        .body().encode(), vertxMsg.replyAddress(), amqpAddressList);
    }
  }

  public static void logVertxMsgForLinkBasedRouting(LogManager log, Message<JsonObject> vertxMsg, String linkAddress) {
    if (log.isDebugEnabled()) {
      log.debug(OUTBOUND_LINK_BASED_ROUTING_LOG_FORMAT, vertxMsg.address(), vertxMsg.replyAddress(), vertxMsg
        .body().encode(), vertxMsg.replyAddress(), linkAddress);
    }
  }

  public static void logAmqpMsgForLinkBasedRouting(LogManager log, InboundMessage inMsg, String linkId,
                                                   String vertxAddress) {
    if (log.isDebugEnabled()) {
      log.debug(INBOUND_LINK_BASED_ROUTING_LOG_FORMAT, inMsg.getAddress(), inMsg.getReplyTo(),
        inMsg.getContent(), linkId, vertxAddress);
    }
  }

  public static void logAmqpMsgForMsgBasedRouting(LogManager log, InboundMessage inMsg, String linkId,
                                                  List<String> vertxAddressList) {
    if (log.isDebugEnabled()) {
      log.debug(INBOUND_MSG_BASED_ROUTING_LOG_FORMAT, inMsg.getAddress(), inMsg.getReplyTo(), inMsg.getContent(),
        linkId, vertxAddressList);
    }
  }

  public static void logInboundReplyTo(LogManager log, InboundMessage inMsg, String replyTo) {
    if (log.isDebugEnabled()) {
      log.debug(INBOUND_REPLYTO_LOG_FORMAT, replyTo, inMsg.getAddress(), replyTo);
    }
  }

  public static void logOutboundReplyTo(LogManager log, Message<JsonObject> msg, String amqpReplyTo) {
    if (log.isDebugEnabled()) {
      log.debug(OUTBOUND_REPLYTO_LOG_FORMAT, msg.address(), msg.body().encode(), amqpReplyTo);
    }
  }
}
