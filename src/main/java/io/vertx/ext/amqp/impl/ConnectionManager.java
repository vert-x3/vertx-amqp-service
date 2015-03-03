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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.impl.ConnectionImpl.State;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionManager extends AbstractAmqpEventListener
{
    private static final Logger _connectionLogger = LoggerFactory.getLogger(ConnectionManager.class);

    protected final List<ManagedConnection> _outboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final Map<String, InboundLinkImpl> _inboundLinks = new ConcurrentHashMap<String, InboundLinkImpl>();

    protected final Map<String, OutboundLinkImpl> _outboundLinks = new ConcurrentHashMap<String, OutboundLinkImpl>();

    protected final NetClient _client;

    protected final Vertx _vertx;

    protected final MessageFactory _msgFactory;
    
    public ConnectionManager(Vertx vertx)
    {
        _vertx = vertx;
        _client = _vertx.createNetClient(new NetClientOptions());
        _msgFactory = new MessageFactory();
    }
    
    ManagedConnection findConnection(final ConnectionSettings settings) throws MessagingException
    {
        for (ManagedConnection con : _outboundConnections)
        {
            if (con.getSettings().getHost().equals(settings.getHost())
                    && con.getSettings().getPort() == settings.getPort())
            {
                if (con.getState() == State.CONNECTED)
                {
                    return con;
                }
                else
                {
                    _connectionLogger.info(String.format("Attempting re-connection to AMQP peer at %s:%s", settings.getHost(),
                            settings.getPort()));
                    break;
                }
            }
        }

        ManagedConnection connection = new ManagedConnection(settings, this, false);
        ConnectionResultHander handler = new ConnectionResultHander(connection);
        _client.connect(settings.getPort(), settings.getHost(), handler);
        _connectionLogger.info(String.format("Attempting connection to AMQP peer at %s:%s", settings.getHost(),
                settings.getPort()));
        _outboundConnections.add(connection);
        return connection;
    }

    OutboundLinkImpl findOutboundLink(String url) throws MessagingException
    {
        if (_outboundLinks.containsKey(url))
        {
            OutboundLinkImpl link = _outboundLinks.get(url);
            if (link.getConnection().getState() != State.FAILED)
            {
                return link;
            }
            else
            {
                // remove dead link
                _outboundLinks.remove(url);
            }
        }

        final ConnectionSettings settings = URLParser.parse(url);
        ManagedConnection con = findConnection(settings);
        OutboundLinkImpl link = con.createOutboundLink(settings.getTarget());
        _connectionLogger.info(String.format("Created outbound link %s @ %s:%s", settings.getTarget(), settings.getHost(),
                settings.getPort()));
        return link;
    }
    
    class ConnectionResultHander implements Handler<AsyncResult<NetSocket>>
    {
        ConnectionImpl _connection;

        Throwable _cause;

        ConnectionResultHander(ConnectionImpl conn)
        {
            _connection = conn;
        }

        @Override
        public void handle(AsyncResult<NetSocket> result)
        {
            if (result.succeeded())
            {
                _connection.setNetSocket(result.result());
                _connection.open();
                _connectionLogger.info(String.format("Connected to AMQP peer at %s:%s", _connection.getSettings().getHost(),
                        _connection.getSettings().getPort()));
            }
            else
            {
                _cause = result.cause();
                /*
                 * _logger.info("-------- Connection failure ----------");
                 * _logger.info(String.format(
                 * "Failed to establish a connection to AMQP peer at %s:%s",
                 * _connection .getSettings().getHost(),
                 * _connection.getSettings().getPort()));
                 * _logger.info("Exception received", _cause);
                 * _logger.info("-------- /Connection failure ----------");
                 */
                _outboundConnections.remove(_connection);
            }
        }
    }
}