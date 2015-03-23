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
 * Determines how and when message fetched by the AMQP-Service.
 */
public enum CreditMode
{
    /**
     * If this option is chosen, the application only needs to set the prefetch
     * value using {@link IncomingLinkOptions#setPrefetch(int)}. Once set, the
     * AMQP-Service will automatically fetch more messages when a certain number
     * of messages are marked as either accepted, rejected or released. The
     * AMQP-Service will determine the optimum threshold for when the fetch
     * happens and how much to fetch.
     */
    AUTO,

    /**
     * If this option is chosen, the application needs to explicitly manage it's
     * capacity by fetching the desired number of messages via
     * {@link AmqpService#fetch(String, int, io.vertx.core.Handler)}, once it is
     * ready to process more.
     */
    EXPLICT;
}