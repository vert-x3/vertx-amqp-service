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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.Consumer;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.ReceiverMode;
import io.vertx.ext.amqp.Tracker;

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
    public AmqpService consume(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode,
            Handler<AsyncResult<Consumer>> result)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AmqpService issueCredit(Consumer consumer, int credits)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AmqpService publish(String address, JsonObject msg, Handler<AsyncResult<Tracker>> result)
    {
        // TODO Auto-generated method stub
        return null;
    }
}