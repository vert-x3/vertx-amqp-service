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
import io.vertx.core.json.JsonObject;
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
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 */
@VertxGen
@ProxyGen
public interface AmqpService
{
    static AmqpService createEventBusProxy(Vertx vertx, String address)
    {
        return ProxyHelper.createProxy(AmqpService.class, vertx, address);
    }

    /**
     * Allows an application to establish a link to an AMQP message-source for
     * receiving messages. The service will receive the messages on behalf of
     * the application and forward it to the event-bus address specified in the
     * consume method. The application will be listening on this address.
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
     *            updates.
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
     *            the event-bus to receive these updates.
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
     *            - The string ref. Use {@link AmqpMessage#getMsgRef()}
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
     *            - The string ref. Use {@link AmqpMessage#getMsgRef()}
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
     *            - The string ref. Use {@link AmqpMessage#getMsgRef()}
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService release(String msgRef, Handler<AsyncResult<Void>> result);

    /**
     * Start the service
     */
    @ProxyIgnore
    public void start();

    /**
     * Stop the service
     */
    @ProxyIgnore
    public void stop();
}