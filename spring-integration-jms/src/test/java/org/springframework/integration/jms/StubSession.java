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

import java.io.Serializable;

import jakarta.jms.BytesMessage;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicSubscriber;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class StubSession implements Session {

	private final String messageText;

	public StubSession(String messageText) {
		this.messageText = messageText;
	}

	@Override
	public void close() throws JMSException {
	}

	@Override
	public void commit() throws JMSException {
	}

	@Override
	public QueueBrowser createBrowser(Queue queue) throws JMSException {
		return null;
	}

	@Override
	public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
		return null;
	}

	@Override
	public BytesMessage createBytesMessage() throws JMSException {
		return null;
	}

	@Override
	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		return new StubConsumer(this.messageText, null);
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal)
			throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
		return null;
	}

	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal)
			throws JMSException {
		return null;
	}

	@Override
	public MapMessage createMapMessage() throws JMSException {
		return null;
	}

	@Override
	public Message createMessage() throws JMSException {
		return null;
	}

	@Override
	public ObjectMessage createObjectMessage() throws JMSException {
		return null;
	}

	@Override
	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		return null;
	}

	@Override
	public MessageProducer createProducer(Destination destination) throws JMSException {
		return new StubProducer(destination);
	}

	@Override
	public Queue createQueue(String queueName) throws JMSException {
		return null;
	}

	@Override
	public StreamMessage createStreamMessage() throws JMSException {
		return null;
	}

	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return null;
	}

	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return null;
	}

	@Override
	public TextMessage createTextMessage() throws JMSException {
		return null;
	}

	@Override
	public TextMessage createTextMessage(String text) throws JMSException {
		return new StubTextMessage(text);
	}

	@Override
	public Topic createTopic(String topicName) throws JMSException {
		return null;
	}

	@Override
	public int getAcknowledgeMode() throws JMSException {
		return 0;
	}

	@Override
	public MessageListener getMessageListener() throws JMSException {
		return null;
	}

	@Override
	public boolean getTransacted() throws JMSException {
		return false;
	}

	@Override
	public void recover() throws JMSException {
	}

	@Override
	public void rollback() throws JMSException {
	}

	@Override
	public void run() {
	}

	@Override
	public void setMessageListener(MessageListener listener) throws JMSException {
	}

	@Override
	public void unsubscribe(String name) throws JMSException {
	}

	@Override
	public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException {
		return new StubConsumer(this.messageText, null);
	}

	@Override
	public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

	@Override
	public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
		return new StubConsumer(this.messageText, null);
	}

	@Override
	public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal)
			throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

	@Override
	public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
		return new StubConsumer(this.messageText, null);
	}

	@Override
	public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector)
			throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

}
