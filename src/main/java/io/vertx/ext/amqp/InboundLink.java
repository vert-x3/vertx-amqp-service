/*
 *
:w * Licensed to the Apache Software Foundation (ASF) under one
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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;

class InboundLink extends BaseLink
{
    private static int DEFAULT_CREDITS = 1;

    private CreditMode _creditMode;

    private int _credits = 0;

    private AtomicInteger _unsettled = new AtomicInteger(0);

    InboundLink(Session ssn, String address, Link link, CreditMode creditMode)
    {
        super(ssn, address, link);
        _creditMode = creditMode;
    }

    void init()
    {
        _link.open();
        if (_creditMode == CreditMode.AUTO && DEFAULT_CREDITS > 0)
        {
            _credits = DEFAULT_CREDITS;
            ((Receiver) _link).flow(_credits);
        }
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

    CreditMode getCreditMode()
    {
        return _creditMode;
    }

    int getUnsettled() throws MessagingException
    {
        checkClosed();
        return _unsettled.get();
    }

    void setCredits(int credits) throws MessagingException
    {
        checkClosed();
        if (credits < 0)
        {
            throw new MessagingException("Capacity cannot be negative");
        }
        _credits = credits;
        issueCredits(_credits, _creditMode == CreditMode.AUTO);
    }
}