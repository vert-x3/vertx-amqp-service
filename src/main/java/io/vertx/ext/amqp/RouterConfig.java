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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RouterConfig
{
    enum INBOUND_ROUTING_PROPERTY_TYPE
    {

        LINK_NAME, SUBJECT, MESSAGE_ID, CORRELATION_ID, ADDRESS, REPLY_TO, CUSTOM;

        static INBOUND_ROUTING_PROPERTY_TYPE get(String key)
        {
            if (key == null || key.trim().equals(""))
            {
                return ADDRESS;
            }

            try
            {
                return INBOUND_ROUTING_PROPERTY_TYPE.valueOf(key);
            }
            catch (IllegalArgumentException e)
            {
                return CUSTOM;
            }
        }

    };

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

    INBOUND_ROUTING_PROPERTY_TYPE _inboundRoutingPropertyType = INBOUND_ROUTING_PROPERTY_TYPE.ADDRESS;

    Map<String, RouteEntry> _inboundRoutes = new ConcurrentHashMap<String, RouteEntry>();

    public RouterConfig(JsonObject config)
    {
        _inboundHost = config.getString("amqp.inbound-host", "localhost");
        _inboundPort = config.getInteger("amqp.inbound-port", 5673);
        _defaultOutboundAddress = config.getString("amqp.default-outbound-address", "amqp://localhost:5672/vertx");
        _defaultHandlerAddress = config.getString("vertx.default-handler-address", "vertx.mod-amqp");
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
                _inboundRoutingPropertyType = INBOUND_ROUTING_PROPERTY_TYPE.get(_inboundRouting
                        .getString("routing-property-type"));
                if (INBOUND_ROUTING_PROPERTY_TYPE.CUSTOM == _inboundRoutingPropertyType)
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
            RouteEntry entry = new RouteEntry(Pattern.compile(key), routes.getString(key));
            map.put(key, entry);
        }
    }

    public String getInboundHost()
    {
        return _inboundHost;
    }

    public int getInboundPort()
    {
        return _inboundPort;
    }

    public String getDefaultHandlerAddress()
    {
        return _defaultHandlerAddress;
    }

    public String getDefaultOutboundAddress()
    {
        return _defaultOutboundAddress;
    }

    public List<String> getHandlerAddressList()
    {
        return _handlerAddressList;
    }

    public boolean isUseCustomPropertyForOutbound()
    {
        return _isUseCustomPropertyForOutbound;
    }

    public String getOutboundRoutingPropertyName()
    {
        return _outboundRoutingPropertyName;
    }

    public Map<String, RouteEntry> getOutboundRoutes()
    {
        return _outboundRoutes;
    }

    public String getInboundRoutingPropertyName()
    {
        return _inboundRoutingPropertyName;
    }

    public String getDefaultInboundAddress()
    {
        return _defaultInboundAddress;
    }

    public INBOUND_ROUTING_PROPERTY_TYPE getInboundRoutingPropertyType()
    {
        return _inboundRoutingPropertyType;
    }

    public Map<String, RouteEntry> getInboundRoutes()
    {
        return _inboundRoutes;
    }

    public static RouteEntry createRouteEntry(RouterConfig config, String pattern, String address)
    {
        return config.new RouteEntry(Pattern.compile(pattern), address);
    }

    class RouteEntry
    {
        Pattern _pattern;

        List<String> _addressList = new ArrayList<String>();

        RouteEntry(Pattern p, String addr)
        {
            _pattern = p;
            _addressList.add(addr);
        }

        void add(String addr)
        {
            _addressList.add(addr);
        }

        void remove(String addr)
        {
            _addressList.remove(addr);
        }

        Pattern getPattern()
        {
            return _pattern;
        }

        List<String> getAddressList()
        {
            return _addressList;
        }
    }
}