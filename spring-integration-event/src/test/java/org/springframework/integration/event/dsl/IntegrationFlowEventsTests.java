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

package org.springframework.integration.event.dsl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnablePublisher;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@RecordApplicationEvents
@DirtiesContext
public class IntegrationFlowEventsTests {

	private static final MessageGroupStore messageGroupStore = new SimpleMessageStore();

	private static final String GROUP_ID = "testGroup";

	@BeforeAll
	public static void setup() {
		messageGroupStore.addMessageToGroup(GROUP_ID, new GenericMessage<>("foo"));
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private PollableChannel resultsChannel;

	@Autowired
	private PollableChannel delayedResults;

	@Autowired
	@Qualifier("flow3Input")
	private MessageChannel flow3Input;

	@Autowired
	private ApplicationEvents applicationEvents;

	@Autowired
	QueueChannel eventFromPublisher;

	@Test
	public void testEventsFlow() {
		assertThat(this.applicationEvents.stream(MessagingEvent.class)).isEmpty();
		this.flow3Input.send(new GenericMessage<>("2"));
		assertThat(this.applicationEvents.stream(MessagingEvent.class))
				.hasSize(1)
				.satisfiesExactly(event -> assertThat(event.getMessage().getPayload()).isEqualTo(4));
	}

	@Test
	public void testRawApplicationEventListeningMessageProducer() {
		this.applicationContext.publishEvent(new TestApplicationEvent1());
		Message<?> receive = this.resultsChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(TestApplicationEvent1.class);

		this.applicationContext.publishEvent(new TestApplicationEvent2());
		receive = this.resultsChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(TestApplicationEvent2.class);
	}

	@Test
	public void testDelayRescheduling() {
		Message<?> receive = this.delayedResults.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("foo");
		assertThat(messageGroupStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageGroupStore.getMessageCountForAllMessageGroups()).isEqualTo(0);
	}

	@Test
	public void eventFromPublisherAnnotation() {
		this.applicationContext.publishEvent(new TestApplicationEvent3());
		Message<?> receive = this.eventFromPublisher.receive(10000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload).isEqualTo("TestApplicationEvent3");
	}

	@Configuration
	@EnableIntegration
	@EnablePublisher
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow flow3() {
			return IntegrationFlow.from("flow3Input")
					.handle(Integer.class, new GenericHandler<Integer>() {

						@SuppressWarnings("unused")
						public void setFoo(String foo) {
						}

						@SuppressWarnings("unused")
						public void setFoo(Integer foo) {
						}

						@Override
						public Object handle(Integer p, MessageHeaders h) {
							return p * 2;
						}

					})
					.handle(new ApplicationEventPublishingMessageHandler())
					.get();
		}

		@Bean
		public ApplicationListener<?> applicationListener() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(TestApplicationEvent1.class);
			producer.setOutputChannel(resultsChannel());
			return producer;
		}

		@Bean
		public PollableChannel resultsChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow eventProducerFlow() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(TestApplicationEvent2.class);

			return IntegrationFlow.from(producer)
					.channel(resultsChannel())
					.get();
		}

		@Bean
		public IntegrationFlow delayFlow() {
			return flow -> flow
					.delay(e -> e
							.messageGroupId(GROUP_ID)
							.messageStore(messageGroupStore)
							.id("delayer"))
					.channel(MessageChannels.queue("delayedResults"));
		}

		@Bean
		QueueChannel eventFromPublisher() {
			return new QueueChannel();
		}

		@EventListener
		@Publisher("eventFromPublisher")
		public String publishEventToChannel(TestApplicationEvent3 testApplicationEvent3) {
			return testApplicationEvent3.getSource().toString();
		}

	}

	@SuppressWarnings("serial")
	private static final class TestApplicationEvent1 extends ApplicationEvent {

		TestApplicationEvent1() {
			super("TestApplicationEvent1");
		}

	}

	@SuppressWarnings("serial")
	private static final class TestApplicationEvent2 extends ApplicationEvent {

		TestApplicationEvent2() {
			super("TestApplicationEvent2");
		}

	}

	@SuppressWarnings("serial")
	private static final class TestApplicationEvent3 extends ApplicationEvent {

		TestApplicationEvent3() {
			super("TestApplicationEvent3");
		}

	}

}
