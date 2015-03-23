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

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.RecoveryOptions;
import io.vertx.ext.amqp.impl.ConnectionImpl.State;

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

    static final OutgoingLinkOptions DEFAULT_OUTGOING_LINK_OPTIONS = new OutgoingLinkOptions();

    protected final List<ManagedConnection> _outboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final List<ManagedConnection> _inboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final Map<String, Incoming> _inboundLinks = new ConcurrentHashMap<String, Incoming>();

    protected final Map<String, Outgoing> _outboundLinks = new ConcurrentHashMap<String, Outgoing>();

    protected final NetClient _client;

    protected final Vertx _vertx;

    protected final NetServer _server;

    protected final AmqpServiceConfig _config;

    protected final LinkEventListener _listener;

    private Map<String, ConnectionSettings> URL_CACHE;

    @SuppressWarnings("serial")
    public LinkManager(Vertx vertx, AmqpServiceConfig config, AmqpServiceImpl parent)
    {
        _vertx = vertx;
        _config = config;
        _listener = parent;
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
        _server.connectHandler(sock -> {
            DefaultConnectionSettings settings = new DefaultConnectionSettings();
            settings.setHost(sock.remoteAddress().host());
            settings.setPort(sock.remoteAddress().port());
            ManagedConnection connection = new ManagedConnection(settings, this, true);
            connection.setNetSocket(sock);
            connection.open();
            _inboundConnections.add(connection);
            connection.addDisconnectHandler(c -> {
                _inboundConnections.remove(c);
            });
        });

        _server.listen(result -> {
            if (result.failed())
            {
                String error = String.format("Error {%s} Server was unable to bind to %s:%s", result.cause(),
                        _config.getInboundHost(), _config.getInboundPort());
                _logger.warn(error, result.cause());
                // We need to stop the verticle
                _logger.fatal("Initiating the shutdown of AMQP Service due to : " + error);
                parent.stopInternal();
            }
        });
    }

    void stop()
    {
        _logger.warn("Stopping Link Manager : Closing all outgoing and incomming connections");
        for (Connection con : _outboundConnections)
        {
            con.close();
        }

        for (Connection con : _inboundConnections)
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

    // TODO handle reconnection.
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
        _client.connect(settings.getPort(), settings.getHost(), result -> {
            if (result.succeeded())
            {
                connection.setNetSocket(result.result());
                connection.open();
                connection.addDisconnectHandler(c -> {
                    _outboundConnections.remove(c);
                });
                _logger.info(String.format("Connected to AMQP peer at %s:%s", connection.getSettings().getHost(),
                        connection.getSettings().getPort()));
            }
            else
            {
                _logger.warn(String.format("Error {%s}, when connecting to AMQP peer at %s:%s", result.cause(),
                        connection.getSettings().getHost(), connection.getSettings().getPort()));
                connection.setState(State.FAILED);
                _outboundConnections.remove(connection);
            }
        });

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
                throw new MessagingException("Link created from an AMQP peer into the bridge failed",
                        ErrorCode.LINK_FAILED);
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
    OutgoingLinkImpl getOutboundLink(String amqpAddress) throws MessagingException
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
            return createOutboundLink(UUID.randomUUID().toString(), amqpAddress, DEFAULT_OUTGOING_LINK_OPTIONS);
        }
    }

    // Method for explicitly creating an outbound link
    OutgoingLinkImpl createOutboundLink(String id, String amqpAddress, OutgoingLinkOptions options)
            throws MessagingException
    {
        final ConnectionSettings settings = getConnectionSettings(amqpAddress);
        ManagedConnection con = getConnection(settings);
        OutgoingLinkImpl link = con.createOutboundLink(settings.getNode(), options.getReliability());
        link.setContext(id);
        _logger.info(String.format("Created outgoing link to AMQP peer [address=%s @ %s:%s, options=%s] ",
                settings.getNode(), settings.getHost(), settings.getPort(), options));
        _outboundLinks.put(amqpAddress, new Outgoing(id, link, options));
        return link;
    }

    // lookup method for inbound-link. Throw an exception if not in the map.
    IncomingLinkImpl getInboundLink(String amqpAddress) throws MessagingException
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
    IncomingLinkImpl createInboundLink(String id, String amqpAddress, IncomingLinkOptions options)
            throws MessagingException
    {
        final ConnectionSettings settings = getConnectionSettings(amqpAddress);
        ManagedConnection con = getConnection(settings);
        IncomingLinkImpl link = con.createInboundLink(settings.getNode(), options.getReliability(),
                options.getPrefetch() > 0 ? CreditMode.AUTO : CreditMode.EXPLICT);
        if (options.getPrefetch() > 0)
        {
            link.setCredits(options.getPrefetch());
        }
        link.setContext(id);
        _inboundLinks.put(amqpAddress, new Incoming(id, link, options));
        _logger.info(String.format("Created incoming link to AMQP peer [address=%s @ %s:%s, options=%s] ",
                settings.getNode(), settings.getHost(), settings.getPort(), options));
        return link;
    }

    // ------------ Event Handler ------------------------
    @Override
    public void onOutgoingLinkOpen(OutgoingLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        String name = link.getAddress();
        String address = new StringBuilder("amqp://").append(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(name).toString();
        if (con.isInbound())
        {
            String id = "inbound-connection:" + UUID.randomUUID().toString();
            link.setContext(id);
            _outboundLinks.put(address, new Outgoing(id, link, DEFAULT_OUTGOING_LINK_OPTIONS));
            _logger.info(String.format("Accepted an outgoing link (subscription) from AMQP peer %s", address));

        }
        _listener.outgoingLinkReady(_outboundLinks.get(address)._id, name, address, link.getConnection().isInbound());
    }

    @Override
    public void onOutgoingLinkClosed(OutgoingLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        String name = link.getAddress();
        String address = new StringBuilder("amqp://").append(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(name).toString();
        _outboundLinks.remove(address);
        _listener.outgoingLinkFinal(_outboundLinks.get(address)._id, name, address, link.getConnection().isInbound());
    }

    @Override
    public void onIncomingLinkClosed(IncomingLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        String name = link.getAddress();
        String address = new StringBuilder("amqp://").append(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(name).toString();
        // TODO if it's an outbound connection, then we need to notify an error
        if (!link.getConnection().isInbound())
        {
            _inboundLinks.remove(address);
        }
        _listener.incomingLinkFinal(_inboundLinks.get(address)._id, name, address, link.getConnection().isInbound());
    }

    @Override
    public void onIncomingLinkOpen(IncomingLinkImpl link)
    {
        ConnectionImpl con = link.getConnection();
        String name = link.getAddress();
        String address = new StringBuilder("amqp://").append(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(name).toString();
        try
        {
            if (link.getConnection().isInbound())
            {
                String id = "inbound-connection:" + UUID.randomUUID().toString();
                link.setContext(id);
                _inboundLinks.put(address, new Incoming(id, link, new IncomingLinkOptions()));
                link.setCredits(_config.getDefaultLinkCredit());
            }
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
        System.out.println("_inboundLinks " + _inboundLinks);
        System.out.println("lookup addr " + address);
        _listener.incomingLinkReady(_inboundLinks.get(address)._id, name, address, link.getConnection().isInbound());
    }

    @Override
    public void onMessage(IncomingLinkImpl link, InboundMessage msg)
    {
        ConnectionImpl con = link.getConnection();
        String peerAddress = new StringBuilder(con.getSettings().getHost()).append(":")
                .append(con.getSettings().getPort()).toString();
        _listener.message((String)link.getContext(), link.getAddress(), peerAddress, msg);
    }

    // ---------- / Event Handler -----------------------

    // ---------- Helper classes
    class Outgoing
    {
        String _id;

        OutgoingLinkImpl _link;

        OutgoingLinkOptions _options;

        String _toStr;

        Outgoing(String id, OutgoingLinkImpl link, OutgoingLinkOptions options)
        {
            _id = id;
            _link = link;
            _options = options;
            _toStr = String.format("[id=%s, link=%s, options=%s]", _id, _link.getAddress(), _options);
        }

        @Override
        public String toString()
        {
            return _toStr;
        }
    }

    class Incoming
    {
        String _id;

        IncomingLinkImpl _link;

        IncomingLinkOptions _options;

        String _toStr;

        Incoming(String id, IncomingLinkImpl link, IncomingLinkOptions options)
        {
            _id = id;
            _link = link;
            _options = options;
            _toStr = String.format("[id=%s, link=%s, options=%s]", _id, _link.getAddress(), _options);
        }

        @Override
        public String toString()
        {
            return _toStr;
        }
    }
}