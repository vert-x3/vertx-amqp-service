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
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.DefaultConnectionSettings;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.InboundLink;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.RecoveryOptions;
import io.vertx.ext.amqp.impl.ConnectionImpl.State;
import io.vertx.ext.amqp.impl.AmqpServiceImpl.ReplyHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LinkManager extends AbstractAmqpEventListener
{
    private static final Logger _logger = LoggerFactory.getLogger(LinkManager.class);

    protected final List<ManagedConnection> _outboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final Map<String, Incoming> _inboundLinks = new ConcurrentHashMap<String, Incoming>();

    protected final Map<String, Outgoing> _outboundLinks = new ConcurrentHashMap<String, Outgoing>();

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
                    _logger.warn(String.format("Error {%s} Server was unable to bind to %s:%s", result.cause(), _config.getInboundHost(),
                            _config.getInboundPort()), result.cause());
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

    ConnectionSettings getConnectionSettings(String address) throws MessagingException
    {
        if (URL_CACHE.containsKey(address))
        {
            return URL_CACHE.get(address);
        }
        else
        {
            final ConnectionSettings settings = AddressParser.parse(address);
            URL_CACHE.put(address, settings);
            return settings;
        }
    }

    ManagedConnection getConnection(final ConnectionSettings settings) throws MessagingException
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

    // Validate if the link is connected. If not use RecoveryOptions to
    // determine course of action.
    <T extends BaseLink> T validateLink(T link, RecoveryOptions options) throws MessagingException
    {
        link.checkClosed();
        switch (link.getConnection().getState())
        {
        case CONNECTED:
            return link;
        case NEW:
            throw new MessagingException("Link is not ready yet", ErrorCode.LINK_NOT_READY);
        case RETRY_IN_PROGRESS:
            throw new MessagingException("Link has failed. Retry in progress", ErrorCode.LINK_RETRY_IN_PROGRESS);
        case FAILED:
            if (link.getConnection().isInbound())
            {
                throw new MessagingException("Link created from an AMQP peer into the bridge failed", ErrorCode.LINK_FAILED);
            }
            switch (options.getRetryPolicy())
            {
            case NO_RETRY:
                throw new MessagingException("Link has failed. No retry instructions specified", ErrorCode.LINK_FAILED);
            default:
                // TODO initiate failover
                return null;
            }
        default:
            throw new MessagingException("Invalid link state", ErrorCode.INTERNAL_ERROR);
        }
    }

    // Lookup method for outbound-link. Create if it doesn't exist.
    OutboundLinkImpl getOutboundLink(String amqpAddress) throws MessagingException
    {
        if (_outboundLinks.containsKey(amqpAddress))
        {
            try
            {
                Outgoing outgoing = _outboundLinks.get(amqpAddress);
                return validateLink(outgoing._link, outgoing._options.getRecoveryOptions());
            }
            catch (MessagingException e)
            {
                if (e.getErrorCode() == ErrorCode.LINK_FAILED)
                {
                    _outboundLinks.remove(amqpAddress);
                }
                throw e;
            }
        }
        else
        {
            // TODO link options should be grabbed from the configuration
            return createOutboundLink(UUID.randomUUID().toString(), amqpAddress, new OutgoingLinkOptions());
        }
    }

    // Method for explicitly creating an outbound link
    OutboundLinkImpl createOutboundLink(String id, String amqpAddress, OutgoingLinkOptions options)
            throws MessagingException
    {
        final ConnectionSettings settings = getConnectionSettings(amqpAddress);
        ManagedConnection con = getConnection(settings);
        OutboundLinkImpl link = con.createOutboundLink(settings.getTarget(), options.getReliability());
        _logger.info(String.format("Created outbound link %s @ %s:%s", settings.getTarget(), settings.getHost(),
                settings.getPort()));
        link.setContext(id);
        _outboundLinks.put(amqpAddress, new Outgoing(id, link, options));
        return link;
    }

    // lookup method for inbound-link. Throw an exception if not in the map.
    InboundLinkImpl getInboundLink(String amqpAddress) throws MessagingException
    {
        if (_inboundLinks.containsKey(amqpAddress))
        {
            try
            {
                Incoming incoming = _inboundLinks.get(amqpAddress);
                return validateLink(incoming._link, incoming._options.getRecoveryOptions());
            }
            catch (MessagingException e)
            {
                if (e.getErrorCode() == ErrorCode.LINK_FAILED)
                {
                    _inboundLinks.remove(amqpAddress);
                }
                throw e;
            }
        }
        else
        {
            throw new MessagingException("Incoming link ref doesn't match any AMQP links", ErrorCode.INVALID_LINK_REF);
        }
    }

    // Method for explicitly creating an inbound link
    InboundLinkImpl createInboundLink(String id, String amqpAddress, IncomingLinkOptions options)
            throws MessagingException
    {
        final ConnectionSettings settings = getConnectionSettings(amqpAddress);
        ManagedConnection con = getConnection(settings);
        InboundLinkImpl link = con.createInboundLink(settings.getTarget(), options.getReliability(),
                options.getPrefetch() > 0 ? CreditMode.AUTO : CreditMode.EXPLICT);
        if (options.getPrefetch() > 0)
        {
            link.setCredits(options.getPrefetch());
        }
        link.setContext(id);
        _inboundLinks.put(amqpAddress, new Incoming(id, link, options));
        _logger.info(String.format("Created inbound link %s @ %s:%s", settings.getTarget(), settings.getHost(),
                settings.getPort()));
        return link;
    }

    // ------------ Event Handler ------------------------
    @Override
    public void onOutboundLinkOpen(OutboundLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        if (con.isInbound())
        {
            String name = link.getAddress();
            String amqpAddress = new StringBuilder(con.getSettings().getHost()).append(":")
                    .append(con.getSettings().getPort()).append("/").append(name).toString();
            String id = "inbound-connection:" + UUID.randomUUID().toString();
            link.setContext(id);
            _outboundLinks.put(id, new Outgoing(amqpAddress, link, null));

            _listener.outboundLinkReady(name, amqpAddress, true);
        }
    }

    @Override
    public void onOutboundLinkClosed(OutboundLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        String name = link.getAddress();
        String address = new StringBuilder(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(name).toString();
        _outboundLinks.remove(address);
        if (link.getConnection().isInbound())
        {
            _listener.outboundLinkFinal(name, address, true);
        }
    }

    @Override
    public void onInboundLinkClosed(InboundLinkImpl link)
    {
        // TODO if it's an outbound connection, then we need to notify an error
        if (!link.getConnection().isInbound())
        {
            ConnectionImpl con = link.getConnection();
            String name = link.getAddress();
            String address = new StringBuilder(con.getSettings().getHost()).append(":")
                    .append(con.getSettings().getPort()).append("/").append(name).toString();
            _inboundLinks.remove(address);
            _listener.inboundLinkFinal(name, address, true);
        }
    }

    @Override
    public void onInboundLinkOpen(InboundLinkImpl link)
    {
        try
        {
            link.setContext(UUID.randomUUID().toString());
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

    class Outgoing
    {
        String _id;

        OutboundLinkImpl _link;

        OutgoingLinkOptions _options;

        Outgoing(String id, OutboundLinkImpl link, OutgoingLinkOptions options)
        {
            _id = id;
            _link = link;
            _options = options;
        }
    }

    class Incoming
    {
        String _id;

        InboundLinkImpl _link;

        IncomingLinkOptions _options;

        Incoming(String id, InboundLinkImpl link, IncomingLinkOptions options)
        {
            _id = id;
            _link = link;
            _options = options;
        }
    }
}