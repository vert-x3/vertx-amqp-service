/**
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

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.amqp.impl.protocol.AmqpMessage;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * AMQP service allows a Vert.x application to,
 * <ul>
 * <li>Establish and cancel incoming/outgoing AMQP links, and map the link it to
 * an event-bus address.</li>
 * <li>Configure the link behavior</li>
 * <li>Control the flow of messages both incoming and outgoing to maintain QoS</li>
 * <li>Send and Receive messages from AMQP peers with different reliability
 * guarantees</li>
 * </ul>
 * <p/>
 * For more information on AMQP visit www.amqp.org This service speaks AMQP 1.0
 * and use QPid Proton(http://qpid.apache.org/proton) for protocol support.
 *
 * @author <a href="mailto:rajith@rajith.lk">Rajith Attapattu</a>
 */
@VertxGen
@ProxyGen
public interface AMQPService {
  /**
   * Unique identifier given by the vertx-amqp-service to an incoming message.
   * It can be accessed via msg.getString(AmqpService.INCOMING_MSG_REF), where
   * msg is the Json message received by the vert.x application.
   */
  String INCOMING_MSG_REF = "vertx.amqp.incoming-msg-ref";

  /**
   * Unique identifier given by vert.x application to an outgoing message. It
   * can be set as msg.put(AmqpService.INCOMING_MSG_REF), where msg is the
   * outgoing Json message sent by the vert.x application.
   */
  String OUTGOING_MSG_REF = "vertx.amqp.outgoing-msg-ref";

  /**
   * Unique identifier given by the vertx-amqp-service to the logical Link
   * established between an AMQP peer and a vert.x subscription. This allows
   * an application to identify the source of a message if it is receiving
   * from multiple sources. If the application established this link
   * themselves, the identity returned via the AsyncResult via the handler in
   * {@link AMQPService#establishIncomingLink(String, String, String, IncomingLinkOptions, Handler)}
   * is the same reference, so you could correlate the message received to the
   * link created.
   */
  String INCOMING_MSG_LINK_REF = "vertx.amqp.incoming-msg-link-ref";

  static AMQPService createEventBusProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(AMQPService.class, vertx, address);
  }

  /**
   * Allows an application to establish a link to an AMQP message-source for
   * receiving messages. The vertx-amqp-service will receive the messages on
   * behalf of the application and forward it to the event-bus address
   * specified in the consume method. The application will be listening on
   * this address.
   *
   * @param amqpAddress         A link will be created to the the AMQP message-source
   *                            identified by this address. .
   * @param eventbusAddress     The event-bus address to be mapped to the above link. The
   *                            application should register a handler for this address on the
   *                            event bus to receive the messages.
   * @param notificationAddress The event-bus address to which notifications about the
   *                            incoming link is sent. Ex. Errors. The application should
   *                            register a handler with the event-bus to receive these
   *                            updates. Please see {@link NotificationType} and
   *                            {@link NotificationHelper} for more details.
   * @param options             Options to configure the link behavior (Ex prefetch,
   *                            reliability). {@link IncomingLinkOptions}
   * @param result              The AsyncResult contains a ref (string) to the mapping
   *                            created. This is required when changing behavior or canceling
   *                            the link and it' association.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService establishIncomingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
                                           IncomingLinkOptions options, Handler<AsyncResult<String>> result);

  /**
   * If prefetch was set to zero, this method allows the application to
   * explicitly fetch a certain number of messages. If prefetch > 0, the AMQP
   * service will prefetch messages for you automatically.
   *
   * @param incomingLinkRef The String ref return by the establishIncommingLink method.
   *                        This uniquely identifies the incoming link and it's mapping to
   *                        an event-bus address.
   * @param messages        The number of message to fetch.
   * @param result          Notifies if there is an error.
   * @return A reference to the service.
   * @see IncomingLinkOptions on how to set prefetch.
   */
  @Fluent
  public AMQPService fetch(String incomingLinkRef, int messages, Handler<AsyncResult<Void>> result);

  /**
   * Allows an application to cancel an incoming link and remove it's mapping
   * to an event-bus address.
   *
   * @param incomingLinkRef The String ref return by the establishIncommingLink method.
   *                        This uniquely identifies the incoming link and it's mapping to
   *                        an event-bus address.
   * @param result          Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService cancelIncomingLink(String incomingLinkRef, Handler<AsyncResult<Void>> result);

  /**
   * Allows an application to establish a link to an AMQP message-sink for
   * sending messages. The application will send the messages to the event-bus
   * address. The AMQP service will receive these messages via the event-bus
   * and forward it to the respective AMQP message sink.
   *
   * @param amqpAddress         A link will be created to the the AMQP message-sink identified
   *                            by this address.
   * @param eventbusAddress     The event-bus address to be mapped to the above link. The
   *                            application should send the messages using this address.
   * @param notificationAddress The event-bus address to which notifications about the
   *                            outgoing link is sent. Ex. Errors, Delivery Status, credit
   *                            availability. The application should register a handler with
   *                            the event-bus to receive these updates. Please see
   *                            {@link NotificationType} and {@link NotificationHelper} for
   *                            more details.
   * @param options             Options to configure the link behavior (Ex reliability).
   *                            {@link IncomingLinkOptions}
   * @param result              The AsyncResult contains a ref (string) to the mapping
   *                            created. This is required when changing behavior or canceling
   *                            the link and it' association.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService establishOutgoingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
                                           OutgoingLinkOptions options, Handler<AsyncResult<String>> result);

  /**
   * Allows an application to cancel an outgoing link and remove it's mapping
   * to an event-bus address.
   *
   * @param outgoingLinkRef The String ref return by the establishOutgoingLink method.
   *                        This uniquely identifies the outgoing link and it's mapping to
   *                        an event-bus address.
   * @param result          Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result);

  /**
   * Allows an application to accept a message it has received.
   *
   * @param msgRef The string ref. Use {@link AmqpMessage#getMsgRef()}
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService accept(String msgRef, Handler<AsyncResult<Void>> result);

  /**
   * Allows an application to reject a message it has received.
   *
   * @param msgRef The string ref. Use {@link AmqpMessage#getMsgRef()}
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService reject(String msgRef, Handler<AsyncResult<Void>> result);

  /**
   * Allows an application to release a message it has received.
   *
   * @param msgRef The string ref. Use {@link AmqpMessage#getMsgRef()}
   * @param result Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService release(String msgRef, Handler<AsyncResult<Void>> result);

  /**
   * Allows a vertx.application to register a Service it provides with the
   * vertx-amqp-service. This allows any AMQP peer to interact with this
   * service by sending (and receiving) messages with the service.
   *
   * @param eventbusAddress    The event-bus address the service is listening for incoming
   *                           requests. The application needs to register a handler with the
   *                           event-bus using this address to receive the above requests.
   * @param notificationAddres The event-bus address to which notifications about the service
   *                           is sent. The application should register a handler with the
   *                           event-bus to receive these updates. Ex notifies the
   *                           application of an incoming link created by an AMQP peer to
   *                           send requests. Please see {@link NotificationType} and
   *                           {@link NotificationHelper} for more details.
   * @param options            Options to configure the Service behavior (Ex initial
   *                           capacity). {@link ServiceOptions}
   * @param result             Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService registerService(String eventbusAddress, String notificationAddres, ServiceOptions options,
                                     Handler<AsyncResult<Void>> result);

  /**
   * Allows an application to unregister a service with vertx-amqp-service.
   *
   * @param eventbusAddress The event-bus address used when registering the service
   * @param result          Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService unregisterService(String eventbusAddress, Handler<AsyncResult<Void>> result);

  /**
   * Allows the service to issue credits to a particular incoming link
   * (created by a remote AMQP peer) for sending more service requests. This
   * allows the Service to always be in control of how many messages it
   * receives so it can maintain the required QoS requirements.
   *
   * @param linkId  The ref for the incoming link. The service gets notified of an
   *                incoming link by registering for notifications. Please
   *                {@link NotificationType#INCOMING_LINK_OPENED} and
   *                {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)}
   *                for more details.
   * @param credits The number of message (requests) the AMQP peer is allowed to
   *                send.
   * @param result  Notifies if there is an error.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService issueCredits(String linkId, int credits, Handler<AsyncResult<Void>> result);

  /**
   * Adds an entry to the inbound routing table. If an existing entry exists
   * under the same pattern, the event-bus address will be added to the list.
   *
   * @param pattern         The pattern to be matched against the chosen message-property
   *                        from the incoming message.
   * @param eventBusAddress The Vert.x event-bus address the message should be sent to if
   *                        matched.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService addInboundRoute(String pattern, String eventBusAddress);

  /**
   * Removes the entry from the inbound routing table.
   *
   * @param pattern         The pattern (key) used when adding the entry to the table.
   * @param eventBusAddress The Vert.x event-bus address the message should be sent to if
   *                        matched.
   * @return
   */
  @Fluent
  public AMQPService removeInboundRoute(String pattern, String eventBusAddress);

  /**
   * Adds an entry to the outbound routing table. If an existing entry exists
   * under the same pattern, the amqp address will be added to the list.
   *
   * @param pattern     The pattern to be matched against the chosen message-property
   *                    from the outgoing message.
   * @param amqpAddress The AMQP address the message should be sent to if matched.
   * @return A reference to the service.
   */
  @Fluent
  public AMQPService addOutboundRoute(String pattern, String amqpAddress);

  /**
   * Removes the entry from the outbound routing table.
   *
   * @param pattern     The pattern (key) used when adding the entry to the table.
   * @param amqpAddress The AMQP address the message should be sent to if matched.
   * @return
   */
  @Fluent
  public AMQPService removeOutboundRoute(String pattern, String amqpAddress);

  /**
   * Start the vertx-amqp-service
   */
  @ProxyIgnore
  public void start();

  /**
   * Stop the vertx-amqp-service
   */
  @ProxyIgnore
  public void stop();
}