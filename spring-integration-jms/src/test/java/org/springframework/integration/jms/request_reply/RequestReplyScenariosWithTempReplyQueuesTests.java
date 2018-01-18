/*
 * Copyright 2002-2018 the original author or authors.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;
/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class RequestReplyScenariosWithTempReplyQueuesTests extends ActiveMQMultiContextTests {

	private final Log logger = LogFactory.getLog(getClass());

	private final SimpleMessageConverter converter = new SimpleMessageConverter();

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@SuppressWarnings("resource")
	@Test
	public void messageCorrelationBasedOnRequestMessageId() throws Exception {
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		CachingConnectionFactory connectionFactory = context.getBean(CachingConnectionFactory.class);
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);

		final Destination requestDestination = context.getBean("siOutQueue", Destination.class);

		new Thread(() -> {
			final Message requestMessage = jmsTemplate.receive(requestDestination);
			Destination replyTo = null;
			try {
				replyTo = requestMessage.getJMSReplyTo();
			}
			catch (Exception e) {
				fail();
			}
			jmsTemplate.send(replyTo, (MessageCreator) session -> {
				try {
					TextMessage message = session.createTextMessage();
					message.setText("bar");
					message.setJMSCorrelationID(requestMessage.getJMSMessageID());
					return message;
				}
				catch (Exception e) {
					// ignore
				}
				return null;
			});
		}).start();
		gateway.exchange(new GenericMessage<String>("foo"));
		context.close();
	}

	@Test
	public void messageCorrelationBasedOnRequestCorrelationIdTimedOutFirstReply() throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

		final Destination requestDestination = context.getBean("siOutQueue", Destination.class);

		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		dmlc.setConnectionFactory(connectionFactory);
		dmlc.setDestination(requestDestination);
		dmlc.setMessageListener((SessionAwareMessageListener<Message>) (message, session) -> {
			Destination replyTo = null;
			try {
				replyTo = message.getJMSReplyTo();
			}
			catch (Exception e1) {
				fail();
			}
			String requestPayload = (String) extractPayload(message);
			if (requestPayload.equals("foo")) {
				try {
					Thread.sleep(6000);
				}
				catch (Exception e2) { /*ignore*/ }
			}
			try {
				TextMessage replyMessage = session.createTextMessage();
				replyMessage.setText(requestPayload);
				replyMessage.setJMSCorrelationID(message.getJMSMessageID());
				MessageProducer producer = session.createProducer(replyTo);
				producer.send(replyMessage);
			}
			catch (Exception e3) {
				// ignore. the test will fail
			}
		});
		dmlc.afterPropertiesSet();
		dmlc.start();

		try {
			gateway.exchange(new GenericMessage<String>("foo"));
		}
		catch (Exception e) {
			// ignore
		}
		Thread.sleep(1000);
		try {
			assertEquals("bar", gateway.exchange(new GenericMessage<String>("bar")).getPayload());
		}
		catch (Exception e) {
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
	public void brokenBrokerTest() throws Exception {

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
				assertEquals(i + "", gateway.exchange(new GenericMessage<String>(String.valueOf(i))).getPayload());
				replyCounter++;
			}
			catch (Exception e) {
				timeoutCounter++;
			}
			if (i == 0 || i == 20 || i == 40) {
				Object replyDestination = TestUtils.getPropertyValue(context.getBean("jog"), "handler.replyDestination");
				if (replyDestination != null) {
					broker.removeDestination((ActiveMQDestination) replyDestination);
				}
			}
		}
		assertEquals(50, replyCounter + timeoutCounter);
		context.close();
	}

	@Test
	public void testConcurrently() throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("mult-producer-and-consumers-temp-reply.xml", this.getClass());
		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		ExecutorService executor = Executors.newFixedThreadPool(10);
		final int testNumbers = 30;
		final CountDownLatch latch = new CountDownLatch(testNumbers);
		final AtomicInteger failures = new AtomicInteger();
		final AtomicInteger timeouts = new AtomicInteger();
		final AtomicInteger missmatches = new AtomicInteger();
		for (int i = 0; i < testNumbers; i++) {
			final int y = i;
			executor.execute(() -> {
				try {

					String reply = (String) gateway.exchange(new GenericMessage<String>(String.valueOf(y))).getPayload();
					if (!String.valueOf(y).equals(reply)) {
						missmatches.incrementAndGet();
					}
				}
				catch (Exception e) {
					if (e instanceof MessageDeliveryException) {
						timeouts.incrementAndGet();
					}
					else {
						failures.incrementAndGet();
					}
				}
				latch.countDown();
			});
		}
		assertTrue(latch.await(30, TimeUnit.SECONDS));
		print(failures, timeouts, missmatches, testNumbers);
		assertEquals(0, missmatches.get());
		assertEquals(0, failures.get());
		assertEquals(0, timeouts.get());
		context.close();
		executor.shutdownNow();
	}

	private void print(AtomicInteger failures, AtomicInteger timeouts, AtomicInteger missmatches, long echangesProcessed) {
		logger.info("============================");
		logger.info(echangesProcessed + " exchanges processed");
		logger.info("Failures: " + failures.get());
		logger.info("Timeouts: " + timeouts.get());
		logger.info("Missmatches: " + missmatches.get());
		logger.info("============================");
	}

	public static class MyRandomlySlowService {
		Random random = new Random();
		List<Integer> list = new ArrayList<Integer>();
		public String secho(String value) throws Exception {
			int i = random.nextInt(2000);
			Thread.sleep(i);
			return value;
		}
	}

	private Object extractPayload(Message jmsMessage) {
		try {
			return converter.fromMessage(jmsMessage);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return null;
	}
}
