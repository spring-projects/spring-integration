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
import static org.junit.Assert.fail;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.SimpleMessageConverter;
/**
 * @author Oleg Zhurakousky
 */
public class RequestReplyScenariosWithTempReplyQueuesTests {

	private final SimpleMessageConverter converter = new SimpleMessageConverter();

	@Test
	public void messageCorrelationBasedOnRequestMessageId() throws Exception{
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);


		final Destination requestDestination = context.getBean("siOutQueue", Destination.class);

		new Thread(new Runnable() {

			public void run() {
				final Message requestMessage = jmsTemplate.receive(requestDestination);
				Destination replyTo = null;
				try {
					replyTo = requestMessage.getJMSReplyTo();
				} catch (Exception e) {
					fail();
				}
				jmsTemplate.send(replyTo, new MessageCreator() {

					public Message createMessage(Session session) throws JMSException {
						try {
							TextMessage message = session.createTextMessage();
							message.setText("bar");
							message.setJMSCorrelationID(requestMessage.getJMSMessageID());
							return message;
						} catch (Exception e) {
							// ignore
						}
						return null;
					}
				});
			}
		}).start();
		gateway.exchange(new GenericMessage<String>("foo"));
		context.close();
	}

	@Test
	public void messageCorrelationBasedOnRequestCorrelationIdTimedOutFirstReply() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

		final Destination requestDestination = context.getBean("siOutQueue", Destination.class);

		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		dmlc.setConnectionFactory(connectionFactory);
		dmlc.setDestination(requestDestination);
		dmlc.setMessageListener(new SessionAwareMessageListener<Message>() {

			public void onMessage(Message message, Session session) {
				Destination replyTo = null;
				try {
					replyTo = message.getJMSReplyTo();
				} catch (Exception e) {
					fail();
				}
				String requestPayload = (String) extractPayload(message);
				if (requestPayload.equals("foo")){
					try {
						Thread.sleep(6000);
					} catch (Exception e) {/*ignore*/}
				}
				try {
					TextMessage replyMessage = session.createTextMessage();
					replyMessage.setText(requestPayload);
					replyMessage.setJMSCorrelationID(message.getJMSMessageID());
					MessageProducer producer = session.createProducer(replyTo);
					producer.send(replyMessage);
				} catch (Exception e) {
					// ignore. the test will fail
				}
			}
		});
		dmlc.afterPropertiesSet();
		dmlc.start();

		try {
			gateway.exchange(new GenericMessage<String>("foo"));
		} catch (Exception e) {
			// ignore
		}
		Thread.sleep(1000);
		try {
			assertEquals("bar", gateway.exchange(new GenericMessage<String>("bar")).getPayload());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	private Object extractPayload(Message jmsMessage) {
		try {
			return converter.fromMessage(jmsMessage);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return null;
	}
}
