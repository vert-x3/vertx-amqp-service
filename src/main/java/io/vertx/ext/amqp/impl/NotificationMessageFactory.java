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
package io.vertx.ext.amqp.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.AmqpService;
import io.vertx.ext.amqp.DeliveryState;
import io.vertx.ext.amqp.ErrorCode;
import io.vertx.ext.amqp.NotificationHelper;
import io.vertx.ext.amqp.NotificationHelper.NotificationType;

public class NotificationMessageFactory
{
    static JsonObject credit(String linkRef, int credits)
    {
        JsonObject json = new JsonObject();
        json.put(NotificationHelper.TYPE, NotificationType.LINK_CREDIT);
        json.put(NotificationHelper.LINK_REF, linkRef);
        json.put(NotificationHelper.LINK_CREDIT, credits);
        return json;
    }

    static JsonObject deliveryState(String msgRef, DeliveryState state, MessageDisposition disp)
    {
        JsonObject json = new JsonObject();
        json.put(NotificationHelper.TYPE, NotificationType.DELIVERY_STATE);
        json.put(AmqpService.OUTGOING_MSG_REF, msgRef);
        json.put(NotificationHelper.DELIVERY_STATE, state);
        json.put(NotificationHelper.MSG_STATE, disp);
        return json;
    }
    
    static JsonObject error(String linkRef, ErrorCode code, String msg)
    {
        JsonObject json = new JsonObject();
        json.put(NotificationHelper.TYPE, NotificationType.LINK_ERROR);
        json.put(NotificationHelper.LINK_REF, linkRef);
        json.put(NotificationHelper.ERROR_CODE, code);
        json.put(NotificationHelper.ERROR_MSG, msg);
        return json;
    }
}