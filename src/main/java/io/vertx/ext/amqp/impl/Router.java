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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.amqp.MessagingException;
import io.vertx.ext.amqp.AmqpServiceConfig;
import io.vertx.ext.amqp.RouteEntry;

/**
 * Contains the routing logic used by the AMQP Service
 * 
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 *
 */
public class Router
{
    private static final Logger _logger = LoggerFactory.getLogger(Router.class);

    private final AmqpServiceConfig _config;

    public Router(AmqpServiceConfig config)
    {
        _config = config;

        if (_logger.isInfoEnabled())
        {
            StringBuilder b = new StringBuilder();
            b.append("Router Config \n[\n");
            b.append("Default outbound-AMQP-address : ").append(config.getDefaultOutboundAddress()).append("\n");
            b.append("Default inbound-vertx-address : ").append(config.getDefaultInboundAddress()).append("\n");
            b.append("Default vertx handler address : ").append(config.getDefaultHandlerAddress()).append("\n");
            b.append("Additional handler address list : ").append(config.getHandlerAddressList()).append("\n");
            b.append("]\n");
            _logger.info(b.toString());
        }
    }

    /*
     * Looks at the Vertx address and provides the list of matching AMQP
     * addresses.
     */
    List<String> routeOutbound(String routingkey) throws MessagingException
    {
        List<String> addrList = new ArrayList<String>();
        for (String key : _config.getOutboundRoutes().keySet())
        {
            RouteEntry route = _config.getOutboundRoutes().get(key);
            if (route.getPattern().matcher(routingkey).matches())
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
    String extractOutboundRoutingKey(Message<JsonObject> m)
    {
        String routingKey = m.address(); // default
        if (_config.isUseCustomPropertyForOutbound() && _config.getOutboundRoutingPropertyName() != null)
        {
            if (m.body().containsKey(_config.getOutboundRoutingPropertyName()))
            {
                routingKey = m.body().getString(_config.getOutboundRoutingPropertyName());
            }
            else if (m.body().containsKey("properties") && m.body().getJsonObject("properties") instanceof Map
                    && m.body().getJsonObject("properties").containsKey(_config.getOutboundRoutingPropertyName()))
            {
                routingKey = m.body().getJsonObject("properties").getString(_config.getOutboundRoutingPropertyName());
            }
            else if (m.body().containsKey("application-properties")
                    && m.body().getJsonObject("application-properties") instanceof Map
                    && m.body().getJsonObject("application-properties")
                            .containsKey(_config.getOutboundRoutingPropertyName()))
            {
                routingKey = m.body().getJsonObject("application-properties")
                        .getString(_config.getOutboundRoutingPropertyName());
            }

            if (_logger.isDebugEnabled())
            {
                _logger.debug("\n============= Custom Routing Property ============");
                _logger.debug("Custom routing property name : " + _config.getOutboundRoutingPropertyName());
                _logger.debug("Routing property value : " + routingKey);
                _logger.debug("============= /Custom Routing Property ============/n");
            }
        }
        else if (m.body().containsKey("vertx.routing-key"))
        {
            routingKey = m.body().getString("vertx.routing-key");
        }
        return routingKey;
    }

    List<String> routeInbound(String routingKey)
    {
        List<String> addressList = new ArrayList<String>();
        if (_config.getInboundRoutes().size() == 0)
        {
            addressList.add(routingKey);
        }
        else
        {
            for (String k : _config.getInboundRoutes().keySet())
            {
                RouteEntry route = _config.getInboundRoutes().get(k);
                if (route.getPattern().matcher(routingKey).matches())
                {
                    addressList.addAll(route.getAddressList());
                }
            }
        }

        if (addressList.size() == 0 && _config.getDefaultInboundAddress() != null)
        {
            addressList.add(_config.getDefaultInboundAddress());
        }
        return addressList;
    }

    void addOutboundRoute(String eventbusAddress, String amqpAddress)
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
        if (_logger.isInfoEnabled())
        {
            _logger.info("\n============= Outbound Route ============");
            _logger.info(String.format("Adding the route entry : {%s : %s}", eventbusAddress, amqpAddress));
            _logger.info("============= /Outbound Route) ============\n");
        }
    }

    void removeOutboundRoute(String eventbusAddress, String amqpAddress)
    {
        if (_config.getOutboundRoutes().containsKey(eventbusAddress))
        {
            RouteEntry entry = _config.getOutboundRoutes().get(eventbusAddress);
            entry.remove(amqpAddress);
            if (entry.getAddressList().size() == 0)
            {
                _config.getOutboundRoutes().remove(eventbusAddress);
            }
            if (_logger.isInfoEnabled())
            {
                _logger.info("\n============= Outbound Route ============");
                _logger.info(String.format("Removing the route entry : {%s : %s}", eventbusAddress, amqpAddress));
                _logger.info("============= /Outbound Route) ============\n");
            }
        }
    }

    void addInboundRoute(String amqpAddress, String eventbusAddress)
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
        if (_logger.isInfoEnabled())
        {
            _logger.info("\n============= Inbound Route ============");
            _logger.info(String.format("Adding the route entry : {%s : %s}", amqpAddress, eventbusAddress));
            _logger.info("============= /Inbound Route) ============\n");
        }
    }

    void removeInboundRoute(String amqpAddress, String eventbusAddress)
    {
        if (_config.getOutboundRoutes().containsKey(amqpAddress))
        {
            RouteEntry entry = _config.getOutboundRoutes().get(amqpAddress);
            entry.remove(eventbusAddress);
            if (entry.getAddressList().size() == 0)
            {
                _config.getOutboundRoutes().remove(amqpAddress);
            }
            if (_logger.isInfoEnabled())
            {
                _logger.info("\n============= Inbound Route ============");
                _logger.info(String.format("Removing the route entry : {%s : %s}", amqpAddress, eventbusAddress));
                _logger.info("============= /Inbound Route) ============\n");
            }
        }
    }
}