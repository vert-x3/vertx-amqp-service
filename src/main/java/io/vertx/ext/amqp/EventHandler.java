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

public interface EventHandler
{
    void onConnectionOpen(Connection con);

    void onConnectionClosed(Connection conn);

    void onSessionOpen(Session ssn);

    void onSessionClosed(Session ssn);

    void onOutboundLinkOpen(OutboundLink link);

    void onOutboundLinkClosed(OutboundLink link);

    void onOutboundLinkCredit(OutboundLink link, int credits);

    void onClearToSend(OutboundLink link);

    void onSettled(Tracker tracker);

    void onInboundLinkOpen(InboundLink link);

    void onInboundLinkClosed(InboundLink link);

    void onCreditOffered(InboundLink link, int offered);

    void onMessage(InboundLink link, AmqpMessage msg);
}
