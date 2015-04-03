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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkRouter
{
    protected final Map<String, String> _incomingLinkToVertxAddr = new ConcurrentHashMap<String, String>();

    protected final Map<String, String> _vertxAddrToOutgoingLink = new ConcurrentHashMap<String, String>();

    // Reverse map for easy cleanup
    protected final Map<String, String> _outgoingLinkToVertxAddr = new ConcurrentHashMap<String, String>();

    public LinkRouter()
    {
    }

    public String routeIncoming(String linkId)
    {
        return _incomingLinkToVertxAddr.get(linkId);
    }

    public String routeOutgoing(String vertxAddress)
    {
        return _vertxAddrToOutgoingLink.get(vertxAddress);
    }

    public void addIncomingRoute(String linkId, String vertxAddr)
    {
        _incomingLinkToVertxAddr.put(linkId, vertxAddr);
    }

    public void removeIncomingRoute(String linkId)
    {
        _incomingLinkToVertxAddr.remove(linkId);
    }

    public void addOutgoingRoute(String vertxAddr, String linkId)
    {
        _vertxAddrToOutgoingLink.put(vertxAddr, linkId);
        _outgoingLinkToVertxAddr.put(linkId, vertxAddr);
    }

    public void removeOutgoingRoute(String linkId)
    {
        _vertxAddrToOutgoingLink.remove(_outgoingLinkToVertxAddr.remove(linkId));
    }
}