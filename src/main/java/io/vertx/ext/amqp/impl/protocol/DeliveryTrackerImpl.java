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
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.DeliveryState;
import io.vertx.ext.amqp.DeliveryTracker;
import io.vertx.ext.amqp.MessageState;
import io.vertx.ext.amqp.NotificationHelper;

public class DeliveryTrackerImpl implements DeliveryTracker
{
    String _ref;

    DeliveryState _deliveryState;

    MessageState _msgState;

    DeliveryTrackerImpl(String ref, DeliveryState deliveryState, MessageState msgState)
    {
        super();
        _ref = ref;
        _deliveryState = deliveryState;
        _msgState = msgState;
    }

    public static DeliveryTracker create(JsonObject json)
    {
        String[] keys = { AmqpService.OUTGOING_MSG_REF, NotificationHelper.DELIVERY_STATE, NotificationHelper.MSG_STATE };
        for (String key : keys)
        {
            if (!json.containsKey(key))
            {
                throw new IllegalArgumentException(
                        String.format("Malformed delivery-tracker message, '%s' missing", key));
            }
        }
        return new DeliveryTrackerImpl(json.getString(AmqpService.OUTGOING_MSG_REF), DeliveryState.valueOf(json
                .getString(NotificationHelper.DELIVERY_STATE)), MessageState.valueOf(json
                .getString(NotificationHelper.MSG_STATE)));
    }

    @Override
    public DeliveryState getDeliveryState()
    {
        return _deliveryState;
    }

    @Override
    public MessageState getMessageState()
    {
        return _msgState;
    }

    @Override
    public String getMessageRef()
    {
        return _ref;
    }
}
