/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

/**
 * Stub JMS Message implementation intended for testing purposes only.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class StubTextMessage implements TextMessage {

	private String messageId;

	private String text;

	private int deliveryMode = DEFAULT_DELIVERY_MODE;

	private Destination destination;

	private String correlationId;

	private Destination replyTo;

	private String type;

	private long timestamp = 0L;

	private long expiration = 0L;

	private int priority = DEFAULT_PRIORITY;

	private boolean redelivered;

	private ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<String, Object>();

	private long deliveryTime;

	public StubTextMessage() {
	}

	public StubTextMessage(String text) {
		this.text = text;
	}

	@Override
	public String getText() throws JMSException {
		return this.text;
	}

	@Override
	public void setText(String text) throws JMSException {
		this.text = text;
	}

	@Override
	public void acknowledge() throws JMSException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBody() throws JMSException {
		this.text = null;
	}

	@Override
	public void clearProperties() throws JMSException {
		this.properties.clear();
	}

	@Override
	public boolean getBooleanProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Boolean) && (Boolean) value;
	}

	@Override
	public byte getByteProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Byte) ? (Byte) value : 0;
	}

	@Override
	public double getDoubleProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Double) ? (Double) value : 0;
	}

	@Override
	public float getFloatProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Float) ? (Float) value : 0;
	}

	@Override
	public int getIntProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Integer) ? (Integer) value : 0;
	}

	@Override
	public String getJMSCorrelationID() throws JMSException {
		return this.correlationId;
	}

	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		return this.correlationId.getBytes();
	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		return this.deliveryMode;
	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		return this.destination;
	}

	@Override
	public long getJMSExpiration() throws JMSException {
		return this.expiration;
	}

	@Override
	public String getJMSMessageID() throws JMSException {
		return this.messageId;
	}

	@Override
	public int getJMSPriority() throws JMSException {
		return this.priority;
	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		return this.redelivered;
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		return this.replyTo;
	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		return this.timestamp;
	}

	@Override
	public String getJMSType() throws JMSException {
		return this.type;
	}

	@Override
	public long getLongProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Long) ? (Long) value : 0;
	}

	@Override
	public Object getObjectProperty(String name) throws JMSException {
		return this.properties.get(name);
	}

	@Override
	public Enumeration<?> getPropertyNames() throws JMSException {
		return this.properties.keys();
	}

	@Override
	public short getShortProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof Short) ? (Short) value : 0;
	}

	@Override
	public String getStringProperty(String name) throws JMSException {
		Object value = this.properties.get(name);
		return (value instanceof String) ? (String) value : null;
	}

	@Override
	public boolean propertyExists(String name) throws JMSException {
		return this.properties.containsKey(name);
	}

	@Override
	public void setBooleanProperty(String name, boolean value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setByteProperty(String name, byte value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setDoubleProperty(String name, double value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setFloatProperty(String name, float value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setIntProperty(String name, int value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setJMSCorrelationID(String correlationId) throws JMSException {
		this.correlationId = correlationId;
	}

	@Override
	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
		this.correlationId = new String(correlationID);
	}

	@Override
	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
		this.deliveryMode = deliveryMode;
	}

	@Override
	public void setJMSDestination(Destination destination) throws JMSException {
		this.destination = destination;
	}

	@Override
	public void setJMSExpiration(long expiration) throws JMSException {
		this.expiration = expiration;
	}

	@Override
	public void setJMSMessageID(String id) throws JMSException {
		this.messageId = id;
	}

	@Override
	public void setJMSPriority(int priority) throws JMSException {
		this.priority = priority;
	}

	@Override
	public void setJMSRedelivered(boolean redelivered) throws JMSException {
		this.redelivered = redelivered;
	}

	@Override
	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		this.replyTo = replyTo;
	}

	@Override
	public void setJMSTimestamp(long timestamp) throws JMSException {
		this.timestamp = timestamp;
	}

	@Override
	public void setJMSType(String type) throws JMSException {
		this.type = type;
	}

	@Override
	public void setLongProperty(String name, long value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setObjectProperty(String name, Object value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setShortProperty(String name, short value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public void setStringProperty(String name, String value) throws JMSException {
		this.properties.put(name, value);
	}

	@Override
	public long getJMSDeliveryTime() throws JMSException {
		return this.deliveryTime;
	}

	@Override
	public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
		this.deliveryTime = deliveryTime;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBody(Class<T> c) throws JMSException {
		return (T) this.text;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public boolean isBodyAssignableTo(Class c) throws JMSException {
		return c.isAssignableFrom(String.class);
	}

}
