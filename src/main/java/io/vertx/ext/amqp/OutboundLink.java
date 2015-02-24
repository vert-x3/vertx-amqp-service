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

import java.nio.ByteBuffer;

import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sender;

class OutboundLink extends BaseLink
{

    OutboundLink(Session ssn, String address, Link link)
    {
        super(ssn, address, link);
    }

    void offerCredits(int credits) throws MessagingException
    {
        ((Sender) _link).offer(credits);
        _ssn.getConnection().write();
    }

    void init()
    {
        _link.open();
    }

    int getUnsettled() throws MessagingException
    {
        checkClosed();
        return _link.getUnsettled();
    }

    Tracker send(AmqpMessage msg) throws MessageFormatException, MessagingException
    {
        checkClosed();
        if (msg instanceof AmqpMessage)
        {
            return send(msg.getProtocolMessage());
        }
        else
        {
            throw new MessageFormatException("Unsupported message implementation");
        }
    }

    Tracker send(org.apache.qpid.proton.message.Message m) throws MessageFormatException, MessagingException
    {
        checkClosed();
        Sender sender = (Sender) _link;
        byte[] tag = longToBytes(_ssn.getNextDeliveryTag());
        Delivery delivery = sender.delivery(tag);
        Tracker tracker = new Tracker(_ssn);
        delivery.setContext(tracker);
        if (sender.getSenderSettleMode() == SenderSettleMode.SETTLED)
        {
            delivery.settle();
            tracker.markSettled();
        }

        if (m.getAddress() == null)
        {
            m.setAddress(_address);
        }
        byte[] buffer = new byte[1024];
        int encoded = m.encode(buffer, 0, buffer.length);
        sender.send(buffer, 0, encoded);
        sender.advance();
        _ssn.getConnection().write();
        return tracker;
    }

    private static byte[] longToBytes(final long value)
    {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        return buffer.array();
    }
}