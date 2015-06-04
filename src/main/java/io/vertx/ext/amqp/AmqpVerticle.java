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
import io.vertx.ext.amqp.impl.AmqpServiceConfig;
import io.vertx.ext.amqp.impl.AmqpServiceImpl;
import io.vertx.ext.amqp.impl.config.AmqpServiceConfigImpl;
import io.vertx.serviceproxy.ProxyHelper;

public class AmqpVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmqpVerticle.class);

  private AmqpServiceImpl service;

  @Override
  public void start() throws Exception {
    super.start();
    AmqpServiceConfig config = new AmqpServiceConfigImpl(config());
    try {
      service = new AmqpServiceImpl(vertx, config, this);
      String address = config().getString("address");
      if (address == null) {
        throw new IllegalStateException("address field must be specified in config for service verticle");
      }

      ProxyHelper.registerService(AmqpService.class, vertx, service, address);
      service.start();
      LOGGER.info(String.format("AmqpService is now available via the address : %s", address));
    } catch (MessagingException e) {
      LOGGER.fatal("Exception when starting AMQP Service", e);
    }
  }

  @Override
  public void stop() throws Exception {
    service.stop();
    LOGGER.warn("Stopping AMQP Service");
    super.stop();
  }
}