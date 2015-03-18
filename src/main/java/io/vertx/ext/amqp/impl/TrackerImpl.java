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

import io.vertx.ext.amqp.DeliveryState;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.MessageDisposition;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.Session;
import io.vertx.ext.amqp.Tracker;

import java.util.concurrent.TimeUnit;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;

class TrackerImpl implements Tracker
{
    private MessageDisposition _disposition = MessageDisposition.UNKNOWN;

    private DeliveryState _state = DeliveryState.UNKNOWN;

    private ConditionManager _pending = new ConditionManager(true);

    private boolean _settled = false;

    private Session _ssn;

    private Object _ctx;

    TrackerImpl(Session ssn)
    {
        _ssn = ssn;
    }

    @Override
    public DeliveryState getState()
    {
        return _state;
    }

    @Override
    public MessageDisposition getDisposition()
    {
        return _disposition;
    }

    public void awaitSettlement(int... flags) throws MessagingException
    {
        _pending.waitUntilFalse();
        if (_state == DeliveryState.LINK_FAILED)
        {
            throw new MessagingException(
                    "The link has failed due to the underlying network connection failure. The message associated with this delivery is in-doubt",
                    ErrorCode.LINK_FAILED);
        }
    }

    public void awaitSettlement(long timeout, TimeUnit unit, int... flags) throws MessagingException, TimeoutException
    {
        try
        {
            _pending.waitUntilFalse(unit.toMillis(timeout));
            if (_state == DeliveryState.LINK_FAILED)
            {
                throw new MessagingException(
                        "The link has failed due to the underlying network connection failure. The message associated with this delivery is in-doubt",
                        ErrorCode.LINK_FAILED);
            }
        }
        catch (ConditionManagerTimeoutException e)
        {
            throw new TimeoutException("The delivery was not settled within the given time period", e);
        }
    }

    @Override
    public boolean isSettled()
    {
        return _settled;
    }

    void markSettled()
    {
        _settled = true;
        _state = DeliveryState.SETTLED;
        _pending.setValueAndNotify(false);
    }

    void setDisposition(MessageDisposition disp)
    {
        _disposition = disp;
    }

    void setDisposition(org.apache.qpid.proton.amqp.transport.DeliveryState state)
    {
        if (state instanceof Accepted)
        {
            _disposition = MessageDisposition.ACCEPTED;
        }
        else if (state instanceof Released)
        {
            _disposition = MessageDisposition.RELEASED;
        }
        else if (state instanceof Rejected)
        {
            _disposition = MessageDisposition.REJECTED;
        }
    }

    void markLinkFailed()
    {
        _state = DeliveryState.LINK_FAILED;
        _pending.setValueAndNotify(false);
    }

    Object getContext()
    {
        return _ctx;
    }

    void setContext(Object ctx)
    {
        _ctx = ctx;
    }
}