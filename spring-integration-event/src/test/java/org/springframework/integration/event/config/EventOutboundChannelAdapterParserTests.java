/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.event.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
@RecordApplicationEvents
@DirtiesContext
public class EventOutboundChannelAdapterParserTests {

	@Autowired
	private volatile ConfigurableApplicationContext context;

	@Autowired
	private ApplicationEvents applicationEvents;

	private static volatile int adviceCalled;

	@AfterEach
	void cleanup() {
		this.applicationEvents.clear();
	}

	@Test
	public void validateEventParser() {
		EventDrivenConsumer adapter = this.context.getBean("eventAdapter", EventDrivenConsumer.class);
		assertThat(adapter).isNotNull();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		MessageHandler handler = (MessageHandler) adapterAccessor.getPropertyValue("handler");
		assertThat(handler instanceof ApplicationEventPublishingMessageHandler).isTrue();
		assertThat(adapterAccessor.getPropertyValue("inputChannel")).isEqualTo(this.context.getBean("input"));
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "publishPayload")).isTrue();
	}

	@Test
	public void validateUsage() {
		DirectChannel channel = context.getBean("input", DirectChannel.class);
		channel.send(new GenericMessage<>("hello"));
		assertThat(this.applicationEvents.stream(String.class)).containsOnly("hello");
	}

	@Test
	public void withAdvice() {
		DirectChannel channel = context.getBean("inputAdvice", DirectChannel.class);
		channel.send(new GenericMessage<>("hello"));
		assertThat(this.applicationEvents.stream(MessagingEvent.class))
				.hasSize(1)
				.satisfiesExactly(event -> assertThat(event.getMessage().getPayload()).isEqualTo("hello"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testInsideChain() {
		DirectChannel channel = context.getBean("inputChain", DirectChannel.class);
		channel.send(new GenericMessage<>("foo"));
		assertThat(this.applicationEvents.stream(MessagingEvent.class))
				.hasSize(1)
				.satisfiesExactly(event -> assertThat(event.getMessage().getPayload()).isEqualTo("foobar"));
	}

	@Test
	@Timeout(10000)
	public void validateUsageWithPollableChannel() throws InterruptedException {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("EventOutboundChannelAdapterParserTestsWithPollable-context.xml",
						EventOutboundChannelAdapterParserTests.class);

		CountDownLatch eventLatch = new CountDownLatch(1);

		ApplicationListener<?> listener = event -> {
			Object source = event.getSource();
			if (source instanceof Message) {
				String payload = (String) ((Message<?>) source).getPayload();
				if (payload.equals("hello")) {
					eventLatch.countDown();
				}
			}
		};
		context.addApplicationListener(listener);
		QueueChannel channel = context.getBean("input", QueueChannel.class);
		channel.send(new GenericMessage<>("hello"));

		assertThat(eventLatch.await(10, TimeUnit.SECONDS)).isTrue();

		context.close();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
