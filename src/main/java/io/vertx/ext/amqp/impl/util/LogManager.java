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
package io.vertx.ext.amqp.impl.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.amqp.impl.protocol.LinkManager;

public class LogManager
{
    private final Logger _logger;

    private final String _prefix;

    LogManager(String prefix, Class<?> clazz)
    {
        _logger = LoggerFactory.getLogger(LinkManager.class);
        _prefix = prefix;
    }

    public boolean isDebugEnabled()
    {
        return _logger.isDebugEnabled();
    }

    public boolean isInfoEnabled()
    {
        return _logger.isInfoEnabled();
    }

    public static LogManager get(String prefix, Class<?> clazz)
    {
        return new LogManager(prefix, clazz);
    }

    public void debug(String format, Object... args)
    {
        if (_logger.isDebugEnabled())
        {
            _logger.debug(_prefix.concat(String.format(format, args)));
        }
    }

    public void debug(Throwable e, String format, Object... args)
    {
        if (_logger.isDebugEnabled())
        {
            _logger.debug(_prefix.concat(String.format(format, args)), e);
        }
    }

    public void info(String format, Object... args)
    {
        if (_logger.isInfoEnabled())
        {
            _logger.info(_prefix.concat(String.format(format, args)));
        }
    }

    public void info(Throwable e, String format, Object... args)
    {
        if (_logger.isInfoEnabled())
        {
            _logger.info(_prefix.concat(String.format(format, args)), e);
        }
    }

    public void warn(String format, Object... args)
    {
        _logger.fatal(_prefix.concat(String.format(format, args)));
    }

    public void warn(Throwable e, String format, Object... args)
    {
        _logger.warn(_prefix.concat(String.format(format, args)), e);
    }

    public void fatal(String format, Object... args)
    {
        _logger.fatal(_prefix.concat(String.format(format, args)));
    }

    public void fatal(Throwable e, String format, Object... args)
    {
        _logger.fatal(_prefix.concat(String.format(format, args)), e);
    }
}