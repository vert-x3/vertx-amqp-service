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

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;

/**
 * AMQP service allows you to handle incoming and outgoing connections to send
 * and receive messages.
 * 
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 */
@VertxGen
@ProxyGen
public interface AmqpService
{
    /*
     * static AmqpService createEventBusProxy(Vertx vertx, String address) {
     * return ProxyHelper.createProxy(AmqpService.class, vertx, address); }
     */

    /**
     * Attempts to create a connection to an AMQP peer identified by the
     * <code>ConnectionSettings<code>.
     * The result is notified via the resultHandler.
     * 
     * A mandatory event-handler is required to notify the application of the various AMQP events
     * related to the connection or the objects created off the connection (Session, Out/InboundLinks ..etc).
     * 
     * @param settings
     *            configures the host:port and other behavior for the
     *            connection.
     * @param resultHandler
     *            the handler which is called when the <code>Connection<code> is
     *            ready to use (or if it fails).
     * @param eventHanlder
     *            the handler notifies the application of the various AMQP
     *            events related to the connection or the objects created off
     *            the connection (Session, Out/InboundLinks ..etc) See
     *            <code>AmqpEvent<code> for the type of events.
     */
    public void connect(ConnectionSettings settings, Handler<AsyncResult<Connection>> resultHandler,
            Handler<AmqpEvent> eventHandler) throws VertxException;

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