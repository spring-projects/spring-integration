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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
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
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", this.getClass());
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
		context.close();
	}

	/**
	 * Validates that JOG will recreate a temporary queue
	 * once a failure detected and that the messages will still be properly correlated
	 */
	@Test
	public void brokenBrokerTest() throws Exception{

		BrokerService broker = new BrokerService();
		broker.setPersistent(false);
		broker.setUseJmx(false);
		broker.setTransportConnectorURIs(new String[]{"tcp://localhost:61623"});
		broker.setDeleteAllMessagesOnStartup(true);
		broker.start();

		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("broken-broker.xml", this.getClass());

		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);

		int replyCounter = 0;
		int timeoutCounter = 0;
		for (int i = 0; i < 50; i++) {
			try {
				assertEquals(i+"", gateway.exchange(new GenericMessage<String>(String.valueOf(i))).getPayload());
				replyCounter++;
			} catch (Exception e) {
				timeoutCounter++;
			}
			if (i == 0 || i == 20 || i == 40){
				Object replyDestination = TestUtils.getPropertyValue(context.getBean("jog"), "handler.replyDestination");
				if (replyDestination != null){
					broker.removeDestination((ActiveMQDestination) replyDestination);
				}
			}
		}
		assertEquals(50, replyCounter + timeoutCounter);
	}

	@Test
	public void testConcurrently() throws Exception{
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("mult-producer-and-consumers-temp-reply.xml", this.getClass());
		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		Executor executor = Executors.newFixedThreadPool(10);
		final int testNumbers = 100;
		final CountDownLatch latch = new CountDownLatch(testNumbers);
		final AtomicInteger failures = new AtomicInteger();
		final AtomicInteger timeouts = new AtomicInteger();
		final AtomicInteger missmatches = new AtomicInteger();
		for (int i = 0; i < testNumbers; i++) {
			final int y = i;
			executor.execute(new Runnable() {
				public void run() {
					try {

						String reply = (String) gateway.exchange(new GenericMessage<String>(String.valueOf(y))).getPayload();
						if (!String.valueOf(y).equals(reply)){
							missmatches.incrementAndGet();
						}
					} catch (Exception e) {
						if (e instanceof MessageDeliveryException) {
							timeouts.incrementAndGet();
						}
						else {
							failures.incrementAndGet();
						}
					}
//					if (latch.getCount()%100 == 0){
//						long count = testNumbers-latch.getCount();
//						if (count > 0){
//							print(failures, timeouts, missmatches, testNumbers-latch.getCount());
//						}
//					}
					latch.countDown();
				}
			});
		}
		latch.await();
		print(failures, timeouts, missmatches, testNumbers);
		Thread.sleep(5000);
		assertEquals(0, missmatches.get());
		assertEquals(0, failures.get());
		assertEquals(0, timeouts.get());
	}

	private void print(AtomicInteger failures, AtomicInteger timeouts, AtomicInteger missmatches, long echangesProcessed){
		System.out.println("============================");
		System.out.println(echangesProcessed + " exchanges processed");
		System.out.println("Failures: " + failures.get());
		System.out.println("Timeouts: " + timeouts.get());
		System.out.println("Missmatches: " + missmatches.get());
		System.out.println("============================");
	}

	public static class MyRandomlySlowService{
		Random random = new Random();
		List<Integer> list = new ArrayList<Integer>();
		public String secho(String value) throws Exception{
			int i = random.nextInt(2000);
//			if (i >= 2000){
//				System.out.println("SLEEPIING: " + i);
//			}
			Thread.sleep(i);
			return value;
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
