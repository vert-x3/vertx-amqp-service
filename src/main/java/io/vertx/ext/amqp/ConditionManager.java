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

public class ConditionManager
{
    private final Object _lock = new Object();

    private boolean _value;

    private boolean _continue;

    public ConditionManager(boolean initialValue)
    {
        _value = initialValue;
    }

    public void setValueAndNotify(boolean value)
    {
        synchronized (_lock)
        {
            if (_value != value)
            {
                _value = value;
                _lock.notifyAll();
            }
        }
    }

    public void wakeUpAndReturn()
    {
        synchronized (_lock)
        {
            _continue = false;
            _lock.notifyAll();
        }
    }

    public void waitUntilFalse()
    {
        waitImpl(Boolean.FALSE, -1);
    }

    public long waitUntilFalse(long timeout) throws ConditionManagerTimeoutException
    {
        long remaining = waitImpl(Boolean.FALSE, timeout);
        if (_value)
        {
            throw new ConditionManagerTimeoutException("Timed out waiting for condition to become false");
        }
        else
        {
            return remaining;
        }
    }

    public void waitUntilTrue()
    {
        waitImpl(Boolean.TRUE, -1);
    }

    public long waitUntilTrue(long timeout) throws ConditionManagerTimeoutException
    {
        long remaining = waitImpl(Boolean.TRUE, timeout);
        if (!_value)
        {
            throw new ConditionManagerTimeoutException("Timed out waiting for condition to become true");
        }
        else
        {
            return remaining;
        }
    }

    long waitImpl(boolean expected, long timeout)
    {
        if (_value == expected)
        {
            return timeout;
        }

        synchronized (_lock)
        {
            long start = 0;
            long elapsed = 0;
            _continue = true;
            while ((_value != expected) && _continue)
            {
                if (timeout > 0)
                {
                    start = System.currentTimeMillis();
                }
                try
                {
                    if (timeout < 0)
                    {
                        preWaiting();
                        _lock.wait();
                    }
                    else
                    {
                        preWaiting();
                        _lock.wait(timeout - elapsed);
                    }
                }
                catch (InterruptedException e)
                {
                }
                finally
                {
                    postWaiting();
                }

                if (timeout > 0)
                {
                    elapsed = System.currentTimeMillis() - start;
                    if (timeout - elapsed <= 0)
                    {
                        break;
                    }
                }
            }
            return timeout - elapsed;
        }
    }

    public boolean getCurrentValue()
    {
        synchronized (_lock)
        {
            return _value;
        }
    }

    public void preWaiting()
    {
    }

    public void postWaiting()
    {
    }

    @Override
    public String toString()
    {
        return "ConditionManager value : " + _value + ", continue : " + _continue;
    }
}
