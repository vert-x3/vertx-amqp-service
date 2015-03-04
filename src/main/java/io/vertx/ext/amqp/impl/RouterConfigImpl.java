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
import io.vertx.ext.amqp.RouteEntry;
import io.vertx.ext.amqp.RouterConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RouterConfigImpl implements RouterConfig
{
    private String _inboundHost;

    private int _inboundPort;

    private String _defaultHandlerAddress;

    private String _defaultOutboundAddress;

    private String _defaultInboundAddress = null;

    private List<String> _handlerAddressList = new ArrayList<String>();

    boolean _isUseCustomPropertyForOutbound = false;

    String _outboundRoutingPropertyName = null;

    Map<String, RouteEntry> _outboundRoutes = new ConcurrentHashMap<String, RouteEntry>();

    String _inboundRoutingPropertyName = null;

    InboundRoutingPropertyType _inboundRoutingPropertyType = InboundRoutingPropertyType.ADDRESS;

    Map<String, RouteEntry> _inboundRoutes = new ConcurrentHashMap<String, RouteEntry>();

    public RouterConfigImpl(JsonObject config)
    {
        _inboundHost = config.getString("amqp.inbound-host", "localhost");
        _inboundPort = config.getInteger("amqp.inbound-port", 5673);
        _defaultOutboundAddress = config.getString("amqp.default-outbound-address", "amqp://localhost:5672/vertx");
        _defaultHandlerAddress = config.getString("vertx.default-handler-address", "vertx.service-amqp");
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
    }

    private void pouplateRouteMap(Map<String, RouteEntry> map, JsonObject routes)
    {
        for (String key : routes.fieldNames())
        {
            RouteEntryImpl entry = new RouteEntryImpl(Pattern.compile(key), routes.getString(key));
            map.put(key, entry);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getInboundHost()
     */
    @Override
    public String getInboundHost()
    {
        return _inboundHost;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getInboundPort()
     */
    @Override
    public int getInboundPort()
    {
        return _inboundPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getDefaultHandlerAddress()
     */
    @Override
    public String getDefaultHandlerAddress()
    {
        return _defaultHandlerAddress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getDefaultOutboundAddress()
     */
    @Override
    public String getDefaultOutboundAddress()
    {
        return _defaultOutboundAddress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getHandlerAddressList()
     */
    @Override
    public List<String> getHandlerAddressList()
    {
        return _handlerAddressList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#isUseCustomPropertyForOutbound()
     */
    @Override
    public boolean isUseCustomPropertyForOutbound()
    {
        return _isUseCustomPropertyForOutbound;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getOutboundRoutingPropertyName()
     */
    @Override
    public String getOutboundRoutingPropertyName()
    {
        return _outboundRoutingPropertyName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getOutboundRoutes()
     */
    @Override
    public Map<String, RouteEntry> getOutboundRoutes()
    {
        return _outboundRoutes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getInboundRoutingPropertyName()
     */
    @Override
    public String getInboundRoutingPropertyName()
    {
        return _inboundRoutingPropertyName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getDefaultInboundAddress()
     */
    @Override
    public String getDefaultInboundAddress()
    {
        return _defaultInboundAddress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getInboundRoutingPropertyType()
     */
    @Override
    public InboundRoutingPropertyType getInboundRoutingPropertyType()
    {
        return _inboundRoutingPropertyType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.vertx.ext.amqp.RouterConfig#getInboundRoutes()
     */
    @Override
    public Map<String, RouteEntry> getInboundRoutes()
    {
        return _inboundRoutes;
    }

    public static RouteEntry createRouteEntry(RouterConfig config, String pattern, String address)
    {
        return new RouteEntryImpl(Pattern.compile(pattern), address);
    }
}