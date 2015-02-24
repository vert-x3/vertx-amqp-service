/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.vertx.ext.amqp;

/**
 * Applications could extend this class to provide their own implementation of
 * the ConnectionSettings based on their configuration.
 */
public class ConnectionSettings
{
    protected String scheme = "amqp";

    protected String host = "localhost";

    protected int port = 5672;

    protected String user = "";

    protected String pass = "";

    protected String id = null;

    protected boolean tcpNodelay = false;

    protected int readBufferSize = 65535;

    protected int writeBufferSize = 65535;

    protected long connectTimeout = Long.getLong("vertx-mod.connection.timeout", 60000);

    protected long idleTimeout = Long.getLong("vertx-mod.connection.idle_timeout", 60000);

    protected String target = null;
    
    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public void setPass(String pass)
    {
        this.pass = pass;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setTcpNodelay(boolean tcpNodelay)
    {
        this.tcpNodelay = tcpNodelay;
    }

    public void setReadBufferSize(int readBufferSize)
    {
        this.readBufferSize = readBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize)
    {
        this.writeBufferSize = writeBufferSize;
    }

    public void setConnectTimeout(long connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    public void setIdleTimeout(long idleTimeout)
    {
        this.idleTimeout = idleTimeout;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getId()
    {
        return id;
    }

    public boolean isTcpNodelay()
    {
        return tcpNodelay;
    }

    public int getReadBufferSize()
    {
        return readBufferSize;
    }

    public int getWriteBufferSize()
    {
        return writeBufferSize;
    }

    public long getConnectTimeout()
    {
        return connectTimeout;
    }

    public long getIdleTimeout()
    {
        return idleTimeout;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }
    
    public String getTarget()
    {
        return target;
    }
}