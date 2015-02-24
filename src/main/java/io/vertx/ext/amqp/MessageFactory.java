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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

class MessageFactory
{
    private void convert(JsonObject in, Properties out)
    {
        if (in.containsKey("to"))
        {
            out.setTo(in.getString("to"));
        }
        if (in.containsKey("subject"))
        {
            out.setSubject(in.getString("subject"));
        }
        if (in.containsKey("reply_to"))
        {
            out.setReplyTo(in.getString("reply_to"));
        }
        if (in.containsKey("message_id"))
        {
            // TODO: handle other types (UUID and long)
            out.setMessageId(in.getString("message_id"));
        }
        if (in.containsKey("correlation_id"))
        {
            // TODO: handle other types (UUID and long)
            out.setCorrelationId(in.getString("correlation_id"));
        }
        // TODO: handle other fields
    }

    private void convert(Properties in, JsonObject out)
    {
        if (in.getTo() != null)
        {
            out.put("to", in.getTo());
        }
        if (in.getSubject() != null)
        {
            out.put("subject", in.getSubject());
        }
        if (in.getReplyTo() != null)
        {
            out.put("reply_to", in.getReplyTo());
        }
        if (in.getMessageId() != null)
        {
            out.put("message_id", in.getMessageId().toString());
        }
        if (in.getCorrelationId() != null)
        {
            out.put("correlation_id", in.getCorrelationId().toString());
        }
        // TODO: handle other fields
    }

    Message convert(JsonObject in) throws MessageFormatException
    {
        Message out = Message.Factory.create();

        if (in.containsKey("properties"))
        {
            out.setProperties(new Properties());
            convert(in.getJsonObject("properties"), out.getProperties());
        }

        if (in.containsKey("application_properties"))
        {
            out.setApplicationProperties(new ApplicationProperties(in.getJsonObject("application_properties").getMap()));
        }

        if (in.containsKey("body"))
        {
            String bodyType = in.getString("body_type");
            if (bodyType == null || bodyType.equals("value"))
            {
                Object o = in.getValue("body");
                if (o instanceof JsonObject)
                {
                    o = ((JsonObject) o).getMap();
                }
                else if (o instanceof JsonArray)
                {
                    o = ((JsonArray) o).getList();
                }
                out.setBody(new AmqpValue(o));
            }
            else if (bodyType.equals("data"))
            {
                out.setBody(new Data(new Binary(in.getBinary("body"))));
            }
            else if (bodyType.equals("sequence"))
            {
                out.setBody(new AmqpSequence(((JsonArray) in.getValue("body")).getList()));
            }
            else
            {
                throw new MessageFormatException("Unrecognised body type: " + bodyType);
            }
        }
        return out;
    }

    private static Object toJsonable(Object in)
    {
        if (in instanceof Number || in instanceof String)
        {
            return in;
        }
        else if (in instanceof Map)
        {
            JsonObject out = new JsonObject();
            for (Object o : ((Map) in).entrySet())
            {
                Map.Entry e = (Map.Entry) o;
                out.put((String) e.getKey(), toJsonable(e.getValue()));
            }
            return out;
        }
        else if (in instanceof List)
        {
            JsonArray out = new JsonArray();
            for (Object i : (List) in)
            {
                out.add(toJsonable(i));
            }
            return out;
        }
        else
        {
            System.out.println("Warning: can't convert object of type " + in.getClass() + " to JSON");
            return in.toString();
        }
    }

    JsonObject convert(Message in)
    {
        JsonObject out = new JsonObject();
        Properties p = in.getProperties();
        if (p != null)
        {
            JsonObject props = new JsonObject();
            convert(p, props);
            out.put("properties", props);
        }
        ApplicationProperties ap = in.getApplicationProperties();
        if (ap != null && ap.getValue() != null)
        {
            out.put("application_properties", new JsonObject(ap.getValue()));
        }
        Section body = in.getBody();
        if (body instanceof AmqpValue)
        {
            out.put("body", toJsonable(((AmqpValue) body).getValue()));
            out.put("body_type", "value");
        }
        else if (body instanceof Data)
        {
            out.put("body", ((Data) body).getValue().getArray());
            out.put("body_type", "data");
        }
        else if (body instanceof AmqpSequence)
        {
            out.put("body", new JsonArray(((AmqpSequence) body).getValue()));
            out.put("body_type", "sequence");
        }
        return out;
    }
}