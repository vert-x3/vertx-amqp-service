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

import static io.vertx.ext.amqp.impl.protocol.SessionImpl.SETTLE;
import static io.vertx.ext.amqp.impl.util.Functions.format;
import static io.vertx.ext.amqp.impl.util.Functions.print;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.ConnectionSettings;
import io.vertx.ext.amqp.CreditMode;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.IncomingLinkOptions;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.OutgoingLinkOptions;
import io.vertx.ext.amqp.ReliabilityMode;
import io.vertx.ext.amqp.RetryOptions;
import io.vertx.ext.amqp.impl.AddressParser;
import io.vertx.ext.amqp.impl.AmqpServiceConfig;
import io.vertx.ext.amqp.impl.AmqpServiceImpl;
import io.vertx.ext.amqp.impl.protocol.ConnectionImpl.State;
import io.vertx.ext.amqp.impl.util.LogManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.qpid.proton.message.Message;

/*
 * The goal is to completely isolate AMQP Link management away from the Service impl.
 * LinkManager should hide all Links and only allow the service to interact via the link id (link-name in AMQP) and the endpoint address.
 */
public class LinkManager extends AbstractAmqpEventListener
{
    private static final LogManager LOG = LogManager.get("LINK_MGT:", LinkManager.class);

    private static final OutgoingLinkOptions DEFAULT_OUTGOING_LINK_OPTIONS = new OutgoingLinkOptions();

    protected final List<ManagedConnection> _outboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final List<ManagedConnection> _inboundConnections = new CopyOnWriteArrayList<ManagedConnection>();

    protected final Map<String, Incoming> _incomingLinks = new ConcurrentHashMap<String, Incoming>();

    protected final Map<String, Outgoing> _outgoingLinks = new ConcurrentHashMap<String, Outgoing>();

    protected final Map<String, String> _sharedIncomingLinks = new ConcurrentHashMap<String, String>();

    protected final Map<String, String> _sharedOutgoingLinks = new ConcurrentHashMap<String, String>();

    protected final Map<String, ManagedSession> _msgRefToSsnMap = new ConcurrentHashMap<String, ManagedSession>();

    protected final NetClient _client;

    protected final Vertx _vertx;

    protected final NetServer _server;

    protected final AmqpServiceConfig _config;

    protected final LinkEventListener _listener;

    private Map<String, ConnectionSettings> URL_CACHE;

    @SuppressWarnings("serial")
    public LinkManager(Vertx vertx, AmqpServiceConfig config, AmqpServiceImpl parent)
    {
        DEFAULT_OUTGOING_LINK_OPTIONS.setReliability(ReliabilityMode.AT_LEAST_ONCE);
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
            connection.write();
            _inboundConnections.add(connection);
            connection.addDisconnectHandler(c -> {
                _inboundConnections.remove(c);
            });
        });

        _server.listen(result -> {
            if (result.failed())
            {
                String error = format("Error {%s} Server was unable to bind to %s:%s", result.cause(),
                        _config.getInboundHost(), _config.getInboundPort());
                LOG.fatal(error, result.cause());
                // We need to stop the verticle
                LOG.fatal("Initiating the shutdown of AMQP Service due to : %s", error);
                parent.stopInternal();
            }
        });
    }

    public void stop()
    {
        LOG.fatal("Stopping Link Manager : Closing all outgoing and incomming connections");
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

    public ConnectionSettings getConnectionSettings(String address) throws MessagingException
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
    public ManagedConnection getConnection(final ConnectionSettings settings) throws MessagingException
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
                    LOG.info("Attempting re-connection to AMQP peer at %s:%s", settings.getHost(), settings.getPort());
                    break;
                }
            }
        }

        ManagedConnection connection = new ManagedConnection(settings, this, false);
        _client.connect(settings.getPort(), settings.getHost(), result -> {
            if (result.succeeded())
            {
                connection.setNetSocket(result.result());
                connection.write();
                connection.addDisconnectHandler(c -> {
                    _outboundConnections.remove(c);
                });
                LOG.info("Connected to AMQP peer at %s:%s", connection.getSettings().getHost(), connection
                        .getSettings().getPort());
            }
            else
            {
                LOG.warn("Error {%s}, when connecting to AMQP peer at %s:%s", result.cause(), connection.getSettings()
                        .getHost(), connection.getSettings().getPort());
                connection.setState(State.FAILED);
                _outboundConnections.remove(connection);
            }
        });

        LOG.info("Attempting connection to AMQP peer at %s:%s", settings.getHost(), settings.getPort());
        _outboundConnections.add(connection);
        return connection;
    }

    // Validate if the link is connected. If not use RecoveryOptions to
    // determine course of action.
    private <T extends BaseLink> T validateLink(T link, RetryOptions options) throws MessagingException
    {
        link.checkClosed();
        switch (link.getConnection().getState())
        {
        case CONNECTED:
            return link;
        case NEW:
            // throw new MessagingException("Link is not ready yet",
            // ErrorCode.LINK_NOT_READY);
            return link;
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

    // =====================================================
    // OutgoingLink
    // =====================================================

    public OutgoingLinkImpl getSharedOutgoingLink(String amqpAddress) throws MessagingException
    {
        if (_sharedOutgoingLinks.containsKey(amqpAddress))
        {
            String id = _sharedOutgoingLinks.get(amqpAddress);
            if (_outgoingLinks.containsKey(id))
            {
                try
                {
                    Outgoing outgoing = _outgoingLinks.get(id);
                    validateLink(outgoing._link, outgoing._options.getRecoveryOptions());
                    return outgoing._link;
                }
                catch (MessagingException e)
                {
                    throw e;
                }
            }
        }
        // Either it doesn't exist, or the link was canned.
        OutgoingLinkImpl link = createOutgoingLinkInternal(amqpAddress, DEFAULT_OUTGOING_LINK_OPTIONS);
        _sharedOutgoingLinks.put(amqpAddress, link.getName());
        return link;
    }

    // Method for explicitly creating an outbound link
    public String createOutgoingLink(String amqpAddress, OutgoingLinkOptions options) throws MessagingException
    {
        return createOutgoingLinkInternal(amqpAddress, options).getName();
    }

    // Internal use
    private OutgoingLinkImpl createOutgoingLinkInternal(String amqpAddress, OutgoingLinkOptions options)
            throws MessagingException
    {
        final ConnectionSettings settings = getConnectionSettings(amqpAddress);
        ManagedConnection con = getConnection(settings);
        OutgoingLinkImpl link = con.createOutboundLink(settings.getNode(), options.getReliability());
        LOG.info("Created outgoing link to AMQP peer [address=%s @ %s:%s, options=%s] ", settings.getNode(),
                settings.getHost(), settings.getPort(), options);
        _outgoingLinks.put(link.getName(), new Outgoing(link, options));
        return link;
    }

    public void sendViaAddress(String amqpAddress, Message outMsg, JsonObject inMsg) throws MessagingException
    {
        send(getSharedOutgoingLink(amqpAddress), DEFAULT_OUTGOING_LINK_OPTIONS, outMsg, inMsg);
    }

    public void sendViaLink(String linkId, Message outMsg, JsonObject inMsg) throws MessagingException
    {
        Outgoing outgoing = _outgoingLinks.get(linkId);
        send(outgoing._link, outgoing._options, outMsg, inMsg);
    }

    private void send(OutgoingLinkImpl link, OutgoingLinkOptions options, Message outMsg, JsonObject inMsg)
            throws MessagingException
    {
        if (options.getReliability() == ReliabilityMode.AT_LEAST_ONCE
                && inMsg.containsKey(AmqpService.OUTGOING_MSG_REF))
        {
            TrackerImpl tracker = link.send(outMsg);
            tracker.setContext(inMsg.getString(AmqpService.OUTGOING_MSG_REF));
        }
        else
        {
            link.send(outMsg);
        }
    }

    public void closeOutgoingLink(String linkId) throws MessagingException
    {
        if (_outgoingLinks.containsKey(linkId))
        {
            _outgoingLinks.remove(linkId)._link.close();
        }
        // else don't bother. Link already canned
    }

    // =====================================================
    // Incoming Link
    // =====================================================
    public void setCredits(String linkId, int credits) throws MessagingException
    {
        if (_incomingLinks.containsKey(linkId))
        {
            try
            {
                Incoming incoming = _incomingLinks.get(linkId);
                validateLink(incoming._link, incoming._options.getRecoveryOptions());
                incoming._link.setCredits(credits);
            }
            catch (MessagingException e)
            {
                throw e;
            }
        }
        else
        {
            throw new MessagingException("Incoming link ref doesn't match any AMQP links", ErrorCode.INVALID_LINK_REF);
        }
    }

    public void settleDelivery(String msgRef, MessageDisposition disposition) throws MessagingException
    {
        if (_msgRefToSsnMap.containsKey(msgRef))
        {
            ManagedSession ssn = _msgRefToSsnMap.remove(msgRef);
            ssn.checkClosed();
            ssn.disposition(msgRef, disposition, SETTLE);
        }
        else
        {
            throw new MessagingException(format(
                    "Invalid message reference : %s. Unable to find a matching AMQP message", msgRef),
                    ErrorCode.INVALID_MSG_REF);
        }
    }

    // Method for explicitly creating an inbound link
    public String createIncomingLink(String amqpAddress, IncomingLinkOptions options) throws MessagingException
    {
        final ConnectionSettings settings = getConnectionSettings(amqpAddress);
        ManagedConnection con = getConnection(settings);
        IncomingLinkImpl link = con.createInboundLink(settings.getNode(), options.getReliability(),
                options.getPrefetch() > 0 ? CreditMode.AUTO : CreditMode.EXPLICT);
        if (options.getPrefetch() > 0)
        {
            link.setCredits(options.getPrefetch());
        }
        _incomingLinks.put(link.getName(), new Incoming(link, options));
        LOG.info("Created incoming link to AMQP peer [address=%s @ %s:%s, options=%s] ", settings.getNode(),
                settings.getHost(), settings.getPort(), options);
        return link.getName();
    }

    public void closeIncomingLink(String linkId) throws MessagingException
    {
        if (_incomingLinks.containsKey(linkId))
        {
            _incomingLinks.remove(linkId)._link.close();
        }
        // else don't bother. Link already canned
    }

    // ------------ Event Handler ------------------------
    @Override
    public void onOutgoingLinkOpen(OutgoingLinkImpl link)
    {
        boolean inbound = link.getConnection().isInbound();
        String id = link.getName();
        String address = link.getSource();
        if (id == null || id.trim().isEmpty())
        {
            id = address;
        }
        if (inbound)
        {
            _outgoingLinks.put(id, new Outgoing(link, DEFAULT_OUTGOING_LINK_OPTIONS));
            LOG.info("Accepted an outgoing link (subscription) from AMQP peer %s", address);
        }
        _listener.outgoingLinkReady(id, address, inbound);
    }

    @Override
    public void onOutgoingLinkClosed(OutgoingLinkImpl link)
    {
        boolean inbound = link.getConnection().isInbound();
        String id = link.getName();
        String address = link.getSource();
        if (id == null || id.trim().isEmpty())
        {
            id = address;
        }
        _outgoingLinks.remove(id);
        _listener.outgoingLinkFinal(id, address, inbound);
    }

    @Override
    public void onIncomingLinkClosed(IncomingLinkImpl link)
    {
        boolean inbound = link.getConnection().isInbound();
        String id = link.getName();
        String address = link.getTarget();
        if (id == null || id.trim().isEmpty())
        {
            id = address;
        }
        // TODO if it's an outbound connection, then we need to notify an error
        if (!inbound)
        {
            _incomingLinks.remove(id);
        }
        _listener.incomingLinkFinal(id, address, inbound);
    }

    @Override
    public void onIncomingLinkOpen(IncomingLinkImpl link)
    {
        boolean inbound = link.getConnection().isInbound();
        String id = link.getName();
        String address = link.getTarget();
        print("Local Source %s ", link.getProtocolLink().getSource() == null ? "null" : link.getProtocolLink().getSource().getAddress());
        print("Remote Source %s ", link.getProtocolLink().getRemoteSource() == null ? "null" : link.getProtocolLink().getRemoteSource().getAddress());
        print("Local Target %s ", link.getProtocolLink().getTarget() == null ? "null" : link.getProtocolLink().getTarget().getAddress());
        print("Remote Target %s ", link.getProtocolLink().getRemoteTarget() == null ? "null" : link.getProtocolLink().getRemoteTarget().getAddress());
        if (id == null || id.trim().isEmpty())
        {
            id = address;
        }
        if (inbound)
        {
            _incomingLinks.put(id, new Incoming(link, new IncomingLinkOptions()));
            /*
             * try { link.setCredits(_config.getDefaultLinkCredit()); } catch
             * (MessagingException e) { _logger.warn( format(
             * "Error setting link credit for incoming link (source=%s) created via an inbound connection"
             * , address), e); }
             */

        }
        LOG.debug("incomingLinkReady inbound=%s, id=%s , address=%s", inbound, id, address);
        _listener.incomingLinkReady(id, address, inbound);
    }

    @Override
    public void onMessage(IncomingLinkImpl link, InboundMessage msg)
    {
        ManagedSession ssn = (ManagedSession) link.getSession();    
        ssn.addMsgRef(msg.getMsgRef(), msg.getSequence());
        _msgRefToSsnMap.put(msg.getMsgRef(), ssn);
        _listener.message(link.getName(), link.getAddress(), link.getReceiverMode(), msg);
    }

    @Override
    public void onOutgoingLinkCredit(OutgoingLinkImpl link, int credits)
    {
        _listener.outgoingLinkCreditGiven(link.getName(), credits);
    }

    @Override
    public void onSettled(OutgoingLinkImpl link, TrackerImpl tracker)
    {
        _listener.deliveryUpdate(link.getName(), (String) tracker.getContext(), tracker.getState(),
                tracker.getDisposition());
    }

    // ---------- / Event Handler -----------------------

    // ---------- Helper classes
    class Outgoing
    {
        OutgoingLinkImpl _link;

        OutgoingLinkOptions _options;

        String _toStr;

        Outgoing(OutgoingLinkImpl link, OutgoingLinkOptions options)
        {
            _link = link;
            _options = options;
            _toStr = format("[link=%s, options=%s]", _link.getAddress(), _options);
        }

        @Override
        public String toString()
        {
            return _toStr;
        }
    }

    class Incoming
    {
        IncomingLinkImpl _link;

        IncomingLinkOptions _options;

        String _toStr;

        Incoming(IncomingLinkImpl link, IncomingLinkOptions options)
        {
            _link = link;
            _options = options;
            _toStr = format("[link=%s, options=%s]", _link.getAddress(), _options);
        }

        @Override
        public String toString()
        {
            return _toStr;
        }
    }
}