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

import static io.vertx.ext.amqp.impl.util.Functions.format;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.MessagingException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ManagedSession extends SessionImpl
{
    private final Map<String, Long> _refToSequenceMap = new ConcurrentHashMap<String, Long>();

    public ManagedSession(ConnectionImpl conn, org.apache.qpid.proton.engine.Session ssn)
    {
        super(conn, ssn);
    }

    void addMsgRef(String ref, long sequence)
    {
        _refToSequenceMap.put(ref, sequence);
    }

    void disposition(String ref, MessageDisposition disposition, int... flags) throws MessagingException,
            MessagingException
    {
        if (_refToSequenceMap.containsKey(ref))
        {
            disposition(_refToSequenceMap.remove(ref), disposition, flags);
        }
        else
        {
            throw new MessagingException(format(
                    "Invalid message reference : %s. Unable to find a matching AMQP message", ref),
                    ErrorCode.INVALID_MSG_REF);
        }
    }
}