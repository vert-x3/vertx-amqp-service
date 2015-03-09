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
import io.vertx.ext.amqp.impl.AmqpServiceImpl2;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * AMQP service allows you to directly use API methods to subscribe, publish,
 * issue credits and message acks without having to use control-messages via the
 * event bus.
 * 
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 */
@VertxGen
@ProxyGen
public interface AmqpService
{
    static AmqpService create(Vertx vertx, JsonObject config) 
    {
        return new AmqpServiceImpl2(vertx, config); 
    }

    static AmqpService createEventBusProxy(Vertx vertx, String address) 
    {
         return ProxyHelper.createProxy(AmqpService.class, vertx, address); 
    }

    /**
     * Allows an application to create a subscription to an AMQP message source.
     * The service will receive the messages on behalf of the application and
     * forward it to the event-bus address specified in the consume method. The
     * application will be listening on this address.
     * 
     * @param amqpAddress
     *            The address that identifies the AMQP message source to
     *            subscribe from.
     * @param ebAddress
     *            The event-bus address the application is listening on for the
     *            messages.
     * @param receiverMode
     *            Specified the reliability expected.
     * @param creditMode
     *            Specifies how credit is replenished.
     * @param result
     *            AsyncResult that contains a String ref to the AMQP 'consumer',
     *            if successfully created.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService consume(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode,
            Handler<AsyncResult<String>> result);

    /**
     * Allows an application to issue message credits for flow control purposes.
     * 
     * @see CreditMode to understand how msg credits works.
     * 
     * @param consumerRef
     *            The String ref return by the consume method.
     * @param credits
     *            The message credits
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService issueCredit(String consumerRef, int credits, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to cancel a subscription it has previously created.
     * 
     * @param consumerRef
     *            The String ref return by the consume method.
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService unregisterConsume(String consumerRef, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to acknowledge a message and set it's disposition.
     * 
     * @param msgRef
     *            - The string ref. Use {@link AmqpMessage#getMsgRef()}
     * @param disposition
     *            - One of ACCEPT, REJECT OR RELEASED.
     * @param result
     *            Notifies if there is an error.
     * @return A reference to the service.
     */
    @Fluent
    public AmqpService acknowledge(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result);

    /**
     * Allows an application to publish a message to an AMQP target.
     * 
     * @param address
     *            The AMQP target to which the messages should be sent.
     * @param msg
     *            The message to be sent.
     * @param result
     *            A JsonObject containing the delivery state and disposition.
     * @return
     */
    @Fluent
    public AmqpService publish(String address, JsonObject msg, Handler<AsyncResult<JsonObject>> result);

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