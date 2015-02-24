/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.vertx.ext.amqp;

import java.util.Map;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.DeliveryAnnotations;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;

@SuppressWarnings("rawtypes")
public class AmqpMessage
{
    private org.apache.qpid.proton.message.Message _msg;

    private Object _content;

    AmqpMessage()
    {
        _msg = Proton.message();
    }

    AmqpMessage(org.apache.qpid.proton.message.Message msg)
    {
        _msg = msg;
    }

    org.apache.qpid.proton.message.Message getProtocolMessage()
    {
        return _msg;
    }

    public boolean isDurable()
    {
        return _msg.isDurable();
    }

    public long getDeliveryCount()
    {
        return _msg.getDeliveryCount();
    }

    public short getPriority()
    {
        return _msg.getPriority();
    }

    public boolean isFirstAcquirer()
    {
        return _msg.isFirstAcquirer();
    }

    public long getTtl()
    {
        return _msg.getTtl();
    }

    public Object getMessageId()
    {
        return _msg.getMessageId();
    }

    public long getGroupSequence()
    {
        return _msg.getGroupSequence();
    }

    public String getReplyToGroupId()
    {
        return _msg.getReplyToGroupId();
    }

    public long getCreationTime()
    {
        return _msg.getCreationTime();
    }

    public String getAddress()
    {
        return _msg.getAddress();
    }

    public byte[] getUserId()
    {
        return _msg.getUserId();
    }

    public String getReplyTo()
    {
        return _msg.getReplyTo();
    }

    public String getGroupId()
    {
        return _msg.getGroupId();
    }

    public String getContentType()
    {
        return _msg.getContentType();
    }

    public long getExpiryTime()
    {
        return _msg.getExpiryTime();
    }

    public Object getCorrelationId()
    {
        return _msg.getCorrelationId();
    }

    public String getContentEncoding()
    {
        return _msg.getContentEncoding();
    }

    public String getSubject()
    {
        return _msg.getSubject();
    }

    public Map getMessageAnnotations()
    {
        return _msg.getMessageAnnotations().getValue();
    }

    public Map getDeliveryAnnotations()
    {
        return _msg.getDeliveryAnnotations().getValue();
    }

    public Map getApplicationProperties()
    {
        return _msg.getApplicationProperties().getValue();
    }

    public Object getContent()
    {
        if (_content == null)
        {
            if (_msg.getBody() != null)
            {
                if (_msg.getBody() instanceof Data)
                {
                    _content = ((Data) _msg.getBody()).getValue().asByteBuffer();
                }
                else
                {
                    _content = ((AmqpValue) _msg.getBody()).getValue();
                }
            }
        }
        return _content;
    }

    public void setDurable(boolean durable)
    {
        _msg.setDurable(durable);
    }

    public void setTtl(long ttl)
    {
        _msg.setTtl(ttl);
    }

    public void setDeliveryCount(long deliveryCount)
    {
        _msg.setDeliveryCount(deliveryCount);
    }

    public void setFirstAcquirer(boolean firstAcquirer)
    {
        _msg.setFirstAcquirer(firstAcquirer);
    }

    public void setPriority(short priority)
    {
        _msg.setPriority(priority);
    }

    public void setGroupSequence(long groupSequence)
    {
        _msg.setGroupSequence(groupSequence);
    }

    public void setUserId(byte[] userId)
    {
        _msg.setUserId(userId);
    }

    public void setCreationTime(long creationTime)
    {
        _msg.setCreationTime(creationTime);
    }

    public void setSubject(String subject)
    {
        _msg.setSubject(subject);
    }

    public void setGroupId(String groupId)
    {
        _msg.setGroupId(groupId);
    }

    public void setAddress(String to)
    {
        _msg.setAddress(to);
    }

    public void setExpiryTime(long absoluteExpiryTime)
    {
        _msg.setExpiryTime(absoluteExpiryTime);
    }

    public void setReplyToGroupId(String replyToGroupId)
    {
        _msg.setReplyToGroupId(replyToGroupId);
    }

    public void setContentEncoding(String contentEncoding)
    {
        _msg.setContentEncoding(contentEncoding);
    }

    public void setContentType(String contentType)
    {
        _msg.setContentType(contentType);
    }

    public void setReplyTo(String replyTo)
    {
        _msg.setReplyTo(replyTo);
    }

    public void setCorrelationId(Object correlationId)
    {
        _msg.setCorrelationId(correlationId);
    }

    public void setMessageId(Object messageId)
    {
        _msg.setMessageId(messageId);
    }

    @SuppressWarnings("unchecked")
    public void setMessageAnnotations(Map map)
    {
        _msg.setMessageAnnotations(new MessageAnnotations(map));
    }

    @SuppressWarnings("unchecked")
    public void setDeliveryAnnotations(Map map)
    {
        _msg.setDeliveryAnnotations(new DeliveryAnnotations(map));
    }

    public void setApplicationProperties(Map map)
    {
        _msg.setApplicationProperties(new ApplicationProperties(map));
    }

    public void setContent(Object content)
    {
        if (content instanceof byte[])
        {
            _msg.setBody(new Data(new Binary((byte[]) content)));
        }
        else
        {
            _msg.setBody(new AmqpValue(content));
        }
    }
}
