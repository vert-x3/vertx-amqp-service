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

public enum InboundRoutingPropertyType
{

    LINK_NAME, SUBJECT, MESSAGE_ID, CORRELATION_ID, ADDRESS, REPLY_TO, CUSTOM;

    public static InboundRoutingPropertyType get(String key)
    {
        if (key == null || key.trim().equals(""))
        {
            return ADDRESS;
        }

        try
        {
            return InboundRoutingPropertyType.valueOf(key);
        }
        catch (IllegalArgumentException e)
        {
            return CUSTOM;
        }
    }
}