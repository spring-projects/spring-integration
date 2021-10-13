/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.request_reply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@LongRunningTest
public class RequestReplyScenariosWithTempReplyQueuesTests extends ActiveMQMultiContextTests {

	private final Log logger = LogFactory.getLog(getClass());

	private final SimpleMessageConverter converter = new SimpleMessageConverter();

	@SuppressWarnings("resource")
	@Test
	public void messageCorrelationBasedOnRequestMessageId() {
		try (ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", this.getClass())) {

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
				catch (Exception ex) {
					fail("Test failed", ex);
				}
				jmsTemplate.send(replyTo,
						session -> {
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
			gateway.exchange(new GenericMessage<>("foo"));
		}
	}

	@Test
	public void messageCorrelationBasedOnRequestCorrelationIdTimedOutFirstReply() throws Exception {
		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		try (ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("producer-temp-reply-consumers.xml", getClass())) {

			RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
			ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

			Destination requestDestination = context.getBean("siOutQueue", Destination.class);

			dmlc.setConnectionFactory(connectionFactory);
			dmlc.setDestination(requestDestination);
			dmlc.setMessageListener((SessionAwareMessageListener<Message>) (message, session) -> {
				Destination replyTo = null;
				try {
					replyTo = message.getJMSReplyTo();
				}
				catch (Exception e1) {
					fail("Test failed", e1);
				}
				String requestPayload = (String) extractPayload(message);
				if (requestPayload.equals("foo")) {
					try {
						Thread.sleep(6000);
					}
					catch (Exception e2) {
						/*ignore*/
					}
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
				gateway.exchange(new GenericMessage<>("foo"));
			}
			catch (Exception e) {
				// ignore
			}
			Thread.sleep(1000);
			assertThat(gateway.exchange(new GenericMessage<>("bar")).getPayload()).isEqualTo("bar");
		}
		finally {
			dmlc.stop();
			dmlc.destroy();
		}
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
		broker.setTransportConnectorURIs(new String[]{ "tcp://localhost:61623" });
		broker.setDeleteAllMessagesOnStartup(true);
		broker.start();

		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("broken-broker.xml", this.getClass());

		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);

		int replyCounter = 0;
		int timeoutCounter = 0;
		for (int i = 0; i < 50; i++) {
			try {
				assertThat(gateway.exchange(new GenericMessage<>(String.valueOf(i))).getPayload())
						.isEqualTo(i + "");
				replyCounter++;
			}
			catch (Exception e) {
				timeoutCounter++;
			}
			if (i == 0 || i == 20 || i == 40) {
				Object replyDestination = TestUtils
						.getPropertyValue(context.getBean("jog"), "handler.replyDestination");
				if (replyDestination != null) {
					broker.removeDestination((ActiveMQDestination) replyDestination);
				}
			}
		}
		assertThat(replyCounter + timeoutCounter).isEqualTo(50);
		context.close();

		broker.stop();
	}

	@Test
	public void testConcurrently() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		try (ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("multi-producer-and-consumers-temp-reply.xml", this.getClass())) {

			final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
			final int testNumbers = 30;
			final CountDownLatch latch = new CountDownLatch(testNumbers);
			final AtomicInteger failures = new AtomicInteger();
			final AtomicInteger timeouts = new AtomicInteger();
			final AtomicInteger mismatches = new AtomicInteger();
			for (int i = 0; i < testNumbers; i++) {
				final int y = i;
				executor.execute(() -> {
					try {

						String reply = (String) gateway.exchange(new GenericMessage<>(String.valueOf(y)))
								.getPayload();
						if (!String.valueOf(y).equals(reply)) {
							mismatches.incrementAndGet();
						}
					}
					catch (Exception e) {
						if (e instanceof MessageDeliveryException) {
							timeouts.incrementAndGet();
						}
						else {
							logger.error("testConcurrently failure", e);
							failures.incrementAndGet();
						}
					}
					latch.countDown();
				});
			}
			assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
			print(failures, timeouts, mismatches, testNumbers);
			assertThat(mismatches.get()).isEqualTo(0);
			assertThat(failures.get()).isEqualTo(0);
			assertThat(timeouts.get()).isEqualTo(0);
		}
		finally {
			executor.shutdownNow();
		}
	}

	private void print(AtomicInteger failures, AtomicInteger timeouts, AtomicInteger mismatches,
			long exchangesProcessed) {

		logger.info("============================");
		logger.info(exchangesProcessed + " exchanges processed");
		logger.info("Failures: " + failures.get());
		logger.info("Timeouts: " + timeouts.get());
		logger.info("Missmatches: " + mismatches.get());
		logger.info("============================");
	}

	public static class MyRandomlySlowService {

		Random random = new Random();

		public String echo(String value) throws Exception {
			int i = random.nextInt(2000);
			Thread.sleep(i);
			return value;
		}

	}

	private Object extractPayload(Message jmsMessage) throws JMSException {
		return this.converter.fromMessage(jmsMessage);
	}

}
