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
package io.vertx.ext.amqp.impl.routing;

import static io.vertx.ext.amqp.impl.util.Functions.format;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.impl.AmqpServiceConfig;
import io.vertx.ext.amqp.impl.config.AmqpServiceConfigImpl;
import io.vertx.ext.amqp.impl.config.ConfigRouteEntry;
import io.vertx.ext.amqp.impl.protocol.InboundMessage;
import io.vertx.ext.amqp.impl.protocol.LinkManager;
import io.vertx.ext.amqp.impl.util.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contains the routing logic used by the AMQP Service
 * 
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 *
 */
public class MessageRouter
{
    private static final LogManager LOG = LogManager.get("MessageRouter:", LinkManager.class);

    private final AmqpServiceConfig _config;

    public MessageRouter(AmqpServiceConfig config)
    {
        _config = config;
        StringBuilder b = new StringBuilder();
        b.append("Router Config \n[\n");
        b.append("Default outbound-AMQP-address : ").append(config.getDefaultOutboundAddress()).append("\n");
        b.append("Default inbound-vertx-address : ").append(config.getDefaultInboundAddress()).append("\n");
        b.append("Default vertx handler address : ").append(config.getDefaultHandlerAddress()).append("\n");
        b.append("Additional handler address list : ").append(config.getHandlerAddressList()).append("\n");
        b.append("]\n");
        LOG.info(b.toString());
    }

    public List<String> routeOutgoing(Message<JsonObject> vertxMsg) throws MessagingException
    {
        String routingKey = extractOutgoingRoutingKey(vertxMsg);
        List<String> addrList = new ArrayList<String>();
        for (String key : _config.getOutboundRoutes().keySet())
        {
            ConfigRouteEntry route = _config.getOutboundRoutes().get(key);
            if (route.getPattern().matcher(routingKey).matches())
            {
                addrList.addAll(route.getAddressList());
            }
        }
        return addrList;
    }

    /**
     * By default it uses the address field.
     * 
     * If a custom routing-property is specified in config it will look to see,
     * if that property is available <br>
     * 1. As field within the Json message <br>
     * 2. If a "properties" field is available, it will look for it as a sub
     * field under that <br>
     * 3. If an "application-properties" field is available, it will look for it
     * as a sub field under that
     * 
     * If no custome property is specified, then it looks if "vertx.routing-key"
     * is specified as a field within the Json message.
     */
    private String extractOutgoingRoutingKey(Message<JsonObject> vertxMsg)
    {
        String routingKey = vertxMsg.address(); // default
        if (_config.isUseCustomPropertyForOutbound() && _config.getOutboundRoutingPropertyName() != null)
        {
            if (vertxMsg.body().containsKey(_config.getOutboundRoutingPropertyName()))
            {
                routingKey = vertxMsg.body().getString(_config.getOutboundRoutingPropertyName());
            }
            else if (vertxMsg.body().containsKey("properties")
                    && vertxMsg.body().getJsonObject("properties") instanceof Map
                    && vertxMsg.body().getJsonObject("properties")
                            .containsKey(_config.getOutboundRoutingPropertyName()))
            {
                routingKey = vertxMsg.body().getJsonObject("properties")
                        .getString(_config.getOutboundRoutingPropertyName());
            }
            else if (vertxMsg.body().containsKey("application-properties")
                    && vertxMsg.body().getJsonObject("application-properties") instanceof Map
                    && vertxMsg.body().getJsonObject("application-properties")
                            .containsKey(_config.getOutboundRoutingPropertyName()))
            {
                routingKey = vertxMsg.body().getJsonObject("application-properties")
                        .getString(_config.getOutboundRoutingPropertyName());
            }

            if (LOG.isDebugEnabled())
            {
                LOG.debug("\n============= Custom Routing Property ============");
                LOG.debug("Custom routing property name : %s",_config.getOutboundRoutingPropertyName());
                LOG.debug("Routing property value : %s",routingKey);
                LOG.debug("============= /Custom Routing Property ============/n");
            }
        }
        else if (vertxMsg.body().containsKey("vertx.routing-key"))
        {
            routingKey = vertxMsg.body().getString("vertx.routing-key");
        }
        return routingKey;
    }

    public List<String> routeIncoming(InboundMessage amqpMsg, String alternateKey)
    {
        String routingKey = extractIncomingRoutingKey(amqpMsg);
        LOG.info(format("Inbound routing info [key=%s, value=%s]", _config.getInboundRoutingPropertyType(),
                routingKey));
        if (routingKey == null || routingKey.trim().isEmpty())
        {
            routingKey = alternateKey;
        }

        List<String> addressList = new ArrayList<String>();
        for (String k : _config.getInboundRoutes().keySet())
        {
            ConfigRouteEntry route = _config.getInboundRoutes().get(k);
            if (route.getPattern().matcher(routingKey).matches())
            {
                addressList.addAll(route.getAddressList());
            }
        }

        // no matches
        if (addressList.size() == 0)
        {
            // use default if specified.
            if (_config.getDefaultInboundAddress() != null)
            {
                addressList.add(_config.getDefaultInboundAddress());
            }
            else
            {
                // else use the routing key as the vertx address
                addressList.add(routingKey);
            }
        }
        return addressList;
    }

    private String extractIncomingRoutingKey(InboundMessage amqpMsg)
    {
        switch (_config.getInboundRoutingPropertyType())
        {
        case ADDRESS:
            return amqpMsg.getAddress();
        case SUBJECT:
            return amqpMsg.getSubject();
        case CUSTOM:
            return (String) amqpMsg.getApplicationProperties().get(_config.getInboundRoutingPropertyName());
        default:
            return null;
        }
    }

    public void addOutboundRoute(String eventbusAddress, String amqpAddress)
    {
        if (_config.getOutboundRoutes().containsKey(eventbusAddress))
        {
            _config.getOutboundRoutes().get(eventbusAddress).add(amqpAddress);
        }
        else
        {
            _config.getOutboundRoutes().put(eventbusAddress,
                    AmqpServiceConfigImpl.createRouteEntry(eventbusAddress, amqpAddress));
        }
        if (LOG.isInfoEnabled())
        {
            LOG.info("\n============= Outbound Route ============");
            LOG.info("Adding the route entry : {%s : %s}", eventbusAddress, amqpAddress);
            LOG.info("============= /Outbound Route) ============\n");
        }
    }

    public void removeOutboundRoute(String eventbusAddress, String amqpAddress)
    {
        if (_config.getOutboundRoutes().containsKey(eventbusAddress))
        {
            ConfigRouteEntry entry = _config.getOutboundRoutes().get(eventbusAddress);
            entry.remove(amqpAddress);
            if (entry.getAddressList().size() == 0)
            {
                _config.getOutboundRoutes().remove(eventbusAddress);
            }
            if (LOG.isInfoEnabled())
            {
                LOG.info("\n============= Outbound Route ============");
                LOG.info("Removing the route entry : {%s : %s}", eventbusAddress, amqpAddress);
                LOG.info("============= /Outbound Route) ============\n");
            }
        }
    }

    public void addInboundRoute(String amqpAddress, String eventbusAddress)
    {
        if (_config.getInboundRoutes().containsKey(amqpAddress))
        {
            _config.getInboundRoutes().get(amqpAddress).add(eventbusAddress);
        }
        else
        {
            _config.getInboundRoutes().put(amqpAddress,
                    AmqpServiceConfigImpl.createRouteEntry(amqpAddress, eventbusAddress));
        }
        if (LOG.isInfoEnabled())
        {
            LOG.info("\n============= Inbound Route ============");
            LOG.info("Adding the route entry : {%s : %s}", amqpAddress, eventbusAddress);
            LOG.info("============= /Inbound Route) ============\n");
        }
    }

    public void removeInboundRoute(String amqpAddress, String eventbusAddress)
    {
        if (_config.getOutboundRoutes().containsKey(amqpAddress))
        {
            ConfigRouteEntry entry = _config.getOutboundRoutes().get(amqpAddress);
            entry.remove(eventbusAddress);
            if (entry.getAddressList().size() == 0)
            {
                _config.getOutboundRoutes().remove(amqpAddress);
            }
            if (LOG.isInfoEnabled())
            {
                LOG.info("\n============= Inbound Route ============");
                LOG.info(String.format("Removing the route entry : {%s : %s}", amqpAddress, eventbusAddress));
                LOG.info("============= /Inbound Route) ============\n");
            }
        }
    }
}