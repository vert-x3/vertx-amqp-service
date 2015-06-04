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

import java.util.Map;

public interface AmqpMessage {
  public String getMsgRef();

  public boolean isDurable();

  public long getDeliveryCount();

  public short getPriority();

  public boolean isFirstAcquirer();

  public long getTtl();

  public Object getMessageId();

  public long getGroupSequence();

  public String getReplyToGroupId();

  public long getCreationTime();

  public String getAddress();

  public byte[] getUserId();

  public String getReplyTo();

  public String getGroupId();

  public String getContentType();

  public long getExpiryTime();

  public Object getCorrelationId();

  public String getContentEncoding();

  public String getSubject();

  public Map getMessageAnnotations();

  public Map getDeliveryAnnotations();

  public Map getApplicationProperties();

  public Object getContent();

  public void setDurable(boolean durable);

  public void setTtl(long ttl);

  public void setDeliveryCount(long deliveryCount);

  public void setFirstAcquirer(boolean firstAcquirer);

  public void setPriority(short priority);

  public void setGroupSequence(long groupSequence);

  public void setUserId(byte[] userId);

  public void setCreationTime(long creationTime);

  public void setSubject(String subject);

  public void setGroupId(String groupId);

  public void setAddress(String to);

  public void setExpiryTime(long absoluteExpiryTime);

  public void setReplyToGroupId(String replyToGroupId);

  public void setContentEncoding(String contentEncoding);

  public void setContentType(String contentType);

  public void setReplyTo(String replyTo);

  public void setCorrelationId(Object correlationId);

  public void setMessageId(Object messageId);

  public void setMessageAnnotations(Map map);

  public void setDeliveryAnnotations(Map map);

  public void setApplicationProperties(Map map);

  public void setContent(Object content);
}