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
package io.vertx.ext.amqp.impl.protocol;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.DeliveryState;

class JsonHelper
{
    public static Tracker jsonToTracker(JsonObject in)
    {
        TrackerImpl tracker = new TrackerImpl(null);
        if (in.containsKey("disposition"))
        {
            tracker.setDisposition(MessageDisposition.valueOf(in.getString("disposition").toUpperCase()));
        }
        if (in.containsKey("state"))
        {
            DeliveryState state = DeliveryState.valueOf(in.getString("state").toUpperCase());
            if (state == DeliveryState.SETTLED)
            {
                tracker.markSettled();
            }
            else if (state == DeliveryState.LINK_FAILED)
            {
                tracker.markLinkFailed();
            }
        }
        return tracker;
    }

    public static JsonObject trackerToJson(Tracker t)
    {
        JsonObject out = new JsonObject();
        out.put("disposition", t.getDisposition().toString());
        out.put("state", t.getState().toString());
        return out;
    }
}