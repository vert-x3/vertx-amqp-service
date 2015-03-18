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

public abstract class AbstractAmqpEventListener implements AmqpEventListener
{
    @Override
    public void onConnectionOpen(ConnectionImpl con)
    {
    }

    @Override
    public void onConnectionClosed(ConnectionImpl conn)
    {
    }

    @Override
    public void onSessionOpen(SessionImpl ssn)
    {
    }

    @Override
    public void onSessionClosed(SessionImpl ssn)
    {
    }

    @Override
    public void onOutboundLinkOpen(OutboundLinkImpl link)
    {
    }

    @Override
    public void onOutboundLinkClosed(OutboundLinkImpl link)
    {
    }

    @Override
    public void onOutboundLinkCredit(OutboundLinkImpl link, int credits)
    {
    }

    @Override
    public void onClearToSend(OutboundLinkImpl link)
    {
    }

    @Override
    public void onSettled(TrackerImpl tracker)
    {
    }

    @Override
    public void onInboundLinkOpen(InboundLinkImpl link)
    {
    }

    @Override
    public void onInboundLinkClosed(InboundLinkImpl link)
    {
    }

    @Override
    public void onCreditOffered(InboundLinkImpl link, int offered)
    {
    }

    @Override
    public void onMessage(InboundLinkImpl link, InboundMessage msg)
    {
    }

}