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

import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.MessagingException;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.proton.engine.Link;

abstract class BaseLink
{
    private final String _toString;

    String _address;

    Link _link;

    SessionImpl _ssn;

    AtomicBoolean _closed = new AtomicBoolean(false);

    boolean _dynamic = false;

    private Object _ctx;

    BaseLink(SessionImpl ssn, String address, Link link)
    {
        _address = address;
        _link = link;
        _ssn = ssn;
        _toString = ssn.getConnection().toString() + "/" + address;
    }

    public String getName()
    {
        return _link.getName();
    }

    public String getAddress()
    {
        return _address;
    }

    String getTarget()
    {
        if (_link.getRemoteTarget() != null)
        {
            return _link.getRemoteTarget().getAddress();
        }
        else if (_link.getTarget() != null)
        {
            return _link.getTarget().getAddress();
        }
        else
        {
            return null;
        }
    }

    String getSource()
    {
        if (_link.getRemoteSource() != null)
        {
            return _link.getRemoteSource().getAddress();
        }
        else if (_link.getSource() != null)
        {
            return _link.getSource().getAddress();
        }
        else
        {
            return null;
        }
    }

    void checkClosed() throws MessagingException
    {
        if (_closed.get())
        {
            throw new MessagingException("Link is closed", ErrorCode.LINK_CLOSED);
        }
    }

    public void close()
    {
        closeImpl();
        _ssn.removeLink(_link);
        _ssn.getConnection().write();
    }

    void closeImpl()
    {
        _closed.set(true);
        _link.close();
    }

    void setDynamicAddress(boolean dynamic)
    {
        _dynamic = dynamic;
    }

    SessionImpl getSession()
    {
        return _ssn;
    }

    ConnectionImpl getConnection()
    {
        return _ssn.getConnection();
    }

    @Override
    public String toString()
    {
        return _toString;
    }

    Object getContext()
    {
        return _ctx;
    }

    void setContext(Object ctx)
    {
        _ctx = ctx;
    }

    Link getProtocolLink()
    {
        return _link;
    }
}