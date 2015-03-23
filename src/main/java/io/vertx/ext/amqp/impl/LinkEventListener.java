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

/* 
 * Internal interface to help delegate events to Service while
 * hiding link management in LinkManager 
 */
public interface LinkEventListener
{
    void incomingLinkReady(String linkName, String address, boolean isFromInboundConnection);

    void incomingLinkFinal(String linkName, String address, boolean isFromInboundConnection);

    void outgoingLinkReady(String linkName, String address, boolean isFromInboundConnection);

    void outgoingLinkFinal(String linkName, String address, boolean isFromInboundConnection);

    void message(String linkName, InboundMessage msg);

    void outgoingLinkCreditGiven(String linkName, String address, int credits);
}