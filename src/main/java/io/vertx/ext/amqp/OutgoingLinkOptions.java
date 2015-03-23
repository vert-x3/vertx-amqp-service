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
public class OutgoingLinkOptions
{
    public final static String RELIABILITY = "reliability";

    public final static String RECOVERY_OPTIONS = "recovery-options";

    private ReliabilityMode reliability = ReliabilityMode.UNRELIABLE;

    private RecoveryOptions recoveryOptions = new RecoveryOptions();

    public OutgoingLinkOptions()
    {
    }

    public OutgoingLinkOptions(OutgoingLinkOptions options)
    {
        this.reliability = options.reliability;
        this.recoveryOptions = options.recoveryOptions;
    }

    public OutgoingLinkOptions(JsonObject options)
    {
        this.reliability = ReliabilityMode.valueOf(options.getString(RELIABILITY, ReliabilityMode.UNRELIABLE.name()));
        this.recoveryOptions = new RecoveryOptions(options.getJsonObject(RECOVERY_OPTIONS));
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.put(RELIABILITY, reliability.name());
        json.put(RECOVERY_OPTIONS, recoveryOptions.toJson());
        return json;
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