package org.springframework.integration.adapter.jms;

import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

public class StubTextMessage implements TextMessage {

	private String text;

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
		return null;
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
		return null;
	}

	public long getJMSTimestamp() throws JMSException {
		return 0;
	}

	public String getJMSType() throws JMSException {
		return null;
	}

	public long getLongProperty(String name) throws JMSException {
		return 0;
	}

	public Object getObjectProperty(String name) throws JMSException {
		return null;
	}

	public Enumeration getPropertyNames() throws JMSException {
		return null;
	}

	public short getShortProperty(String name) throws JMSException {
		return 0;
	}

	public String getStringProperty(String name) throws JMSException {
		return null;
	}

	public boolean propertyExists(String name) throws JMSException {
		return false;
	}

	public void setBooleanProperty(String name, boolean value) throws JMSException {
	}

	public void setByteProperty(String name, byte value) throws JMSException {
	}

	public void setDoubleProperty(String name, double value) throws JMSException {
	}

	public void setFloatProperty(String name, float value) throws JMSException {
	}

	public void setIntProperty(String name, int value) throws JMSException {
	}

	public void setJMSCorrelationID(String correlationID) throws JMSException {
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
	}

	public void setJMSTimestamp(long timestamp) throws JMSException {
	}

	public void setJMSType(String type) throws JMSException {
	}

	public void setLongProperty(String name, long value) throws JMSException {
	}

	public void setObjectProperty(String name, Object value) throws JMSException {
	}

	public void setShortProperty(String name, short value) throws JMSException {
	}

	public void setStringProperty(String name, String value) throws JMSException {
	}

}
