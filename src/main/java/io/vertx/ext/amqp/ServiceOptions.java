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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Allows a vert.x application to customize the registration of a service.
 * Future extension point to add more options.
 *
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 */
@DataObject
public class ServiceOptions {
  /**
   * Allows a service to request vertx-amqp-service to issue credits
   * immediately upon the establishment of an incoming link from an AMQP peer
   * (client). If not set, it defaults to zero. The AMQP peers (clients) will
   * not be able to send messages (requests) until the "service" explicitly
   * issues credits using
   * {@link AmqpService#issueCredits(String, int, io.vertx.core.Handler)}
   */
  public final static String INITIAL_CAPACITY = "initial-capacity";

  private int initialCapacity = 0;

  public ServiceOptions() {
  }

  public ServiceOptions(ServiceOptions options) {
    this.initialCapacity = options.initialCapacity;
  }

  public ServiceOptions(JsonObject options) {
    this.initialCapacity = options.getInteger(INITIAL_CAPACITY, 1);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(INITIAL_CAPACITY, initialCapacity);
    return json;
  }

  public int getInitialCapacity() {
    return initialCapacity;
  }

  public void setInitialCapacity(int capacity) {
    this.initialCapacity = capacity;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}