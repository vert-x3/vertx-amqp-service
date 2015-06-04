package io.vertx.ext.amqp;

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

import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.impl.AmqpServiceConfig;
import io.vertx.ext.amqp.impl.config.AmqpServiceConfigImpl;
import io.vertx.ext.amqp.impl.routing.InboundRoutingPropertyType;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 *
 */
public class TestRouterConfig extends TestCase
{
    @Test
    public void testInboundRoutingKeyValues()
    {
        assertEquals(InboundRoutingPropertyType.SUBJECT, InboundRoutingPropertyType.get("SUBJECT"));
        assertEquals(InboundRoutingPropertyType.ADDRESS, InboundRoutingPropertyType.get(""));
        assertEquals(InboundRoutingPropertyType.ADDRESS, InboundRoutingPropertyType.get(null));
        assertEquals(InboundRoutingPropertyType.CUSTOM, InboundRoutingPropertyType.get("xxxxxxx"));
    }

    @Test
    public void testConfigDefault()
    {
        AmqpServiceConfig config = new AmqpServiceConfigImpl(new JsonObject());

        assertEquals("localhost", config.getInboundHost());
        assertEquals(5673, config.getInboundPort());
        assertEquals("amqp://localhost:5672/vertx", config.getDefaultOutboundAddress());
        assertEquals("vertx.service-amqp.bridge", config.getDefaultHandlerAddress());
        assertEquals(0, config.getHandlerAddressList().size());
        assertEquals(0, config.getInboundRoutes().size());
        assertEquals(0, config.getOutboundRoutes().size());
        assertEquals(InboundRoutingPropertyType.ADDRESS, config.getInboundRoutingPropertyType());
        assertFalse(config.isUseCustomPropertyForOutbound());
        assertNull(config.getOutboundRoutingPropertyName());
        assertNull(config.getInboundRoutingPropertyName());
    }

    @Test
    public void testConfig1() throws FileNotFoundException, URISyntaxException
    {
        final URL url = getClass().getResource("/test-config1.json");
        JsonObject obj = new JsonObject(new Scanner(new File(url.toURI())).useDelimiter("\\Z").next());

        AmqpServiceConfig config = new AmqpServiceConfigImpl(obj);

        assertEquals("localhost", config.getInboundHost());
        assertEquals(5673, config.getInboundPort());
        assertEquals("amqp://localhost:5672/vertx", config.getDefaultOutboundAddress());
        assertEquals("vertx.service-amqp.bridge", config.getDefaultHandlerAddress());
        assertEquals(0, config.getHandlerAddressList().size());
        assertEquals(0, config.getInboundRoutes().size());
        assertEquals(0, config.getOutboundRoutes().size());
        assertEquals(InboundRoutingPropertyType.ADDRESS, config.getInboundRoutingPropertyType());
        assertFalse(config.isUseCustomPropertyForOutbound());
        assertNull(config.getOutboundRoutingPropertyName());
        assertNull(config.getInboundRoutingPropertyName());
        assertNull(config.getDefaultInboundAddress());
    }

    @Test
    public void testConfig2() throws FileNotFoundException, URISyntaxException
    {
        final URL url = getClass().getResource("/test-config2.json");
        JsonObject obj = new JsonObject(new Scanner(new File(url.toURI())).useDelimiter("\\Z").next());

        AmqpServiceConfig config = new AmqpServiceConfigImpl(obj);

        assertEquals("localhost", config.getInboundHost());
        assertEquals(5673, config.getInboundPort());
        assertEquals("amqp://localhost:5672/vertx", config.getDefaultOutboundAddress());
        assertEquals("vertx.service-amqp.bridge", config.getDefaultHandlerAddress());
        assertEquals(3, config.getHandlerAddressList().size());
        assertEquals(2, config.getInboundRoutes().size());
        assertEquals(3, config.getOutboundRoutes().size());
        assertEquals(InboundRoutingPropertyType.SUBJECT, config.getInboundRoutingPropertyType());
        assertFalse(config.isUseCustomPropertyForOutbound());
        assertNull(config.getOutboundRoutingPropertyName());
        assertNull(config.getInboundRoutingPropertyName());
        assertNull(config.getDefaultInboundAddress());
    }

    @Test
    public void testConfig3() throws FileNotFoundException, URISyntaxException
    {
        final URL url = getClass().getResource("/test-config3.json");
        JsonObject obj = new JsonObject(new Scanner(new File(url.toURI())).useDelimiter("\\Z").next());

        AmqpServiceConfig config = new AmqpServiceConfigImpl(obj);

        assertEquals("localhost", config.getInboundHost());
        assertEquals(5673, config.getInboundPort());
        assertEquals("amqp://localhost:5672/vertx", config.getDefaultOutboundAddress());
        assertEquals("vertx.service-amqp.bridge", config.getDefaultHandlerAddress());
        assertEquals(3, config.getHandlerAddressList().size());
        assertEquals(2, config.getInboundRoutes().size());
        assertEquals(3, config.getOutboundRoutes().size());
        assertEquals(InboundRoutingPropertyType.CUSTOM, config.getInboundRoutingPropertyType());
        assertTrue(config.isUseCustomPropertyForOutbound());
        assertEquals("routing-key", config.getOutboundRoutingPropertyName());
        assertEquals("station-code", config.getInboundRoutingPropertyName());
        assertEquals("test", config.getDefaultInboundAddress());
    }
}