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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.InboundRoutingPropertyType;
import io.vertx.ext.amqp.AmqpServiceConfig;
import io.vertx.ext.amqp.RouteEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AmqpServiceConfigImpl implements AmqpServiceConfig
{
    private String _inboundHost;

    private int _inboundPort;

    private String _defaultHandlerAddress;

    private String _defaultOutboundAddress;

    private String _defaultInboundAddress = null;

    private List<String> _handlerAddressList = new ArrayList<String>();

    boolean _isUseCustomPropertyForOutbound = false;

    String _outboundRoutingPropertyName = null;

    int _maxedCachedURLEntries = 10;

    int _defaultLinkCredit = 1;

    Map<String, RouteEntry> _outboundRoutes = new ConcurrentHashMap<String, RouteEntry>();

    String _inboundRoutingPropertyName = null;

    InboundRoutingPropertyType _inboundRoutingPropertyType = InboundRoutingPropertyType.ADDRESS;

    Map<String, RouteEntry> _inboundRoutes = new ConcurrentHashMap<String, RouteEntry>();

    public AmqpServiceConfigImpl(JsonObject config)
    {
        _inboundHost = config.getString("amqp.inbound-host", "localhost");
        _inboundPort = config.getInteger("amqp.inbound-port", 5673);
        _defaultOutboundAddress = config.getString("amqp.default-outbound-address", "amqp://localhost:5672/vertx");
        _defaultHandlerAddress = config.getString("address", "vertx.service-amqp");
        _defaultInboundAddress = config.getString("vertx.default-inbound-address", null);

        if (config.containsKey("vertx.handlers"))
        {
            JsonArray handlers = config.getJsonArray("vertx.handlers");
            Iterator<Object> it = handlers.iterator();
            while (it.hasNext())
            {
                _handlerAddressList.add((String) it.next());
            }
        }

        if (config.containsKey("vertx.routing-outbound"))
        {
            JsonObject _outboundRouting = config.getJsonObject("vertx.routing-outbound");

            if (_outboundRouting.containsKey("routing-property-name"))
            {
                _isUseCustomPropertyForOutbound = true;
                _outboundRoutingPropertyName = _outboundRouting.getString("routing-property-name");
            }

            if (_outboundRouting.containsKey("routes"))
            {
                pouplateRouteMap(_outboundRoutes, _outboundRouting.getJsonObject("routes"));
            }
        }

        if (config.containsKey("vertx.routing-inbound"))
        {
            JsonObject _inboundRouting = config.getJsonObject("vertx.routing-inbound");

            if (_inboundRouting.containsKey("routing-property-type"))
            {
                _inboundRoutingPropertyType = InboundRoutingPropertyType.get(_inboundRouting
                        .getString("routing-property-type"));
                if (InboundRoutingPropertyType.CUSTOM == _inboundRoutingPropertyType)
                {
                    _inboundRoutingPropertyName = _inboundRouting.getString("routing-property-name");
                }
            }

            if (_inboundRouting.containsKey("routes"))
            {
                pouplateRouteMap(_inboundRoutes, _inboundRouting.getJsonObject("routes"));
            }
        }

        // TODO implement other config options
    }

    private void pouplateRouteMap(Map<String, RouteEntry> map, JsonObject routes)
    {
        for (String key : routes.fieldNames())
        {
            RouteEntry entry = new RouteEntry(Pattern.compile(key), routes.getString(key));
            map.put(key, entry);
        }
    }

    @Override
    public String getInboundHost()
    {
        return _inboundHost;
    }

    @Override
    public int getInboundPort()
    {
        return _inboundPort;
    }

    @Override
    public String getDefaultHandlerAddress()
    {
        return _defaultHandlerAddress;
    }

    @Override
    public String getDefaultOutboundAddress()
    {
        return _defaultOutboundAddress;
    }

    @Override
    public List<String> getHandlerAddressList()
    {
        return _handlerAddressList;
    }

    @Override
    public boolean isUseCustomPropertyForOutbound()
    {
        return _isUseCustomPropertyForOutbound;
    }

    @Override
    public String getOutboundRoutingPropertyName()
    {
        return _outboundRoutingPropertyName;
    }

    @Override
    public Map<String, RouteEntry> getOutboundRoutes()
    {
        return _outboundRoutes;
    }

    @Override
    public String getInboundRoutingPropertyName()
    {
        return _inboundRoutingPropertyName;
    }

    @Override
    public String getDefaultInboundAddress()
    {
        return _defaultInboundAddress;
    }

    @Override
    public InboundRoutingPropertyType getInboundRoutingPropertyType()
    {
        return _inboundRoutingPropertyType;
    }

    @Override
    public Map<String, RouteEntry> getInboundRoutes()
    {
        return _inboundRoutes;
    }

    public static RouteEntry createRouteEntry(String pattern, String address)
    {
        return new RouteEntry(Pattern.compile(pattern), address);
    }

    @Override
    public int getMaxedCachedURLEntries()
    {
        return _maxedCachedURLEntries;
    }

    @Override
    public int getDefaultLinkCredit()
    {
        return _defaultLinkCredit;
    }
}