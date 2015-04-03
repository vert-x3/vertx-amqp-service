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
    public final static String INITIAL_CAPACITY = "initial-capacity";

    private int initialCapacity = 0;

    public ServiceOptions()
    {
    }

    public ServiceOptions(ServiceOptions options)
    {
        this.initialCapacity = options.initialCapacity;
    }

    public ServiceOptions(JsonObject options)
    {
        this.initialCapacity = options.getInteger(INITIAL_CAPACITY, 1);
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.put(INITIAL_CAPACITY, initialCapacity);
        return json;
    }

    public int getInitialCapacity()
    {
        return initialCapacity;
    }

    public void setInitialCapacity(int capacity)
    {
        this.initialCapacity = capacity;
    }

    @Override
    public String toString()
    {
        return toJson().encode();
    }
}