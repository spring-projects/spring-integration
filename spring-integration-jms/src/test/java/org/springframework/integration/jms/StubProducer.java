/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.jms.CompletionListener;
import jakarta.jms.Destination;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;

/**
 * @author Mark Fisher
 */
public class StubProducer implements MessageProducer {

	private Message lastMessageSent;

	private Destination staticDestination;

	private long deliveryDelay;

	public StubProducer(Destination destination) {
		this.staticDestination = destination;
	}

	public Message getLastMessageSent() {
		return this.lastMessageSent;
	}

	public void close() throws JMSException {
	}

	public int getDeliveryMode() throws JMSException {
		return 0;
	}

	public Destination getDestination() throws JMSException {
		return null;
	}

	public boolean getDisableMessageID() throws JMSException {
		return false;
	}

	public boolean getDisableMessageTimestamp() throws JMSException {
		return false;
	}

	public int getPriority() throws JMSException {
		return 0;
	}

	public long getTimeToLive() throws JMSException {
		return 0;
	}

	public void send(Message message) throws JMSException {
		if (this.staticDestination == null) {
			throw new UnsupportedOperationException("producer was not created with a static Destination");
		}
		this.lastMessageSent = message;
	}

	public void send(Destination destination, Message message) throws JMSException {
		if (this.staticDestination != null) {
			throw new UnsupportedOperationException("producer was created with a static Destination");
		}
		if (destination == null) {
			throw new InvalidDestinationException("null destination");
		}
		this.lastMessageSent = message;
	}

	public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		if (this.staticDestination == null) {
			throw new UnsupportedOperationException("producer was not created with a static Destination");
		}
		this.lastMessageSent = message;
	}

	public void send(Destination destination, Message message,
			int deliveryMode, int priority, long timeToLive) throws JMSException {
		if (this.staticDestination != null) {
			throw new UnsupportedOperationException("producer was created with a static Destination");
		}
		if (destination == null) {
			throw new InvalidDestinationException("null destination");
		}
		this.lastMessageSent = message;
	}

	public void setDeliveryMode(int deliveryMode) throws JMSException {
	}

	public void setDisableMessageID(boolean value) throws JMSException {
	}

	public void setDisableMessageTimestamp(boolean value) throws JMSException {
	}

	public void setPriority(int defaultPriority) throws JMSException {
	}

	public void setTimeToLive(long timeToLive) throws JMSException {
	}

	@Override
	public void setDeliveryDelay(long deliveryDelay) throws JMSException {
		this.deliveryDelay = deliveryDelay;
	}

	@Override
	public long getDeliveryDelay() throws JMSException {
		return this.deliveryDelay;
	}

	@Override
	public void send(Message message, CompletionListener completionListener) throws JMSException {
		send(message);
	}

	@Override
	public void send(Message message, int deliveryMode, int priority, long timeToLive,
			CompletionListener completionListener) throws JMSException {
		send(message);
	}

	@Override
	public void send(Destination destination, Message message, CompletionListener completionListener)
			throws JMSException {
		send(destination, message);
	}

	@Override
	public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
			CompletionListener completionListener) throws JMSException {
		send(destination, message);
	}

}
