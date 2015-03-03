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

public class DefaultAsyncResult<T> implements AsyncResult<T>
{
    private T _result;

    private boolean _success;

    private Throwable _th;

    DefaultAsyncResult(T result)
    {
        this(result, true, null);
    }

    DefaultAsyncResult(Throwable th)
    {
        this(null, false, th);
    }

    DefaultAsyncResult(T result, boolean success, Throwable th)
    {
        _result = result;
        _success = true;
        _th = th;
    }

    @Override
    public Throwable cause()
    {
        return _th;
    }

    @Override
    public boolean failed()
    {
        return !_success;
    }

    @Override
    public T result()
    {
        return _result;
    }

    @Override
    public boolean succeeded()
    {
        return _success;
    }
}