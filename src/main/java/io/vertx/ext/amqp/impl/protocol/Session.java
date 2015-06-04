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
package io.vertx.ext.amqp.impl.protocol;

import io.vertx.ext.amqp.impl.CreditMode;
import io.vertx.ext.amqp.MessageFormatException;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.ReliabilityMode;


public interface Session {
  public OutgoingLink createOutboundLink(String address, ReliabilityMode mode) throws MessagingException;

  public IncomingLink createInboundLink(String address, ReliabilityMode mode, CreditMode creditMode)
    throws MessagingException;

  public void disposition(AmqpMessage msg, MessageDisposition disposition, int... flags)
    throws MessageFormatException, MessagingException;

  public void settle(AmqpMessage msg, int... flags) throws MessageFormatException, MessagingException;

  public void close();
}