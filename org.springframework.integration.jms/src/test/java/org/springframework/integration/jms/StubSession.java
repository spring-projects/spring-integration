/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.jms;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * @author Mark Fisher
 */
public class StubSession implements Session {

	private final String messageText;


	public StubSession(String messageText) {
		this.messageText = messageText;
	}


	public void close() throws JMSException {
	}

	public void commit() throws JMSException {
	}

	public QueueBrowser createBrowser(Queue queue) throws JMSException {
		return null;
	}

	public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
		return null;
	}

	public BytesMessage createBytesMessage() throws JMSException {
		return null;
	}

	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		return new StubConsumer(this.messageText, null);
	}

	public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

	public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal)
			throws JMSException {
		return new StubConsumer(this.messageText, messageSelector);
	}

	public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
		return null;
	}

	public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal)
			throws JMSException {
		return null;
	}

	public MapMessage createMapMessage() throws JMSException {
		return null;
	}

	public Message createMessage() throws JMSException {
		return null;
	}

	public ObjectMessage createObjectMessage() throws JMSException {
		return null;
	}

	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		return null;
	}

	public MessageProducer createProducer(Destination destination) throws JMSException {
		return new StubProducer(destination);
	}

	public Queue createQueue(String queueName) throws JMSException {
		return null;
	}

	public StreamMessage createStreamMessage() throws JMSException {
		return null;
	}

	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return null;
	}

	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return null;
	}

	public TextMessage createTextMessage() throws JMSException {
		return null;
	}

	public TextMessage createTextMessage(String text) throws JMSException {
		return new StubTextMessage(text);
	}

	public Topic createTopic(String topicName) throws JMSException {
		return null;
	}

	public int getAcknowledgeMode() throws JMSException {
		return 0;
	}

	public MessageListener getMessageListener() throws JMSException {
		return null;
	}

	public boolean getTransacted() throws JMSException {
		return false;
	}

	public void recover() throws JMSException {
	}

	public void rollback() throws JMSException {
	}

	public void run() {
	}

	public void setMessageListener(MessageListener listener) throws JMSException {
	}

	public void unsubscribe(String name) throws JMSException {
	}

}
