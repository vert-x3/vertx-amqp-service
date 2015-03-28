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


class AmqpEventImpl implements AmqpEvent
{
    private final EventType _type;

    private Connection _con;

    private Session _ssn;

    private Link _link;

    private AmqpMessage _msg;

    private Tracker _tracker;

    AmqpEventImpl(EventType type)
    {
        _type = type;
    }

    void setConnection(Connection con)
    {
        _con = con;
    }

    void setSession(Session ssn)
    {
        _ssn = ssn;
    }

    void setLink(Link link)
    {
        _link = link;
    }

    void setTracker(Tracker tracker)
    {
        _tracker = tracker;
    }

    void setMessage(AmqpMessage msg)
    {
        _msg = msg;
    }

    @Override
    public EventType getType()
    {
        return _type;
    }

    @Override
    public Connection getConnection()
    {
        return _con;
    }

    @Override
    public Session getSession()
    {
        return _ssn;
    }

    @Override
    public Link getLink()
    {
        return _link;
    }

    @Override
    public Tracker getDeliveryTracker()
    {
        return _tracker;
    }

    @Override
    public AmqpMessage getMessage()
    {
        return _msg;
    }
}