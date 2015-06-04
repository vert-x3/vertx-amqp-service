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

import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.MessagingException;

/**
 * Thrown when an operation fails to complete within the given timeout.
 */
@SuppressWarnings("serial")
public class TimeoutException extends MessagingException {
  public TimeoutException(String msg) {
    super(msg, ErrorCode.OPERATION_TIMED_OUT);
  }

  public TimeoutException(String msg, Throwable t) {
    super(msg, t, ErrorCode.OPERATION_TIMED_OUT);
  }
}