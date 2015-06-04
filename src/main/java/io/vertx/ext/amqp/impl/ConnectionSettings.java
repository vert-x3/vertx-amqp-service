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

/**
 * Applications could implement this interface to provide their own
 * implementation of the ConnectionSettings based on their configuration. They
 * could also use the DefaultConnectionSettings and filling in the details using
 * the setter methods.
 */

public interface ConnectionSettings {
  public String getHost();

  public int getPort();

  public String getId();

  public boolean isTcpNodelay();

  public int getReadBufferSize();

  public int getWriteBufferSize();

  public long getConnectTimeout();

  public long getIdleTimeout();

  public void setTarget(String target);

  public String getNode();
}