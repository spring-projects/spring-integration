/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.impl.NatsJetStreamMetaData;
import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test resilience of synchronous message producer using NATS DSL configuration outbound
 * adapter
 *
 * <p>Integration test cases to test NATS spring components communication with docker/devlocal NATS
 * server.
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
		classes = {
				NatsTestConfig.class,
				NatsSynchronousMessageProducerResilienceTest.ContextConfig.class
		})
public class NatsSynchronousMessageProducerResilienceTest
		extends AbstractNatsIntegrationTestSupport {

	private static final Log LOG =
			LogFactory.getLog(NatsSynchronousMessageProducerResilienceTest.class);

	private static final String TEST_SUBJECT = "test-subject";

	private static final String TEST_STREAM = "test-stream";

	private static final String TEST_SUBJECT_CONSUMER = "test-subject-consumer";

	public static HashMap<String, Throwable> errorMessages = new HashMap<>();

	public static HashMap<String, NatsJetStreamMetaData> consumedMessages = new HashMap<>();

	@Autowired
	private Connection natsConnection;

	@Autowired
	@Qualifier("producerChannelServerIssue")
	private DirectChannel producerChannelServerIssue;

	@Autowired
	private ApplicationContext appContext;

	/**
	 * Tests negative flow using outbound(message producer).
	 *
	 * <p>Test scenario: Send some messages while NATS server is unavailable.
	 *
	 * <p>Result Expected: these messages will be retried as per the spring retry configuration. Once
	 * the retries are exhausted, verify if the messages sent during the server unavailability is sent
	 * to errorChannel for logging
	 *
	 * <p>Bean Context for this test defined below in Producer Flow Bean: {@link
	 * NatsSynchronousMessageProducerResilienceTest.ContextConfig#outboundFlow()}
	 *
	 * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
	 *                              status of the current thread is cleared when this exception is thrown.
	 */
	@Test
	public void testProducerResiliency() throws InterruptedException {
		// Send 10 messages - positive flow - message sent successfully
		sendBunchMessages(0, 10);
		// stop Nats server to simulate server unavailability
		stopNatsServerInLocal();
		// send some messages while NATS server is unavailable - these messages will be retried as per
		// the configuration
		sendBunchMessages(10, 20);
		// start Nats server again
		startNatsServerInLocal();
		// send some more messages to check if the messages are sent - producer flow should not block
		sendBunchMessages(20, 30);

		// wait for messages arrivals
		Thread.sleep(10000);

		// Verify if the messages sent during the server unavailability is sent to errorChannel for
		// processing
		// these messages are captured in a Hashmap in errorChannel Service activator for verification.
		for (int i = 10; i < 20; i++) {
			String payload = "Hello" + i;
			Throwable error = errorMessages.get(payload);
			Assert.assertNotNull(error);
			Assert.assertTrue(
					error.getMessage().contains("Timeout or no response waiting for NATS JetStream server"));
		}
		// Verify if all other messages are sent to the consumer channel
		for (int i = 0; i < 10; i++) {
			String payload = "Hello" + i;
			Assert.assertFalse(errorMessages.containsKey(payload));
			Assert.assertTrue(consumedMessages.containsKey(payload));
		}
		for (int i = 20; i < 30; i++) {
			String payload = "Hello" + i;
			Assert.assertFalse(errorMessages.containsKey(payload));
			Assert.assertTrue(consumedMessages.containsKey(payload));
		}
	}

	/*
	 * Method to sent list of messages
	 * */
	private void sendBunchMessages(int from, int to) {
		for (int i = from; i < to; i++) {
			String payload = "Hello" + i;
			MessageBuilder<String> builder = MessageBuilder.withPayload(payload);
			LOG.debug("Sending Message to Channel => " + payload);
			Assert.assertTrue(
					this.producerChannelServerIssue.send(MessageBuilder.withPayload(payload).build()));
		}
	}

	/**
	 * Configuration class to setup Beans required to initialize NATS Message producers using NATS dsl
	 * classes
	 */
	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	public static class ContextConfig {

		@Autowired
		private Connection natsConnection;

		@PostConstruct
		public void streamSetup() throws IOException, JetStreamApiException {
			createStreamConfig(this.natsConnection, TEST_STREAM, TEST_SUBJECT);
			// Consumer Config to limit the redelivery
			ConsumerConfiguration consumerConfiguration =
					ConsumerConfiguration.builder()
							.ackWait(Duration.ofSeconds(5))
							.maxDeliver(1)
							.durable(TEST_SUBJECT_CONSUMER)
							.build();
			createConsumer(this.natsConnection, TEST_STREAM, consumerConfiguration);
		}

		@Bean
		public MessageChannel producerChannelServerIssue() {
			return MessageChannels.direct().getObject();
		}

		@Bean
		public NatsTemplate natsTemplate() {
			return new NatsTemplate(this.natsConnection, TEST_SUBJECT, messageConvertor());
		}

		@Bean
		public IntegrationFlow outboundFlow() {
			return IntegrationFlow.from(producerChannelServerIssue())
					.log(LoggingHandler.Level.INFO)
					.handle(Nats.outboundAdapter(natsTemplate()), e -> e.advice(producerAdvice()))
					.get();
		}

		/*
		 * Advice Bean configuration to setup the retry template and errorChannel
		 * for error message processing
		 * */
		@Bean
		public Advice producerAdvice() {
			RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
			RetryTemplate retryTemplate = new RetryTemplate();
			FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
			fixedBackOffPolicy.setBackOffPeriod(3000);
			retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
			RetryPolicy retryPolicy = new SimpleRetryPolicy(3);
			retryTemplate.setRetryPolicy(retryPolicy);
			advice.setRetryTemplate(retryTemplate);
			advice.setRecoveryCallback(new ErrorMessageSendingRecoverer(errorChannel()));
			return advice;
		}

		@Bean
		public MessageConverter<String> messageConvertor() {
			return new MessageConverter<>(String.class);
		}

		@Bean
		public IntegrationFlow inboundFlow() {
			return IntegrationFlow.from(
							Nats.messageDrivenChannelAdapter(
											Nats.container(
															testConsumerFactoryForNegativeFlow(), NatsMessageDeliveryMode.PULL)
													.id("testContainer")
													.concurrency(1),
											messageConvertor()) //
									.id("testAdapter") //
					) //
					.log(LoggingHandler.Level.INFO) //
					.channel("consumerChannel") //
					.get();
		}

		@Bean
		public NatsConsumerFactory testConsumerFactoryForNegativeFlow() {
			final ConsumerProperties consumerProperties =
					new ConsumerProperties(
							TEST_STREAM, TEST_SUBJECT, TEST_SUBJECT_CONSUMER, "test-subject-group");
			consumerProperties.setConsumerMaxWait(Duration.ofSeconds(1));
			return new NatsConsumerFactory(this.natsConnection, consumerProperties);
		}

		@Bean
		public MessageChannel errorChannel() {
			return MessageChannels.direct().getObject();
		}

		@Bean
		public MessageChannel consumerChannel() {
			return MessageChannels.direct().getObject();
		}

		/*
		 * Error Channel Service Activator
		 * Messages will reach here once the retries are exhausted.
		 * This method captures the error messages payload and their exceptions in Hashmap for verification
		 * */
		@ServiceActivator(inputChannel = "errorChannel")
		public void processErrorMessage(@Payload final ErrorMessage message) {
			MessagingException messagingException = (MessagingException) message.getPayload();
			Message originalFailedMessage = messagingException.getFailedMessage();
			errorMessages.put((String) originalFailedMessage.getPayload(), messagingException);
			LOG.error("*********************************************");
			LOG.error(
					"ERROR Message received in service Activator for payload: "
							+ originalFailedMessage.getPayload()
							+ " => error: "
							+ messagingException);
		}

		/*
		 * Consumer Channel Service Activator
		 * This method captures the consumers messages payload and their MetaData in Hashmap for verification
		 * */
		@ServiceActivator(inputChannel = "consumerChannel")
		public void processConsumedMessage(@Payload final Message<?> message) {
			String payload = (String) message.getPayload();
			NatsJetStreamMetaData metaData =
					(NatsJetStreamMetaData) message.getHeaders().get("nats_metadata");
			consumedMessages.put(payload, metaData);
			LOG.info("*********************************************");
			LOG.info(
					"Consumed Message received in service Activator for payload: "
							+ payload
							+ " => metaData: "
							+ metaData);
		}
	}
}
