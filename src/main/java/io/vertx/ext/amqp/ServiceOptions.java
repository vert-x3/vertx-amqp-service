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

@DataObject
public class ServiceOptions
{
    public final static String PREFETCH = "prefetch";

    public final static String EXCLUSIVE = "exclusive";

    public final static String RELIABILITY = "reliability";

    public final static String RECOVERY_OPTIONS = "recovery-options";

    private int prefetch = 1;

    private boolean exclusive = false;

    private ReliabilityMode reliability = ReliabilityMode.UNRELIABLE;

    private RecoveryOptions recoveryOptions = new RecoveryOptions();

    public ServiceOptions()
    {
    }

    public ServiceOptions(ServiceOptions options)
    {
        this.prefetch = options.prefetch;
        this.exclusive = options.exclusive;
        this.reliability = options.reliability;
        this.recoveryOptions = options.recoveryOptions;
    }

    public ServiceOptions(JsonObject options)
    {
        this.prefetch = options.getInteger(PREFETCH, 1);
        this.exclusive = options.getBoolean(EXCLUSIVE, false);
        this.reliability = ReliabilityMode.valueOf(options.getString(RELIABILITY, ReliabilityMode.UNRELIABLE.name()));
        this.recoveryOptions = new RecoveryOptions(options.getJsonObject(RECOVERY_OPTIONS));
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.put(PREFETCH, prefetch);
        json.put(EXCLUSIVE, exclusive);
        json.put(RELIABILITY, reliability.name());
        return json;
    }

    public int getPrefetch()
    {
        return prefetch;
    }

    public void setPrefetch(int prefetch)
    {
        this.prefetch = prefetch;
    }

    public boolean isExclusive()
    {
        return exclusive;
    }

    public void setExclusive(boolean exclusive)
    {
        this.exclusive = exclusive;
    }

    public ReliabilityMode getReliability()
    {
        return reliability;
    }

    public void setReliability(ReliabilityMode reliability)
    {
        this.reliability = reliability;
    }

    public RecoveryOptions getRecoveryOptions()
    {
        return recoveryOptions;
    }

    public void setRecoveryOptions(RecoveryOptions recoveryOptions)
    {
        this.recoveryOptions = recoveryOptions;
    }

    @Override
    public String toString()
    {
        return toJson().encode();
    }
}