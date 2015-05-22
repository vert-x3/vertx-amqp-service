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

/**
 * Classifies the type of Notification. Provides an easy way to handle
 * notification messages sent by the vertx-amqp-service. This works in
 * conjunction with {@link NotificationHelper}
 * 
 * See {@link PublishToQueueVerticle} and {@link FortuneCookieServiceVerticle}
 * in the examples to see how this is used.
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 */
public enum NotificationType
{
    /**
     * For this type of notification, use
     * {@link NotificationHelper#getDeliveryTracker(io.vertx.core.json.JsonObject)}
     * method to parse the message into a {@link DeliveryTracker} object which
     * provides info about a message delivery.
     */
    DELIVERY_STATE,

    /**
     * Notifies the application an outgoing link (messages traveling outbound)
     * from the vertx-amqp-service to an AMQP peer was created. Use
     * {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)} to
     * obtain the link reference.
     */
    OUTGOING_LINK_OPENED,

    /**
     * Notifies the application the outgoing link (messages traveling outbound)
     * from the vertx-amqp-service to an AMQP peer was closed. Use
     * {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)} to
     * obtain the link reference.
     */

    OUTGOING_LINK_CLOSED,

    /**
     * Notifies the application an incoming link (messages traveling inbound)
     * from an AMQP peer into vertx-amqp-service was created. Use
     * {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)} to
     * obtain the link reference.
     * 
     * This method is very useful for vert.x services registered with
     * vertx-amqp-service to keep track of new AMQP peers and manage them. Ex
     * flow control by issuing message credits.
     */

    INCOMING_LINK_OPENED,

    /**
     * Notifies the application the incoming link (messages traveling inbound)
     * from an AMQP peer into vertx-amqp-service was closed. Use
     * {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)} to
     * obtain the link reference.
     * 
     * This method is very useful for vert.x services registered with
     * vertx-amqp-service to keep track of new AMQP peers and manage them. Ex
     * cleanup resources.
     */
    INCOMING_LINK_CLOSED,

    /**
     * Notifies the application that the remote AMQP peer has granted message
     * credits so the application can send more messages. Use
     * {@link NotificationHelper#getCredits(io.vertx.core.json.JsonObject)} to
     * obtain the credits received.
     */
    LINK_CREDIT,

    /**
     * Notifies the application of an error associated with an incoming or
     * outgoing link. Use
     * {@link NotificationHelper#getErrorCode(io.vertx.core.json.JsonObject)} to
     * retrieve the error and
     * {@link NotificationHelper#getLinkRef(io.vertx.core.json.JsonObject)} to
     * retrieve the link ref.
     */
    LINK_ERROR;
};
