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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Allows a vert.x application to customize the establishing of an incoming link.
 * Prefetch and reliability are supported and recovery options in a future
 * release. Future extension point to add more options.
 * 
 * @author <a href="mailto:rajith@rajith.lk">Rajith Muditha Attapattu</a>
 *
 */
@DataObject
public class IncomingLinkOptions
{
    public final static String PREFETCH = "prefetch";

    public final static String RELIABILITY = "reliability";

    public final static String RECOVERY_OPTIONS = "recovery-options";

    private int prefetch = 1;

    private ReliabilityMode reliability = ReliabilityMode.UNRELIABLE;

    private RetryOptions recoveryOptions = new RetryOptions();

    public IncomingLinkOptions()
    {
    }

    public IncomingLinkOptions(IncomingLinkOptions options)
    {
        this.prefetch = options.prefetch;
        this.reliability = options.reliability;
        this.recoveryOptions = options.recoveryOptions;
    }

    public IncomingLinkOptions(JsonObject options)
    {
        this.prefetch = options.getInteger(PREFETCH, 1);
        this.reliability = ReliabilityMode.valueOf(options.getString(RELIABILITY, ReliabilityMode.UNRELIABLE.name()));
        this.recoveryOptions = new RetryOptions(options.getJsonObject(RECOVERY_OPTIONS));
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.put(PREFETCH, prefetch);
        json.put(RELIABILITY, reliability.name());
        return json;
    }

    public int getPrefetch()
    {
        return prefetch;
    }

    /**
     * <i>Defaults to "1". </i><br>
     * <i>If set to a value > 0 </i>, the Vert.x-AMQP-Service will automatically
     * fetch more messages when a certain number of messages are marked as
     * either accepted, rejected or released. The Vert.x-AMQP-Service will
     * determine the optimum threshold for when the fetch happens and how much
     * to fetch. <br>
     * <i>If set to "zero"</i>, the vert.x application will need to explicitly
     * request messages using
     * {@link AmqpService#fetch(String, int, io.vertx.core.Handler)}
     */
    public void setPrefetch(int prefetch)
    {
        this.prefetch = prefetch;
    }

    /**
     * Please see {@link ReliabilityMode} to understand the reliability modes
     * and it's implications.
     */
    public ReliabilityMode getReliability()
    {
        return reliability;
    }

    public void setReliability(ReliabilityMode reliability)
    {
        this.reliability = reliability;
    }

    public RetryOptions getRecoveryOptions()
    {
        return recoveryOptions;
    }

    public void setRecoveryOptions(RetryOptions recoveryOptions)
    {
        this.recoveryOptions = recoveryOptions;
    }

    @Override
    public String toString()
    {
        return toJson().encode();
    }
}