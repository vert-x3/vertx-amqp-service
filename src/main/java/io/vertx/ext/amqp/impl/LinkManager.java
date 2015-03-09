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
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.amqp.AmqpServiceConfig;
import io.vertx.ext.amqp.Connection;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.DefaultConnectionSettings;
import io.vertx.ext.amqp.InboundLink;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.impl.ConnectionImpl.State;
import io.vertx.ext.amqp.impl.AmqpServiceImpl.ReplyHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LinkManager extends AbstractAmqpEventListener
{
    private static final Logger _logger = LoggerFactory.getLogger(LinkManager.class);

    protected final List<ManagedConnection> _outboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final Map<String, InboundLinkImpl> _inboundLinks = new ConcurrentHashMap<String, InboundLinkImpl>();

    protected final Map<String, OutboundLinkImpl> _outboundLinks = new ConcurrentHashMap<String, OutboundLinkImpl>();

    protected final NetClient _client;

    protected final Vertx _vertx;

    protected final NetServer _server;

    protected final AmqpServiceConfig _config;

    protected final LinkEventListener _listener;

    private Map<String, ConnectionSettings> URL_CACHE;

    @SuppressWarnings("serial")
    public LinkManager(Vertx vertx, AmqpServiceConfig config, LinkEventListener listener)
    {
        _vertx = vertx;
        _config = config;
        _listener = listener;
        _client = _vertx.createNetClient(new NetClientOptions());

        URL_CACHE = Collections.synchronizedMap(new LinkedHashMap<String, ConnectionSettings>(config
                .getMaxedCachedURLEntries() + 1, 1.1f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ConnectionSettings> eldest)
            {
                return size() > config.getMaxedCachedURLEntries();
            }
        });

        NetServerOptions serverOp = new NetServerOptions();
        serverOp.setHost(config.getInboundHost());
        serverOp.setPort(config.getInboundPort());
        _server = _vertx.createNetServer(serverOp);
        _server.connectHandler(new InboundConnectionHandler(this));
        _server.listen(new AsyncResultHandler<NetServer>()
        {
            public void handle(AsyncResult<NetServer> result)
            {
                if (result.failed())
                {
                    _logger.warn(String.format("Server was unable to bind to %s:%s", _config.getInboundHost(),
                            _config.getInboundPort()));
                    // We need to stop the verticle
                }
            }
        });
    }

    void stop()
    {
        for (Connection con : _outboundConnections)
        {
            con.close();
        }

        _server.close();
    }

    ConnectionSettings getConnectionSettings(String url)
    {
        if (URL_CACHE.containsKey(url))
        {
            return URL_CACHE.get(url);
        }
        else
        {
            final ConnectionSettings settings = URLParser.parse(url);
            URL_CACHE.put(url, settings);
            return settings;
        }
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
                    _logger.info(String.format("Attempting re-connection to AMQP peer at %s:%s", settings.getHost(),
                            settings.getPort()));
                    break;
                }
            }
        }

        ManagedConnection connection = new ManagedConnection(settings, this, false);
        ConnectionResultHander handler = new ConnectionResultHander(connection);
        _client.connect(settings.getPort(), settings.getHost(), handler);
        _logger.info(String.format("Attempting connection to AMQP peer at %s:%s", settings.getHost(),
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

        final ConnectionSettings settings = getConnectionSettings(url);
        ManagedConnection con = findConnection(settings);
        OutboundLinkImpl link = con.createOutboundLink(settings.getTarget());
        _logger.info(String.format("Created outbound link %s @ %s:%s", settings.getTarget(), settings.getHost(),
                settings.getPort()));
        return link;
    }

    List<OutboundLinkImpl> getOutboundLinks(List<String> amqpAddressList) throws MessagingException
    {
        List<OutboundLinkImpl> links = new ArrayList<OutboundLinkImpl>();
        for (String addr : amqpAddressList)
        {
            links.add(findOutboundLink(addr));
        }
        return links;
    }

    // ------------ Event Handler ------------------------
    @Override
    public void onOutboundLinkOpen(OutboundLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        if (con.isInbound())
        {
            String name = link.getAddress();
            String url = new StringBuilder(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                    .append("/").append(name).toString();
            _outboundLinks.put(url, link);

            _listener.outboundLinkReady(name, url, true);
        }
    }

    @Override
    public void onOutboundLinkClosed(OutboundLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        String name = link.getAddress();
        String url = new StringBuilder(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(name).toString();
        _outboundLinks.remove(url);
        if (link.getConnection().isInbound())
        {
            _listener.outboundLinkFinal(name, url, true);
        }
    }

    @Override
    public void onInboundLinkOpen(InboundLink link)
    {
        try
        {
            link.setCredits(_config.getDefaultLinkCredit());
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(InboundLinkImpl link, InboundMessage msg)
    {
        _listener.message(link.getAddress(), msg);
    }

    // ---------- / Event Handler -----------------------

    // ---------- Helper classes
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
                _logger.info(String.format("Connected to AMQP peer at %s:%s", _connection.getSettings().getHost(),
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

    class InboundConnectionHandler implements Handler<NetSocket>
    {
        AmqpEventListener _handler;

        InboundConnectionHandler(AmqpEventListener handler)
        {
            _handler = handler;
        }

        public void handle(NetSocket sock)
        {
            DefaultConnectionSettings settings = new DefaultConnectionSettings();
            settings.setHost(sock.remoteAddress().host());
            settings.setPort(sock.remoteAddress().port());
            ManagedConnection connection = new ManagedConnection(settings, _handler, true);
            connection.setNetSocket(sock);
            connection.open();
        }
    }
}