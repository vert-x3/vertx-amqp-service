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
 * 
 * For more information on AMQP visit www.amqp.org This service speaks AMQP 1.0
 * and use QPid Proton(http://qpid.apache.org/proton) for protocol support.
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Attapattu</a>
 */
@VertxGen
@ProxyGen
public interface AmqpService
{
    /**
     * Unique identifier given by the vertx-amqp-service to an incoming message.
     * It can be accessed via msg.getString(AmqpService.INCOMING_MSG_REF), where
     * msg is the Json message received by the vert.x application.
     */
    public static final String INCOMING_MSG_REF = "vertx.amqp.incoming-msg-ref";

    /**
     * Unique identifier given by vert.x application to an outgoing message. It
     * can be set as msg.put(AmqpService.INCOMING_MSG_REF), where msg is the
     * outgoing Json message sent by the vert.x application.
     */
    public static final String OUTGOING_MSG_REF = "vertx.amqp.outgoing-msg-ref";

    /**
     * Unique identifier given by the vertx-amqp-service to the logical Link
     * established between an AMQP peer and a vert.x subscription. This allows
     * an application to identify the source of a message if it is receiving
     * from multiple sources. If the application established this link
     * themselves, the identity returned via the AsyncResult via the handler in
     * {@link AmqpService#establishIncommingLink(String, String, String, IncomingLinkOptions, Handler)}
     * is the same reference, so you could correlate the message received to the
     * link created.
     */
    public static final String INCOMING_MSG_LINK_REF = "vertx.amqp.incoming-msg-link-ref";

    static AmqpService createEventBusProxy(Vertx vertx, String address)
    {
        return ProxyHelper.createProxy(AmqpService.class, vertx, address);
    }

    /**
     * Allows an application to establish a link to an AMQP message-source for
     * receiving messages. The vertx-amqp-service will receive the messages on
     * behalf of the application and forward it to the event-bus address
     * specified in the consume method. The application will be listening on
     * this address.
     * 
     * @param amqpAddress
     *            A link will be created to the the AMQP message-source
     *            identified by this address. .
     * @param eventbusAddress
     *            The event-bus address to be mapped to the above link. The
     *            application should register a handler for this address on the
     *            event bus to receive the messages.
     * @param notificationAddress
     *            The event-bus address to which notifications about the
     *            incoming link is sent. Ex. Errors. The application should
     *            register a handler with the event-bus to receive these
     *            updates. Please see {@link NotificationType} and
     *            {@link NotificationHelper} for more details.
     * @param options
     *            Options to configure the link behavior (Ex prefetch,
     *            reliability). {@link IncommingLinkOptions}
     * @param result
     *            The AsyncResult contains a ref (string) to the mapping
     *            created. This is required when changing behavior or canceling
     *            the link and it' association.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService establishIncommingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
            IncomingLinkOptions options, Handler<AsyncResult<String>> result);

    /**
     * If prefetch was set to zero, this method allows the application to
     * explicitly fetch a certain number of messages. If prefetch > 0, the AMQP
     * service will prefetch messages for you automatically.
     * 
     * @see IncomingLinkOptions on how to set prefetch.
     * 
     * @param incomingLinkRef
     *            The String ref return by the establishIncommingLink method.
     *            This uniquely identifies the incoming link and it's mapping to
     *            an event-bus address.
     * @param messages
     *            The number of message to fetch.
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService fetch(String incomingLinkRef, int messages, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to cancel an incoming link and remove it's mapping
     * to an event-bus address.
     * 
     * @param incomingLinkRef
     *            The String ref return by the establishIncommingLink method.
     *            This uniquely identifies the incoming link and it's mapping to
     *            an event-bus address.
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService cancelIncommingLink(String incomingLinkRef, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to establish a link to an AMQP message-sink for
     * sending messages. The application will send the messages to the event-bus
     * address. The AMQP service will receive these messages via the event-bus
     * and forward it to the respective AMQP message sink.
     * 
     * @param amqpAddress
     *            A link will be created to the the AMQP message-sink identified
     *            by this address.
     * @param eventbusAddress
     *            The event-bus address to be mapped to the above link. The
     *            application should send the messages using this address.
     * @param notificationAddress
     *            The event-bus address to which notifications about the
     *            outgoing link is sent. Ex. Errors, Delivery Status, credit
     *            availability. The application should register a handler with
     *            the event-bus to receive these updates. Please see
     *            {@link NotificationType} and {@link NotificationHelper} for
     *            more details.
     * @param options
     *            Options to configure the link behavior (Ex reliability).
     *            {@link IncommingLinkOptions}
     * @param result
     *            The AsyncResult contains a ref (string) to the mapping
     *            created. This is required when changing behavior or canceling
     *            the link and it' association.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService establishOutgoingLink(String amqpAddress, String eventbusAddress, String notificationAddress,
            OutgoingLinkOptions options, Handler<AsyncResult<String>> result);

    /**
     * Allows an application to cancel an outgoing link and remove it's mapping
     * to an event-bus address.
     * 
     * @param outgoingLinkRef
     *            The String ref return by the establishOutgoingLink method.
     *            This uniquely identifies the outgoing link and it's mapping to
     *            an event-bus address.
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService cancelOutgoingLink(String outgoingLinkRef, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to accept a message it has received.
     * 
     * @param msgRef
     *            The string ref. Use {@link AmqpMessage#getMsgRef()}
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService accept(String msgRef, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to reject a message it has received.
     * 
     * @param msgRef
     *            The string ref. Use {@link AmqpMessage#getMsgRef()}
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService reject(String msgRef, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to release a message it has received.
     * 
     * @param msgRef
     *            The string ref. Use {@link AmqpMessage#getMsgRef()}
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result);

    /**
     * Allows a vertx.application to register a Service it provides with the
     * vertx-amqp-service. This allows any AMQP peer to interact with this
     * service by sending (and receiving) messages with the service.
     * 
     * @param eventbusAddress
     *            The event-bus address the service is listening for incoming
     *            requests. The application needs to register a handler with the
     *            event-bus using this address to receive the above requests.
     * @param notificationAddres
     *            The event-bus address to which notifications about the service
     *            is sent. The application should register a handler with the
     *            event-bus to receive these updates. Ex notifies the
     *            application of an incoming link created by an AMQP peer to
     *            send requests. Please see {@link NotificationType} and
     *            {@link NotificationHelper} for more details.
     * @param options
     *            Options to configure the Service behavior (Ex initial
     *            capacity). {@link ServiceOptions}
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService registerService(String eventbusAddress, String notificationAddres, ServiceOptions options,
            Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to unregister a service with vertx-amqp-service.
     * 
     * @param eventbusAddress
     *            The event-bus address used when registering the service
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService unregisterService(String eventbusAddress, Handler<AsyncResult<Void>> result);

    /**
     * Allows the service to issue credits to a particular incoming link
     * (created by a remote AMQP peer) for sending more service requests. This
     * allows the Service to always be in control of how many messages it
     * receives so it can maintain the required QoS requirements.
     * 
     * @param linkId
     *            The ref for the incoming link. The service gets notified of an
     *            incoming link by registering for notifications. Please
     *            {@link NotificationType#INCOMING_LINK_OPENED} and
     *            {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)}
     *            for more details.
     * @param credits
     *            The number of message (requests) the AMQP peer is allowed to
     *            send.
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService issueCredits(String linkId, int credits, Handler<AsyncResult<Void>> result);

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