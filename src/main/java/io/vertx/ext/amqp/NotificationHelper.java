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

import io.vertx.core.json.JsonObject;
import io.vertx.ext.amqp.impl.DeliveryTrackerImpl;

/**
 * A helper class for parsing notification messages. This allows applications to
 * not worry about the message format and instead deal with enums and java types
 * instead of json.
 * 
 * See {@link PublishToQueueVerticle} and {@link FortuneCookieServiceVerticle}
 * in the examples to see how this is used.
 * 
 * @author <a href="mailto:rajith@redhat.com">Rajith Attapattu</a>
 */
public class NotificationHelper
{
    public enum NotificationType
    {
        DELIVERY_STATE, LINK_CREDIT, LINK_ERROR;
    };

    public final static String TYPE = "vertx.amqp.notification-type";

    public final static String LINK_REF = "vertx.amqp.link-ref";

    public final static String LINK_CREDIT = "vertx.amqp.link-credit";

    public final static String DELIVERY_STATE = "vertx.amqp.delivery-state";

    public final static String MSG_STATE = "vertx.amqp.msg-state";

    public final static String ERROR_CODE = "vertx.amqp.error-code";

    public final static String ERROR_MSG = "vertx.amqp.error-msg";

    public static NotificationType getType(JsonObject json)
    {
        if (json != null && json.containsKey(TYPE))
        {
            return NotificationType.valueOf(json.getString(TYPE));
        }
        else
        {
            throw new IllegalArgumentException("Malfored notification message. 'vertx.amqp.notification-type' missing");
        }
    }

    public static ErrorCode getErrorCode(JsonObject json)
    {
        if (json != null && json.containsKey(ERROR_CODE))
        {
            return ErrorCode.valueOf(json.getString(ERROR_CODE));
        }
        else
        {
            throw new IllegalArgumentException("Malfored error message, 'vertx.amqp.error-code' missing");
        }
    }

    public static String getLinkRef(JsonObject json)
    {
        if (json != null && json.containsKey(LINK_REF))
        {
            return json.getString(LINK_REF);
        }
        else
        {
            throw new IllegalArgumentException(String.format(
                    "Malfored notification message, '%s' missing, but expected", LINK_REF));
        }
    }

    public static int getCredits(JsonObject json)
    {
        if (json != null && json.containsKey(LINK_CREDIT))
        {
            return json.getInteger(LINK_CREDIT);
        }
        else
        {
            throw new IllegalArgumentException(String.format("Malfored credit notification message, '%s' missing",
                    LINK_CREDIT));
        }
    }

    public static DeliveryTracker getDeliveryTracker(JsonObject json)
    {
        return DeliveryTrackerImpl.create(json);
    }
}