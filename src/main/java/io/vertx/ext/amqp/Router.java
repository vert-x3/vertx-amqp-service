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
package io.vertx.ext.amqp;

import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.amqp.Connection.State;
import io.vertx.ext.amqp.RouterConfig.RouteEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Router implements EventHandler, Handler<Message<JsonObject>>
{
    private static final Logger _logger = LoggerFactory.getLogger(Router.class);

    private final Vertx _vertx;

    private final EventBus _eb;

    private final MessageFactory _msgFactory;

    private OutboundLink _defaultOutboundLink;

    private final List<Connection> _outboundConnections = new CopyOnWriteArrayList<Connection>();

    private final List<InboundLink> _inboundLinks = new CopyOnWriteArrayList<InboundLink>();

    private final Map<String, OutboundLink> _outboundLinks = new ConcurrentHashMap<String, OutboundLink>();

    private final Map<String, Message<JsonObject>> _vertxReplyTo = new ConcurrentHashMap<String, Message<JsonObject>>();

    private final RouterConfig _config;

    private final NetClient _client;

    private final NetServer _server;

    private final String _replyToAddressPrefix;

    private final MessageConsumer<JsonObject> _defaultConsumer;

    private final List<MessageConsumer<JsonObject>> _consumers = new ArrayList<MessageConsumer<JsonObject>>();

    Router(Vertx vertx, MessageFactory msgFactory, RouterConfig config) throws MessagingException
    {
        System.out.println("vertx.home " + System.getProperty("vertx.home"));

        _vertx = vertx;
        _eb = _vertx.eventBus();
        _client = _vertx.createNetClient(new NetClientOptions());
        _msgFactory = msgFactory;
        _config = config;
        _replyToAddressPrefix = "amqp://" + _config.getInboundHost() + ":" + _config.getInboundPort();

        _defaultConsumer = _eb.consumer(config.getDefaultHandlerAddress(), this);
        for (String handlerAddress : config.getHandlerAddressList())
        {
            _consumers.add(_eb.consumer(handlerAddress, this));
        }

        _defaultOutboundLink = findOutboundLink(config.getDefaultOutboundAddress());

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
        if (_logger.isInfoEnabled())
        {
            StringBuilder b = new StringBuilder();
            b.append("Configuring module \n[\n");
            b.append("Default vertx handler address : ").append(config.getDefaultHandlerAddress()).append("\n");
            b.append("Default vertx address : ").append(config.getDefaultInboundAddress()).append("\n");
            b.append("Default outbound address : ").append(config.getDefaultOutboundAddress()).append("\n");
            b.append("Handler address list : ").append(config.getHandlerAddressList()).append("\n");
            b.append("]\n");
            _logger.info(b.toString());
        }
    }

    public void stop()
    {
        for (Connection con : _outboundConnections)
        {
            con.close();
        }

        _server.close();
    }

    Connection findConnection(final ConnectionSettings settings) throws MessagingException
    {
        for (Connection con : _outboundConnections)
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

        Connection connection = new Connection(settings, this, false);
        ConnectionResultHander handler = new ConnectionResultHander(connection);
        _client.connect(settings.getPort(), settings.getHost(), handler);
        _logger.info(String.format("Attempting connection to AMQP peer at %s:%s", settings.getHost(),
                settings.getPort()));
        _outboundConnections.add(connection);
        return connection;
    }

    OutboundLink findOutboundLink(String url) throws MessagingException
    {
        if (_outboundLinks.containsKey(url))
        {
            OutboundLink link = _outboundLinks.get(url);
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
        Connection con = findConnection(settings);
        OutboundLink link = con.createOutBoundLink(settings.getTarget());
        _logger.info(String.format("Created outbound link %s @ %s:%s", settings.getTarget(), settings.getHost(),
                settings.getPort()));
        return link;
    }

    List<OutboundLink> routeOutbound(String address) throws MessagingException
    {
        List<OutboundLink> links = new ArrayList<OutboundLink>();
        for (String key : _config.getOutboundRoutes().keySet())
        {
            RouteEntry route = _config.getOutboundRoutes().get(key);
            if (route.getPattern().matcher(address).matches())
            {
                for (String addr : route.getAddressList())
                {
                    links.add(findOutboundLink(addr));
                }
            }
        }
        return links;
    }

    @Override
    public void handle(Message<JsonObject> m)
    {
        try
        {
            org.apache.qpid.proton.message.Message msg = _msgFactory.convert(m.body());
            if (msg.getReplyTo() == null && m.replyAddress() != null)
            {
                msg.setReplyTo(_replyToAddressPrefix + "/" + m.replyAddress());
            }

            String routingKey = m.address(); // default
            if (_config.isUseCustomPropertyForOutbound() && _config.getOutboundRoutingPropertyName() != null)
            {
                if (m.body().containsKey("properties")
                        && m.body().getJsonObject("properties").containsKey(_config.getOutboundRoutingPropertyName()))
                {
                    routingKey = m.body().getJsonObject("properties")
                            .getString(_config.getOutboundRoutingPropertyName());
                }
                else if (m.body().containsKey("application-properties")
                        && m.body().getJsonObject("application-properties")
                                .containsKey(_config.getOutboundRoutingPropertyName()))
                {
                    routingKey = m.body().getJsonObject("application-properties")
                            .getString(_config.getOutboundRoutingPropertyName());
                }

                if (_logger.isInfoEnabled())
                {
                    _logger.info("\n============= Custom Routing Property ============");
                    _logger.info("Custom routing property name : " + _config.getOutboundRoutingPropertyName());
                    _logger.info("Routing property value : " + routingKey);
                    _logger.info("============= /Custom Routing Property ============\n");
                }
            }

            List<OutboundLink> links = routeOutbound(routingKey);
            if (links.size() == 0)
            {
                if (_defaultOutboundLink.getConnection().getState() == State.FAILED)
                {
                    _logger.info("Reconnecting to default outbound link");
                    _defaultOutboundLink = findOutboundLink(_config.getDefaultOutboundAddress());
                }
                links.add(_defaultOutboundLink);
            }

            if (m.replyAddress() != null)
            {
                _vertxReplyTo.put(m.replyAddress(), m);
            }

            if (_logger.isInfoEnabled())
            {
                _logger.info("\n============= Outbound Routing ============");
                _logger.info(String.format("Received msg from vertx [to=%s, reply-to=%s, body=%s] ", m.address(),
                        m.replyAddress(), m.body().encode(), m.replyAddress()));
                StringBuilder b = new StringBuilder("Matched the following outbound links [");
                for (OutboundLink link : links)
                {
                    b.append(link).append(" ");
                }
                b.append("].");
                _logger.info(b.toString());
                _logger.info("============= /Outbound Routing ============/n");
            }

            for (OutboundLink link : links)
            {
                link.send(msg);
            }
        }
        catch (MessagingException e)
        {
            System.out.println("Exception routing message " + e);
            e.printStackTrace();
        }
    }

    // ------------ Event Handler ------------------------

    public void onConnectionOpen(Connection con)
    {
    }

    public void onConnectionClosed(Connection conn)
    {
    }

    public void onSessionOpen(Session ssn)
    {
    }

    public void onSessionClosed(Session ssn)
    {

    }

    public void onOutboundLinkOpen(OutboundLink link)
    {
        Connection con = link.getConnection();
        if (con.isInbound())
        {
            String address = link.getAddress();
            String url = new StringBuilder(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                    .append("/").append(address).toString();
            _outboundLinks.put(url, link);

            if (_logger.isInfoEnabled())
            {
                _logger.info("\n============= Outbound Route ============");
                _logger.info(String.format("Adding a mapping for {%s : %s}", address, url));
                _logger.info("============= /Outbound Route) ============\n");
            }

            if (_config.getOutboundRoutes().containsKey(address))
            {
                _config.getOutboundRoutes().get(address).add(url);
            }
            else
            {
                _config.getOutboundRoutes().put(address, RouterConfig.createRouteEntry(_config, address, url));
            }
        }
    }

    public void onOutboundLinkClosed(OutboundLink link)
    {
        Connection con = link.getConnection();
        String address = link.getAddress();
        String url = new StringBuilder(con.getSettings().getHost()).append(":").append(con.getSettings().getPort())
                .append("/").append(address).toString();
        _outboundLinks.remove(url);
        if (link.getConnection().isInbound() && _config.getOutboundRoutes().containsKey(address))
        {
            RouteEntry entry = _config.getOutboundRoutes().get(address);
            entry.remove(url);
            if (entry.getAddressList().size() == 0)
            {
                _config.getOutboundRoutes().remove(address);
            }
        }
    }

    public void onOutboundLinkCredit(OutboundLink link, int credits)
    {
    }

    public void onClearToSend(OutboundLink link)
    {
    }

    public void onSettled(Tracker tracker)
    {
    }

    public void onInboundLinkOpen(InboundLink link)
    {
        try
        {
            link.setCredits(10);
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
    }

    public void onInboundLinkClosed(InboundLink link)
    {
    }

    public void onCreditOffered(InboundLink link, int offered)
    {
    }

    public void onMessage(InboundLink link, AmqpMessage msg)
    {
        JsonObject out = _msgFactory.convert(msg.getProtocolMessage());

        // Handle replyTo
        if (msg.getAddress() != null)
        {
            try
            {
                ConnectionSettings settings = URLParser.parse(msg.getAddress());
                if (_vertxReplyTo.containsKey(settings.getTarget()))
                {
                    if (_logger.isInfoEnabled())
                    {
                        _logger.info("\n============= Inbound Routing (Reply-to) ============");
                        _logger.info(String.format(
                                "Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s'", msg.getAddress(),
                                msg.getReplyTo(), msg.getContent(), link));
                        _logger.info("It's a reply to vertx message with reply-to=" + settings.getTarget());
                        _logger.info("============= /Inbound Routing (Reply-to) ============\n");
                    }
                    Message<JsonObject> request = _vertxReplyTo.get(settings.getTarget());
                    request.reply(out);
                    _vertxReplyTo.remove(settings.getTarget());
                    request = null;
                    return;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                // ignore
            }
        }

        String key = null;
        switch (_config.getInboundRoutingPropertyType())
        {
        case LINK_NAME:
            key = link.getAddress();
            break;
        case SUBJECT:
            key = msg.getSubject();
            break;
        case MESSAGE_ID:
            key = msg.getMessageId().toString();
            break;
        case CORRELATION_ID:
            key = msg.getCorrelationId().toString();
            break;
        case ADDRESS:
            key = msg.getAddress();
            break;
        case REPLY_TO:
            key = msg.getReplyTo();
            break;
        case CUSTOM:
            key = (String) msg.getApplicationProperties().get(_config.getInboundRoutingPropertyName());
            break;
        }

        List<String> addressList = new ArrayList<String>();
        if (_config.getInboundRoutes().size() == 0)
        {
            addressList.add(key);
        }
        else
        {
            for (String k : _config.getInboundRoutes().keySet())
            {
                RouteEntry route = _config.getInboundRoutes().get(k);
                if (route.getPattern().matcher(key).matches())
                {
                    for (String addr : route.getAddressList())
                    {
                        addressList.add(addr);
                    }
                }
            }
        }

        if (addressList.size() == 0 && _config.getDefaultInboundAddress() != null)
        {
            addressList.add(_config.getDefaultInboundAddress());
        }

        for (String address : addressList)
        {
            if (msg.getReplyTo() != null)
            {
                _eb.send(address, out, new ReplyHandler(msg.getProtocolMessage()));
            }
            else
            {
                _eb.send(address, out);
            }
        }
        if (_logger.isInfoEnabled())
        {
            _logger.info("\n============= Inbound Routing ============");
            _logger.info(String.format("Received message [to=%s, reply-to=%s, body=%s] from AMQP peer '%s'",
                    msg.getAddress(), msg.getReplyTo(), msg.getContent(), link));
            _logger.info(String.format("Inbound routing info [key=%s, value=%s]",
                    _config.getInboundRoutingPropertyType(), key));
            _logger.info("Matched the following vertx address list : " + addressList);
            _logger.info("============= /Inbound Routing ============\n");
        }
    }

    // ---------- / Event Handler -----------------------

    // ---------- Helper classes
    class ReplyHandler implements Handler<AsyncResult<Message<JsonObject>>>
    {
        org.apache.qpid.proton.message.Message _protocolMsg;

        ReplyHandler(org.apache.qpid.proton.message.Message m)
        {
            _protocolMsg = m;
        }

        @Override
        public void handle(AsyncResult<Message<JsonObject>> result)
        {
            Message<JsonObject> msg = result.result();
            try
            {
                if (_logger.isInfoEnabled())
                {
                    _logger.info("\n============= Outbound Routing (Reply To) ============");
                    _logger.info("Routing vertx reply to AMQP space");
                    _logger.info("Reply msg : " + msg.body());
                    _logger.info("============= /Outbound Routing (Reply To) ============\n");
                }
                OutboundLink link = findOutboundLink(_protocolMsg.getReplyTo());
                link.send(_msgFactory.convert(msg.body()));
            }
            catch (MessagingException e)
            {
                _logger.error("Error handling reply", e);
            }
        }
    }

    class ConnectionResultHander implements Handler<AsyncResult<NetSocket>>
    {
        Connection _connection;

        Throwable _cause;

        ConnectionResultHander(Connection conn)
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
        EventHandler _handler;

        InboundConnectionHandler(EventHandler handler)
        {
            _handler = handler;
        }

        public void handle(NetSocket sock)
        {
            ConnectionSettings settings = new ConnectionSettings();
            settings.setHost(sock.remoteAddress().host());
            settings.setPort(sock.remoteAddress().port());
            Connection connection = new Connection(settings, _handler, true);
            connection.setNetSocket(sock);
            connection.open();
        }
    }
}