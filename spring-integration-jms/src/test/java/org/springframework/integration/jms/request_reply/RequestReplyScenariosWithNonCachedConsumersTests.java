/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jms.request_reply;

import static org.junit.Assert.assertEquals;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
/**
 * @author Oleg Zhurakousky
 */
public class RequestReplyScenariosWithNonCachedConsumersTests {

	@Test(expected=MessageTimeoutException.class)
	public void messageCorrelationBasedOnRequestMessageIdOptimized() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext("producer-no-cached-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("optimizedMessageId", RequestReplyExchanger.class);
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

		final Destination requestDestination = context.getBean("siOutQueueC", Destination.class);
		final Destination replyDestination = context.getBean("siInQueueC", Destination.class);
		new Thread(new Runnable() {

			public void run() {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, new MessageCreator() {

					public Message createMessage(Session session) throws JMSException {
						TextMessage message = session.createTextMessage();
						message.setText("bar");
						message.setJMSCorrelationID(requestMessage.getJMSMessageID());
						return message;
					}
				});
			}
		}).start();
		org.springframework.integration.Message<?> siReplyMessage = gateway.exchange(new GenericMessage<String>("foo"));
		assertEquals("bar", siReplyMessage.getPayload());
	}

	@Test
	public void messageCorrelationBasedOnRequestMessageIdNonOptimized() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext("producer-no-cached-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("nonoptimizedMessageId", RequestReplyExchanger.class);
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

		final Destination requestDestination = context.getBean("siOutQueueD", Destination.class);
		final Destination replyDestination = context.getBean("siInQueueD", Destination.class);
		new Thread(new Runnable() {

			public void run() {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, new MessageCreator() {

					public Message createMessage(Session session) throws JMSException {
						TextMessage message = session.createTextMessage();
						message.setText("bar");
						message.setJMSCorrelationID(requestMessage.getJMSMessageID());
						return message;
					}
				});
			}
		}).start();
		org.springframework.integration.Message<?> siReplyMessage = gateway.exchange(new GenericMessage<String>("foo"));
		assertEquals("bar", siReplyMessage.getPayload());
	}

	@Test
	public void messageCorrelationBasedOnRequestCorrelationIdOptimized() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext("producer-no-cached-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("optimized", RequestReplyExchanger.class);
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

		final Destination requestDestination = context.getBean("siOutQueueA", Destination.class);
		final Destination replyDestination = context.getBean("siInQueueA", Destination.class);
		new Thread(new Runnable() {

			public void run() {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, new MessageCreator() {

					public Message createMessage(Session session) throws JMSException {
						TextMessage message = session.createTextMessage();
						message.setText("bar");
						message.setJMSCorrelationID(requestMessage.getJMSCorrelationID());
						return message;
					}
				});
			}
		}).start();
		org.springframework.integration.Message<?> siReplyMessage = gateway.exchange(new GenericMessage<String>("foo"));
		assertEquals("bar", siReplyMessage.getPayload());
	}

	@Test(expected=MessageTimeoutException.class)
	public void messageCorrelationBasedOnRequestCorrelationIdNonOptimized() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext("producer-no-cached-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("nonoptimized", RequestReplyExchanger.class);
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

		final Destination requestDestination = context.getBean("siOutQueueB", Destination.class);
		final Destination replyDestination = context.getBean("siInQueueB", Destination.class);
		new Thread(new Runnable() {

			public void run() {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				jmsTemplate.send(replyDestination, new MessageCreator() {

					public Message createMessage(Session session) throws JMSException {
						TextMessage message = session.createTextMessage();
						message.setText("bar");
						message.setJMSCorrelationID(requestMessage.getJMSCorrelationID());
						return message;
					}
				});
			}
		}).start();
		org.springframework.integration.Message<?> siReplyMessage = gateway.exchange(new GenericMessage<String>("foo"));
		assertEquals("bar", siReplyMessage.getPayload());
	}
}
