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
package org.springframework.integration.nats;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collection;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.nats.support.config.ControlBusGateway;
import org.springframework.integration.nats.support.config.DefaultContextConfig;
import org.springframework.integration.nats.support.thread.BasicThreading;
import org.springframework.integration.nats.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This testcase shows by example how Scatter-Gather pattern can be applied for the advanced needs
 * e.g. like object propagation of nats message to the workflow level. Such advanced scenario could
 * be useful e.g. for manuel acknowledgment from spring integration workflow and not with
 * nats-spring-integration library. It can be e.g. useful to support transactional behavior of whole
 * workflow. Manuel acknowledgment provides the ability to manage acknowledgment based on the
 * workflow implementation details and provides big flexibility depending on the business
 * requirements.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
		classes = {
				NatsTestConfig.class,
				DefaultContextConfig.class,
				NatsSpringIntegrationScatterGatherTest.ContextConfig.class
		})
public class NatsSpringIntegrationScatterGatherTest extends AbstractNatsIntegrationTestSupport {

	@Autowired
	public ControlBusGateway controlBus;

	@Autowired
	private Connection natsConnection;

	@Autowired
	@Qualifier("messageConvertorString")
	private MessageConverter<String> messageConvertorString;

	@Test
	public void springIntegrationScatterGatherTest()
			throws JetStreamApiException, IOException, InterruptedException {

		// start adapter by sending input to Message gateway
		this.controlBus.send("@idUsedMessageDrivenChannelAdapter.start()");

		// Message producer to send valid message with correct converter String
		final NatsTemplate natsTemplateString =
				new NatsTemplate(
						this.natsConnection, DefaultContextConfig.TEST_SUBJECT, this.messageConvertorString);

		// publish valid messages to subject via natsTemplateString. Just general publishing of message
		// using NATS
		for (int i = 0; i < 100; i++) {
			final PublishAck ack = natsTemplateString.send("Hello" + i);
			assertNotNull(ack);
		}
		BasicThreading.wait(
				10000, "Await in main thread to give a chance the workflow thread to receive a messages");
	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	@Repository
	public static class ContextConfig extends DefaultContextConfig {

		@Autowired
		@Qualifier("messageConvertorString")
		private MessageConverter<String> messageConvertorString;

		@PostConstruct
		public void streamSetup() throws IOException, JetStreamApiException {
			super.streamSetup();
		}

		/**
		 * Background: one possible usecase of this example is message redelivery with unsuccessful
		 * situation, with different technical reasons like e.g. network issue, serve unavailability
		 * etc. This main workflow uses publish subscribe mechanism of spring integration to scatter the
		 * NATS message over the flows. There are 2 subscribed scattered flows flow2 and flow3 to the
		 * main workflow. It is assumed that scattered flows processing messages and producing differ
		 * type of messages respectively. Gather approach shows how two different types of messages
		 * could be aggregated into one single message for further processing. The flow3 simulates the
		 * business behavior and produces simulate business message, where flow2 is predefined to keep
		 * the original NATS message up to the time of gathering of messages, which occurs after all
		 * workflows accomplished (flow2 and flow3 in this example). Gather processor shows the ability
		 * of main workflow to have access to the original NATS object, which allows to trigger the
		 * acknowledgement manually, get detailed information from NATS metadata etc.
		 *
		 * @return
		 */
		@Bean
		public IntegrationFlow inboundNatsScatterGatherTestFlow() {
			return IntegrationFlow.from(
							Nats.messageDrivenChannelAdapter(
											Nats.container(defaultConsumerFactory(), NatsMessageDeliveryMode.PULL)
													.id("testErrorContainer")
													.concurrency(10)) //
									.id("idUsedMessageDrivenChannelAdapter") //
					) //
					.log(LoggingHandler.Level.INFO) //
					.scatterGather(
							pubSub(),
							a ->
									a.outputProcessor(
											g -> {
												Pair<NatsMessage, SimulatorNotificationRQ> all =
														transformMessages(g.getMessages());
												MessageBuilder<String> finalMessage =
														MessageBuilder.withPayload(all.getSecond().getPayload());
												finalMessage.setHeader("NATS-MESSAGE", all.getFirst());
												return finalMessage.build();
											}))
					.channel("consumerDirectChannel") //
					.get();
		}

		@Bean
		PublishSubscribeChannel pubSub() {
			PublishSubscribeChannel pubSub = new PublishSubscribeChannel(exec());
			pubSub.setApplySequence(true);
			return pubSub;
		}

		@Bean
		IntegrationFlow flow2() {
			return IntegrationFlow.from("pubSub")
					.<NatsMessage>handle(
							(p, h) -> {
								String strMessage = convertFromNatsMessage(p);
								LOG.info(
										"Flow2: Message with data:" + strMessage + " to be kept to gather it later.");
								return p;
							})
					.get();
		}

		@Bean
		IntegrationFlow flow3() {
			return IntegrationFlow.from("pubSub")
					.<NatsMessage>handle(
							(p, h) -> {
								String strMessage = convertFromNatsMessage(p);
								LOG.info(
										"Flow3: Message with data:"
												+ strMessage
												+ " to be processed and returns SimulatorNotificationRQ");
								return new SimulatorNotificationRQ(strMessage);
							})
					.get();
		}

		/**
		 * retrieve original payload from NATS message
		 *
		 * @param p
		 * @return
		 */
		private String convertFromNatsMessage(NatsMessage p) {
			Assert.assertTrue(
					"Received message is not expected type of NatsMessage.", p instanceof NatsMessage);
			String strMessage = null;
			try {
				strMessage = messageConvertorString.fromMessage(p);
			}
			catch (IOException e) {
				Assert.fail("Nats message should be convertable");
			}
			return strMessage;
		}

		/**
		 * Transforms the list representation of messages to the flat structure like pair. As we have
		 * only 2 flow it is possible to transform into pair data structure.
		 *
		 * @param allMessages
		 * @return
		 */
		private Pair<NatsMessage, SimulatorNotificationRQ> transformMessages(
				Collection<Message<?>> allMessages) {
			NatsMessage natsMessage = null;
			SimulatorNotificationRQ rq = null;
			Assert.assertEquals(
					"Amount of returned messages from subFlows should be exactly like:",
					2,
					allMessages.size());
			for (Message<?> message : allMessages) {
				if (message.getPayload() instanceof NatsMessage) {
					natsMessage = (NatsMessage) message.getPayload();
				}
				else if (message.getPayload() instanceof SimulatorNotificationRQ) {
					rq = (SimulatorNotificationRQ) message.getPayload();
				}
				else
					Assert.fail(
							"Messages are not in expected types of NatsMessage and SimulatorNotificationRQ!");
			}
			if (rq == null || natsMessage == null) {
				Assert.fail("All returned messages from subFlows has to be initialised!");
			}
			Assert.assertTrue("Origin of messages returned from SubFlows should be equal in spite of concurrent consumers but objects can be different:", convertFromNatsMessage(natsMessage).equals(rq.getPayload()));
			return Pair.of(natsMessage, rq);
		}

		@Bean
		public TaskExecutor exec() {
			ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
			exec.setCorePoolSize(2);
			return exec;
		}

		class SimulatorNotificationRQ {

			private final String payload;

			public SimulatorNotificationRQ(String payload) {
				this.payload = payload;
			}

			public String getPayload() {
				return payload;
			}
		}
	}
}
