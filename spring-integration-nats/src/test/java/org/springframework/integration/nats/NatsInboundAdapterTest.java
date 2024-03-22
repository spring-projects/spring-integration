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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.impl.NatsJetStreamMetaData;
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
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test the resilience of message consumer on adpater restart(application restart) using
 * NATS DSL configuration inbound adapter
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
@ContextConfiguration(classes = {NatsTestConfig.class, NatsInboundAdapterTest.ContextConfig.class})
public class NatsInboundAdapterTest extends AbstractNatsIntegrationTestSupport {

	private static final Log LOG = LogFactory.getLog(NatsInboundAdapterTest.class);

	private static final String TEST_SUBJECT = "test-subject";

	private static final String TEST_STREAM = "test-stream";

	private static final String TEST_SUBJECT_CONSUMER = "test-subject-consumer";

	public static HashMap<String, Throwable> errorMessages = new HashMap<>();

	public static HashMap<String, NatsJetStreamMetaData> consumedMessages = new HashMap<>();

	public static List<Message> consumedMessagesList = new ArrayList<>();

	@Autowired
	private Connection natsConnection;

	@Autowired
	@Qualifier("producerChannelServerIssue")
	private DirectChannel producerChannelServerIssue;

	@Autowired
	private ApplicationContext appContext;

	// Messaging gateway to start/stop adapter on demand
	@Autowired
	private ControlBusGateway controlBus;

	@Test
	public void testConsumerResiliencyOnAdapterRestart() throws InterruptedException {
		final String messageText = "Hello";
		// Send 20 messages
		sendBunchMessages(messageText, 0, 20);
		// wait for consumer to start processing messages
		Thread.sleep(10000);

		// stop adapter using messaging gateway
		this.controlBus.send("@testAdapter.stop()");
		LOG.info("messages consumed till inbound adapter stopped - 1st stop");
		LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		consumedMessages.keySet().stream()
				.map(s -> "OK: " + s + " => " + consumedMessages.get(s).deliveredCount())
				.forEach(System.out::println);
		consumedMessages.clear();

		// start adapter back - consumer should resume message processing
		this.controlBus.send("@testAdapter.start()");
		// wait for consumer to start processing messages
		Thread.sleep(20000);

		// stop adapter using messaging gateway
		this.controlBus.send("@testAdapter.stop()");
		LOG.info("messages consumed till inbound adapter stopped - 2nd stop");
		LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		consumedMessages.keySet().stream()
				.map(s -> "OK: " + s + " => " + consumedMessages.get(s).deliveredCount())
				.forEach(System.out::println);
		consumedMessages.clear();

		// Send 20 messages
		sendBunchMessages(messageText, 20, 25);

		// start adapter back - consumer should resume message processing
		this.controlBus.send("@testAdapter.start()");
		// wait for consumer to start processing messages
		Thread.sleep(60000);

		// stop adapter using messaging gateway
		this.controlBus.send("@testAdapter.stop()");
		LOG.info("messages consumed till inbound adapter stopped - 3rd stop");
		LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		consumedMessages.keySet().stream()
				.map(s -> "OK: " + s + " => " + consumedMessages.get(s).deliveredCount())
				.forEach(System.out::println);
		consumedMessages.clear();

		Assert.assertEquals(25, consumedMessagesList.size());

		for (int i = 0; i < 25; i++) {
			final Message message = consumedMessagesList.get(i);
			final String payload = (String) message.getPayload();
			final NatsJetStreamMetaData metaData =
					(NatsJetStreamMetaData) message.getHeaders().get("nats_metadata");
			Assert.assertTrue((messageText + i).equalsIgnoreCase(payload));
			Assert.assertEquals(1, metaData.deliveredCount());
			Assert.assertEquals(i + 1, metaData.consumerSequence());
			Assert.assertEquals(i + 1, metaData.streamSequence());
		}
	}

	/*
	 * Helper Method to sent list of messages
	 * */
	private void sendBunchMessages(String text, int from, int to) {
		for (int i = from; i < to; i++) {
			String payload = text + i;
			MessageBuilder<String> builder = MessageBuilder.withPayload(payload);
			LOG.debug("Sending Message to Channel => " + payload);
			Assert.assertTrue(
					this.producerChannelServerIssue.send(MessageBuilder.withPayload(payload).build()));
		}
	}

	/**
	 * Gateway interface used to start and stop adapter(spring integration) components.
	 */
	@MessagingGateway(defaultRequestChannel = "controlBus.input")
	private interface ControlBusGateway {

		void send(String command);
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
			// Consumer Config with durable consumer name
			ConsumerConfiguration consumerConfiguration =
					ConsumerConfiguration.builder()
							.ackWait(Duration.ofSeconds(30))
							.durable(TEST_SUBJECT_CONSUMER)
							.build();
			createConsumer(this.natsConnection, TEST_STREAM, consumerConfiguration);
		}

		@Bean
		public IntegrationFlow controlBus() {
			return IntegrationFlowDefinition::controlBus;
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
					.handle(Nats.outboundAdapter(natsTemplate()))
					.get();
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
									.autoStartup(true)) //
					.channel("consumerChannel") //
					.get();
		}

		@Bean
		public NatsConsumerFactory testConsumerFactoryForNegativeFlow() {
			final ConsumerProperties consumerProperties =
					new ConsumerProperties(
							TEST_STREAM, TEST_SUBJECT, TEST_SUBJECT_CONSUMER, "test-subject-group");
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
		 * Consumer Channel Service Activator
		 * This method captures the consumers messages payload and their MetaData in Hashmap for verification
		 * */
		@ServiceActivator(inputChannel = "consumerChannel")
		public void processConsumedMessage(@Payload final Message<?> message)
				throws InterruptedException {
			String payload = (String) message.getPayload();
			NatsJetStreamMetaData metaData =
					(NatsJetStreamMetaData) message.getHeaders().get("nats_metadata");
			consumedMessages.put(payload + "_" + metaData.timestamp(), metaData);
			Thread.sleep(1000);
			LOG.info("*********************************************");
			LOG.info(
					"Consumed Message received in service Activator for payload: "
							+ payload
							+ "_"
							+ metaData.timestamp()
							+ " => metaData: "
							+ metaData);
			LOG.info("*********************************************");
			consumedMessagesList.add(message);
		}
	}
}
