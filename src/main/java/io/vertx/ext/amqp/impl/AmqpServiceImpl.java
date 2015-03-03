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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.ReceiverMode;
import io.vertx.ext.amqp.Tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.qpid.proton.message.Message;

public class AmqpServiceImpl extends ConnectionManager implements AmqpService
{
    private static final Logger _logger = LoggerFactory.getLogger(AmqpServiceImpl.class);

    private final EventBus _eb;

    private Map<String, IncomingMsgRef> _inboundMsgRefs = new HashMap<String, IncomingMsgRef>();

    private Map<String, String> _consumerRefToEBAddr = new HashMap<String, String>();

    public AmqpServiceImpl(Vertx vertx, JsonObject config)
    {
        super(vertx);
        _eb = vertx.eventBus();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public AmqpService consume(String amqpAddress, String ebAddress, ReceiverMode receiverMode, CreditMode creditMode,
            Handler<AsyncResult<String>> result)
    {
        final ConnectionSettings settings = URLParser.parse(amqpAddress);
        try
        {
            ManagedConnection con = findConnection(settings);
            InboundLinkImpl link = con.createInboundLink(settings.getTarget(), receiverMode, creditMode);
            String id = UUID.randomUUID().toString();

            link.setContext(id);
            _inboundLinks.put(id, link);
            _consumerRefToEBAddr.put(id, ebAddress);
            _logger.info(String.format("Created inbound link {%s @ %s:%s} for ref '%s'", settings.getTarget(),
                    settings.getHost(), settings.getPort(), id));
            result.handle(new DefaultAsyncResult<String>(id));
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<String>(e));
        }

        return this;
    }

    @Override
    public AmqpService issueCredit(String consumerRef, int credits, Handler<AsyncResult<Void>> result)
    {
        if (_inboundLinks.containsKey(consumerRef))
        {
            try
            {
                _inboundLinks.get(consumerRef).setCredits(credits);
            }
            catch (MessagingException e)
            {
                result.handle(new DefaultAsyncResult<Void>(e));
            }
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid consumer reference : %s. Unable to find a matching AMQP link", consumerRef))));
        }
        return this;
    }

    @Override
    public AmqpService unregisterConsume(String consumerRef, Handler<AsyncResult<Void>> result)
    {
        if (_inboundLinks.containsKey(consumerRef))
        {
            _inboundLinks.get(consumerRef).close();
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid consumer reference : %s. Unable to find a matching AMQP link", consumerRef))));
        }
        return this;
    }

    @Override
    public AmqpService acknowledge(String msgRef, MessageDisposition disposition, Handler<AsyncResult<Void>> result)
    {
        if (_inboundMsgRefs.containsKey(msgRef))
        {
            IncomingMsgRef ref = _inboundMsgRefs.remove(msgRef);
            if (_inboundLinks.containsKey(ref._linkRef))
            {
                SessionImpl ssn = _inboundLinks.get(ref._linkRef).getSession();
                try
                {
                    ssn.disposition(ref._sequence, disposition);
                }
                catch (MessagingException e)
                {
                    result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                            "Exception when setting disposition for message reference : %s.", msgRef))));
                }
            }
        }
        else
        {
            result.handle(new DefaultAsyncResult<Void>(new MessagingException(String.format(
                    "Invalid message reference : %s. Unable to find a matching AMQP message", msgRef))));
        }
        return this;
    }

    @Override
    public AmqpService publish(String address, JsonObject msg, Handler<AsyncResult<JsonObject>> result)
    {
        try
        {
            OutboundLinkImpl link = findOutboundLink(address);
            Message m = _msgFactory.convert(msg);
            TrackerImpl tracker = (TrackerImpl) link.send(m);
            tracker.setContext(result);
        }
        catch (MessagingException e)
        {
            result.handle(new DefaultAsyncResult<JsonObject>(e));
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSettled(Tracker tracker)
    {
        TrackerImpl trackerImpl = (TrackerImpl) tracker;
        Handler<AsyncResult<JsonObject>> result = (Handler<AsyncResult<JsonObject>>) trackerImpl.getContext();
        JsonObject json = Functions.trackerToJson(tracker);
        result.handle(new DefaultAsyncResult<JsonObject>(json));
    }

    @Override
    public void onMessage(InboundLinkImpl link, InboundMessage msg)
    {
        JsonObject m = _msgFactory.convert(msg.getProtocolMessage());
        m.put("vertx.msg-ref", msg.getMsgRef());
        String consumerRef = (String) link.getContext();
        if (link.getReceiverMode() != ReceiverMode.AT_MOST_ONCE)
        {
            // TODO this map needs to be cleaned up if consumer is cancelled
            IncomingMsgRef ref = this.new IncomingMsgRef();
            ref._sequence = msg.getSequence();
            ref._linkRef = consumerRef;
            _inboundMsgRefs.put(msg.getMsgRef(), ref);
        }
        _eb.send(_consumerRefToEBAddr.get(consumerRef), m);
    }

    private class IncomingMsgRef
    {
        long _sequence;

        String _linkRef;
    }
}