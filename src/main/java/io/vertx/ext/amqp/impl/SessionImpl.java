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

import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.MessageFormatException;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.ReliabilityMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;

class SessionImpl implements Session
{
    static final int CUMULATIVE = 0x01;

    static final int SETTLE = 0x02;

    private static final DeliveryState ACCEPTED = Accepted.getInstance();

    private static final DeliveryState REJECTED = new Rejected();

    private static final DeliveryState RELEASED = Released.getInstance();

    private ConnectionImpl _conn;

    private org.apache.qpid.proton.engine.Session _ssn;

    private AtomicBoolean _closed = new AtomicBoolean(false);

    private final Map<Link, BaseLink> _links = new HashMap<Link, BaseLink>();

    private final AtomicLong _deliveryTag = new AtomicLong(0);

    private final AtomicLong _incommingSequence = new AtomicLong(0);

    private final Map<Long, Delivery> _unsettled = new ConcurrentHashMap<Long, Delivery>();

    private final AtomicLong _lastSettled = new AtomicLong(0);

    private final AtomicLong _lastDispositionMark = new AtomicLong(0);

    private final String _id;

    SessionImpl(ConnectionImpl conn, org.apache.qpid.proton.engine.Session ssn)
    {
        _id = UUID.randomUUID().toString();
        _conn = conn;
        _ssn = ssn;
    }

    void open()
    {
        _ssn.open();
        _conn.write();
    }

    @Override
    public OutgoingLink createOutboundLink(String address, ReliabilityMode mode) throws MessagingException
    {
        checkClosed();
        Sender sender;
        Source source = new Source();
        Target target = new Target();
        if (address == null || address.isEmpty() || address.equals("#"))
        {
            String temp = UUID.randomUUID().toString();
            sender = _ssn.sender(temp);
            target.setDynamic(true);
        }
        else
        {
            sender = _ssn.sender(address);
            target.setAddress(address);
        }
        sender.setTarget(target);
        sender.setSource(source);
        sender.setSenderSettleMode(mode == ReliabilityMode.UNRELIABLE ? SenderSettleMode.SETTLED
                : SenderSettleMode.UNSETTLED);
        sender.open();

        OutgoingLinkImpl outLink = new OutgoingLinkImpl(this, address, sender);
        outLink.setDynamicAddress(target.getDynamic());
        _links.put(sender, outLink);
        sender.setContext(outLink);
        return outLink;
    }

    @Override
    public IncomingLink createInboundLink(String address, ReliabilityMode mode, CreditMode creditMode)
            throws MessagingException
    {
        Receiver receiver;
        Source source = new Source();
        Target target = new Target();
        if (address == null || address.isEmpty() || address.equals("#"))
        {
            String temp = UUID.randomUUID().toString();
            receiver = _ssn.receiver(temp);
            source.setDynamic(true);
        }
        else
        {
            receiver = _ssn.receiver(address);
            source.setAddress(address);
        }
        receiver.setSource(source);
        receiver.setTarget(target);
        switch (mode)
        {
        case UNRELIABLE:
            receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
            receiver.setSenderSettleMode(SenderSettleMode.SETTLED);
            break;
        case AT_LEAST_ONCE:
            receiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
            receiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
            break;
        // case EXACTLY_ONCE:
        // receiver.setReceiverSettleMode(ReceiverSettleMode.SECOND);
        // receiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
        // break;
        }
        receiver.open();

        IncomingLinkImpl inLink = new IncomingLinkImpl(this, address, receiver, mode, creditMode);
        inLink.setDynamicAddress(source.getDynamic());
        _links.put(receiver, inLink);
        receiver.setContext(inLink);
        return inLink;
    }

    @Override
    public void disposition(AmqpMessage msg, MessageDisposition disposition, int... flags)
            throws MessageFormatException, MessagingException
    {
        disposition(convertMessage(msg).getSequence(), disposition, flags);
    }

    void disposition(long sequence, MessageDisposition disposition, int... flags) throws MessageFormatException,
            MessagingException
    {
        DeliveryState state;
        switch (disposition)
        {
        case ACCEPTED:
            state = ACCEPTED;
            break;
        case REJECTED:
            state = REJECTED;
            break;
        case RELEASED:
            state = RELEASED;
            break;
        default:
            throw new MessagingException("UNKNOWN is not a valid option for this method", ErrorCode.INTERNAL_ERROR);
        }
        disposition(sequence, state, flags);
    }

    @Override
    public void settle(AmqpMessage msg, int... flags) throws MessageFormatException, MessagingException
    {
        settle(convertMessage(msg).getSequence(), flags.length == 0 ? false : (flags[0] & CUMULATIVE) != 0, true);
    }

    @Override
    public void close()
    {
        if (!_closed.get())
        {
            closeImpl();
            // _conn.removeSession(_ssn);
            _conn.write();
        }
    }

    void closeImpl()
    {
        _closed.set(true);
        _ssn.close();
        for (Link link : _links.keySet())
        {
            _links.get(link).closeImpl();
        }
        _links.clear();
    }

    void removeLink(Link link)
    {
        _links.remove(link);
    }

    ConnectionImpl getConnection()
    {
        return _conn;
    }

    long getNextDeliveryTag()
    {
        return _deliveryTag.incrementAndGet();
    }

    long getNextIncommingSequence()
    {
        return _incommingSequence.incrementAndGet();
    }

    String getID()
    {
        return _id;
    }

    void checkClosed() throws MessagingException
    {
        if (_closed.get())
        {
            throw new MessagingException("Session is closed", ErrorCode.SESSION_CLOSED);
        }
    }

    void addUnsettled(long id, Delivery d)
    {
        _unsettled.put(id, d);
    }

    void disposition(long sequence, DeliveryState state, int... flags)
    {
        int flag = flags.length == 1 ? flags[0] : 0;
        boolean cumilative = (flag & CUMULATIVE) != 0;
        boolean settle = (flag & SETTLE) != 0;

        long count = cumilative ? _lastDispositionMark.get() : sequence;
        long end = sequence;

        while (count <= end)
        {
            Delivery d = _unsettled.get(count);
            if (d != null)
            {
                d.disposition(state);
            }
            count++;
        }
        _lastDispositionMark.set(end);
        if (settle)
        {
            settle(sequence, cumilative, false);
        }
        _conn.write();
    }

    void settle(long sequence, boolean cumilative, boolean write)
    {
        long count = cumilative ? _lastSettled.get() : sequence;
        long end = sequence;

        System.out.println("settle : count " + count);
        System.out.println("settle : end " + end);
        System.out.println("settle : _unsettled " + _unsettled);
        while (count <= end)
        {
            Delivery d = _unsettled.get(count);
            if (d != null)
            {
                System.out.println("Delivery " + d);
                if (!d.isSettled() && d.getLink().getReceiverSettleMode() == ReceiverSettleMode.FIRST)
                {
                    d.settle();
                    ((IncomingLinkImpl) d.getLink().getContext()).decrementUnsettledCount();
                    _unsettled.remove(count);
                }
            }
            count++;
        }
        _lastSettled.set(end);
        _conn.write();
    }

    InboundMessage convertMessage(AmqpMessage msg) throws MessageFormatException, MessagingException
    {
        if (!(msg instanceof InboundMessage))
        {
            throw new MessageFormatException("The supplied message is not a recognized type",
                    ErrorCode.INVALID_MSG_FORMAT);
        }

        InboundMessage m = (InboundMessage) msg;

        if (m.getSessionID() != _id)
        {
            throw new MessagingException("The supplied message is not associated with this session",
                    ErrorCode.INVALID_MSG_REF);
        }

        return m;
    }
}