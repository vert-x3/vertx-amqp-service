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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public class AmqpVerticle extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(AmqpVerticle.class);

    private Router _router;

    @Override
    public void start() throws Exception
    {
        super.start();
        RouterConfig config = new RouterConfig(config());
        try
        {
            _router = new Router(vertx, new MessageFactory(), config);
        }
        catch (MessagingException e)
        {
            logger.fatal("Exception when starting router", e);
        }
    }

    @Override
    public void stop() throws Exception
    {
        _router.stop();
        super.stop();
    }
}