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

import static io.vertx.ext.amqp.impl.util.Functions.print;

import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.ReliabilityMode;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;

class IncomingLinkImpl extends BaseLink implements IncomingLink
{
    private static int DEFAULT_CREDITS = 1;

    private final CreditMode _creditMode;

    private final ReliabilityMode _receiverMode;

    private int _credits = 0;

    private AtomicInteger _unsettled = new AtomicInteger(0);

    IncomingLinkImpl(SessionImpl ssn, String address, Link link, ReliabilityMode receiverMode, CreditMode creditMode)
    {
        super(ssn, address, link);
        _creditMode = creditMode;
        _receiverMode = receiverMode;
    }

    void init()
    {
        _link.open();
        /*if (_creditMode == CreditMode.AUTO && DEFAULT_CREDITS > 0)
        {
            _credits = DEFAULT_CREDITS;
            ((Receiver) _link).flow(_credits);
        }*/
        _ssn.getConnection().write();
    }

    void issueCredits(int credits, boolean drain)
    {
        Receiver receiver = (Receiver) _link;
        if (drain)
        {
            // receiver.setDrain(true);
        }
        receiver.flow(credits);
        print("\n==============================");
        print("\nSetting credits=%s, for link '%s'", credits, this.getName());
        print("\n==============================");
        _ssn.getConnection().write();
    }

    void decrementUnsettledCount()
    {
        _unsettled.decrementAndGet();
    }

    void issuePostReceiveCredit()
    {
        if (_creditMode == CreditMode.AUTO)
        {
            if (_credits == 1)
            {
                issueCredits(1, false);
            }
            else if (_unsettled.get() < _credits / 2)
            {
                issueCredits(_credits - _unsettled.get(), false);
            }
        }
    }

    @Override
    public ReliabilityMode getReceiverMode()
    {
        return _receiverMode;
    }

    @Override
    public CreditMode getCreditMode()
    {
        return _creditMode;
    }

    @Override
    public int getUnsettled() throws MessagingException
    {
        checkClosed();
        return _unsettled.get();
    }

    @Override
    public void setCredits(int credits) throws MessagingException
    {
        checkClosed();
        if (credits < 0)
        {
            throw new MessagingException("Capacity cannot be negative", ErrorCode.INTERNAL_ERROR);
        }
        _credits = credits;
        issueCredits(_credits, _creditMode == CreditMode.AUTO);
    }

    @Override
    public boolean isInbound()
    {
        return true;
    }
}