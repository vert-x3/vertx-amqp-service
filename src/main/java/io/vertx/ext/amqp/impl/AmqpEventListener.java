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
 * A convenience interface for internal implementations.
 */
public interface AmqpEventListener
{
    void onConnectionOpen(ConnectionImpl con);

    void onConnectionClosed(ConnectionImpl conn);

    void onSessionOpen(SessionImpl ssn);

    void onSessionClosed(SessionImpl ssn);

    void onOutboundLinkOpen(OutboundLinkImpl link);

    void onOutboundLinkClosed(OutboundLinkImpl link);

    void onOutboundLinkCredit(OutboundLinkImpl link, int credits);

    void onClearToSend(OutboundLinkImpl link);

    void onSettled(TrackerImpl tracker);

    void onInboundLinkOpen(InboundLinkImpl link);

    void onInboundLinkClosed(InboundLinkImpl link);

    void onCreditOffered(InboundLinkImpl link, int offered);

    void onMessage(InboundLinkImpl link, InboundMessage msg);
}