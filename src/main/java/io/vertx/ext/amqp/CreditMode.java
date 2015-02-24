/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.vertx.ext.amqp;

/**
 * Determines how and when message credits are issued. 
 * 
 * If the CreditMode is {@link CreditMode#AUTO}, the application only needs to
 * set the capacity once and library will automatically replenish credits when a
 * certain number of messages are marked as either accepted, rejected or
 * released. The library will determine the optimum threshold for when the
 * re-issue happens. <br>
 * 
 * If the CreditMode is {@link CreditMode#EXPLICT}, the application needs to
 * explicitly manage it's message credit and use
 * {@link OutboundLink#setCapacity(int)} to issue credits when it is ready to
 * process more messages.
 */
public enum CreditMode
{
    EXPLICT, AUTO;
}