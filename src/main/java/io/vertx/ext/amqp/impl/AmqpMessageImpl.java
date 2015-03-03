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

import io.vertx.ext.amqp.AmqpMessage;

import java.util.Map;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.DeliveryAnnotations;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;

@SuppressWarnings("rawtypes")
class AmqpMessageImpl implements AmqpMessage
{
    private org.apache.qpid.proton.message.Message _msg;

    private Object _content;

    AmqpMessageImpl()
    {
        _msg = Proton.message();
    }

    AmqpMessageImpl(org.apache.qpid.proton.message.Message msg)
    {
        _msg = msg;
    }

    org.apache.qpid.proton.message.Message getProtocolMessage()
    {
        return _msg;
    }

    @Override
    public String getMsgRef()
    {
        throw new UnsupportedOperationException("This operation is not supported for outgoing messages");
    }

    @Override
    public boolean isDurable()
    {
        return _msg.isDurable();
    }

    @Override
    public long getDeliveryCount()
    {
        return _msg.getDeliveryCount();
    }

    @Override
    public short getPriority()
    {
        return _msg.getPriority();
    }

    @Override
    public boolean isFirstAcquirer()
    {
        return _msg.isFirstAcquirer();
    }

    @Override
    public long getTtl()
    {
        return _msg.getTtl();
    }

    @Override
    public Object getMessageId()
    {
        return _msg.getMessageId();
    }

    @Override
    public long getGroupSequence()
    {
        return _msg.getGroupSequence();
    }

    @Override
    public String getReplyToGroupId()
    {
        return _msg.getReplyToGroupId();
    }

    @Override
    public long getCreationTime()
    {
        return _msg.getCreationTime();
    }

    @Override
    public String getAddress()
    {
        return _msg.getAddress();
    }

    @Override
    public byte[] getUserId()
    {
        return _msg.getUserId();
    }

    @Override
    public String getReplyTo()
    {
        return _msg.getReplyTo();
    }

    @Override
    public String getGroupId()
    {
        return _msg.getGroupId();
    }

    @Override
    public String getContentType()
    {
        return _msg.getContentType();
    }

    @Override
    public long getExpiryTime()
    {
        return _msg.getExpiryTime();
    }

    @Override
    public Object getCorrelationId()
    {
        return _msg.getCorrelationId();
    }

    @Override
    public String getContentEncoding()
    {
        return _msg.getContentEncoding();
    }

    @Override
    public String getSubject()
    {
        return _msg.getSubject();
    }

    @Override
    public Map getMessageAnnotations()
    {
        return _msg.getMessageAnnotations().getValue();
    }

    @Override
    public Map getDeliveryAnnotations()
    {
        return _msg.getDeliveryAnnotations().getValue();
    }

    @Override
    public Map getApplicationProperties()
    {
        return _msg.getApplicationProperties().getValue();
    }

    @Override
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

    @Override
    public void setDurable(boolean durable)
    {
        _msg.setDurable(durable);
    }

    @Override
    public void setTtl(long ttl)
    {
        _msg.setTtl(ttl);
    }

    @Override
    public void setDeliveryCount(long deliveryCount)
    {
        _msg.setDeliveryCount(deliveryCount);
    }

    @Override
    public void setFirstAcquirer(boolean firstAcquirer)
    {
        _msg.setFirstAcquirer(firstAcquirer);
    }

    @Override
    public void setPriority(short priority)
    {
        _msg.setPriority(priority);
    }

    @Override
    public void setGroupSequence(long groupSequence)
    {
        _msg.setGroupSequence(groupSequence);
    }

    @Override
    public void setUserId(byte[] userId)
    {
        _msg.setUserId(userId);
    }

    @Override
    public void setCreationTime(long creationTime)
    {
        _msg.setCreationTime(creationTime);
    }

    @Override
    public void setSubject(String subject)
    {
        _msg.setSubject(subject);
    }

    @Override
    public void setGroupId(String groupId)
    {
        _msg.setGroupId(groupId);
    }

    @Override
    public void setAddress(String to)
    {
        _msg.setAddress(to);
    }

    @Override
    public void setExpiryTime(long absoluteExpiryTime)
    {
        _msg.setExpiryTime(absoluteExpiryTime);
    }

    @Override
    public void setReplyToGroupId(String replyToGroupId)
    {
        _msg.setReplyToGroupId(replyToGroupId);
    }

    @Override
    public void setContentEncoding(String contentEncoding)
    {
        _msg.setContentEncoding(contentEncoding);
    }

    @Override
    public void setContentType(String contentType)
    {
        _msg.setContentType(contentType);
    }

    @Override
    public void setReplyTo(String replyTo)
    {
        _msg.setReplyTo(replyTo);
    }

    @Override
    public void setCorrelationId(Object correlationId)
    {
        _msg.setCorrelationId(correlationId);
    }

    @Override
    public void setMessageId(Object messageId)
    {
        _msg.setMessageId(messageId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setMessageAnnotations(Map map)
    {
        _msg.setMessageAnnotations(new MessageAnnotations(map));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setDeliveryAnnotations(Map map)
    {
        _msg.setDeliveryAnnotations(new DeliveryAnnotations(map));
    }

    @Override
    public void setApplicationProperties(Map map)
    {
        _msg.setApplicationProperties(new ApplicationProperties(map));
    }

    @Override
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