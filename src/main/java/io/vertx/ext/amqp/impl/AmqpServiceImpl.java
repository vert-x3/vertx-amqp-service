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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.AmqpEvent;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.Connection;
import io.vertx.ext.amqp.ConnectionSettings;

public class AmqpServiceImpl implements AmqpService
{
    public AmqpServiceImpl(Vertx vertx, JsonObject config)
    {

    }

    @Override
    public void start()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void stop()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void connect(ConnectionSettings settings, Handler<AsyncResult<Connection>> resultHandler,
            Handler<AmqpEvent> eventHandler) throws VertxException
    {
        if (resultHandler == null)
        {
            throw new VertxException("Result Handler cannot be null");
        }

        if (eventHandler == null)
        {
            throw new VertxException("Event Handler cannot be null");
        }

    }
}