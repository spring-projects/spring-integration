/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.jms;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

public class StubTextMessage implements TextMessage {

	private String text;

	private Destination replyTo;

	private String correlationID;

	private String type;

	private ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<String, Object>();


	public String getText() throws JMSException {
		return this.text;
	}

	public void setText(String text) throws JMSException {
		this.text = text;
	}

	public void acknowledge() throws JMSException {
	}

	public void clearBody() throws JMSException {
	}

	public void clearProperties() throws JMSException {
	}

	public boolean getBooleanProperty(String name) throws JMSException {
		return false;
	}

	public byte getByteProperty(String name) throws JMSException {
		return 0;
	}

	public double getDoubleProperty(String name) throws JMSException {
		return 0;
	}

	public float getFloatProperty(String name) throws JMSException {
		return 0;
	}

	public int getIntProperty(String name) throws JMSException {
		return 0;
	}

	public String getJMSCorrelationID() throws JMSException {
		return this.correlationID;
	}

	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		return null;
	}

	public int getJMSDeliveryMode() throws JMSException {
		return 0;
	}

	public Destination getJMSDestination() throws JMSException {
		return null;
	}

	public long getJMSExpiration() throws JMSException {
		return 0;
	}

	public String getJMSMessageID() throws JMSException {
		return null;
	}

	public int getJMSPriority() throws JMSException {
		return 0;
	}

	public boolean getJMSRedelivered() throws JMSException {
		return false;
	}

	public Destination getJMSReplyTo() throws JMSException {
		return this.replyTo;
	}

	public long getJMSTimestamp() throws JMSException {
		return 0;
	}

	public String getJMSType() throws JMSException {
		return this.type;
	}

	public long getLongProperty(String name) throws JMSException {
		return 0;
	}

	public Object getObjectProperty(String name) throws JMSException {
		return this.properties.get(name);
	}

	public Enumeration getPropertyNames() throws JMSException {
		return this.properties.keys();
	}

	public short getShortProperty(String name) throws JMSException {
		return 0;
	}

	public String getStringProperty(String name) throws JMSException {
		return null;
	}

	public boolean propertyExists(String name) throws JMSException {
		return this.properties.containsKey(name);
	}

	public void setBooleanProperty(String name, boolean value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setByteProperty(String name, byte value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setDoubleProperty(String name, double value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setFloatProperty(String name, float value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setIntProperty(String name, int value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setJMSCorrelationID(String correlationID) throws JMSException {
		this.correlationID = correlationID;
	}

	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
	}

	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
	}

	public void setJMSDestination(Destination destination) throws JMSException {
	}

	public void setJMSExpiration(long expiration) throws JMSException {
	}

	public void setJMSMessageID(String id) throws JMSException {
	}

	public void setJMSPriority(int priority) throws JMSException {
	}

	public void setJMSRedelivered(boolean redelivered) throws JMSException {
	}

	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		this.replyTo = replyTo;
	}

	public void setJMSTimestamp(long timestamp) throws JMSException {
	}

	public void setJMSType(String type) throws JMSException {
		this.type = type;
	}

	public void setLongProperty(String name, long value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setObjectProperty(String name, Object value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setShortProperty(String name, short value) throws JMSException {
		this.properties.put(name, value);
	}

	public void setStringProperty(String name, String value) throws JMSException {
		this.properties.put(name, value);
	}

}
