/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gunnar Hillert
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EventOutboundChannelAdapterParserTests {

	@Autowired
	private volatile ConfigurableApplicationContext context;

	private volatile boolean receivedEvent;

	private static volatile int adviceCalled;

	@Test
	public void validateEventParser() {
		EventDrivenConsumer adapter = this.context.getBean("eventAdapter", EventDrivenConsumer.class);
		assertThat(adapter).isNotNull();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		MessageHandler handler = (MessageHandler) adapterAccessor.getPropertyValue("handler");
		assertThat(handler instanceof ApplicationEventPublishingMessageHandler).isTrue();
		assertThat(adapterAccessor.getPropertyValue("inputChannel")).isEqualTo(this.context.getBean("input"));
		assertThat(TestUtils.getPropertyValue(handler, "publishPayload", Boolean.class)).isTrue();
	}

	@Test
	public void validateUsage() {
		ApplicationListener<?> listener = event -> {
			if (event instanceof PayloadApplicationEvent) {
				String payload = (String) ((PayloadApplicationEvent<?>) event).getPayload();
				if (payload.equals("hello")) {
					receivedEvent = true;
				}
			}
		};
		this.context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("input", DirectChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		assertThat(this.receivedEvent).isTrue();
	}

	@Test
	public void withAdvice() {
		this.receivedEvent = false;
		ApplicationListener<?> listener = event -> {
			Object source = event.getSource();
			if (source instanceof Message) {
				String payload = (String) ((Message<?>) source).getPayload();
				if (payload.equals("hello")) {
					receivedEvent = true;
				}
			}
		};
		context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("inputAdvice", DirectChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		assertThat(this.receivedEvent).isTrue();
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test //INT-2275
	public void testInsideChain() {
		this.receivedEvent = false;
		ApplicationListener<?> listener = event -> {
			Object source = event.getSource();
			if (source instanceof Message) {
				String payload = (String) ((Message<?>) source).getPayload();
				if (payload.equals("foobar")) {
					receivedEvent = true;
				}
			}
		};
		this.context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("inputChain", DirectChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		assertThat(this.receivedEvent).isTrue();
	}

	@Test(timeout = 10000)
	public void validateUsageWithPollableChannel() throws Exception {
		this.receivedEvent = false;
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("EventOutboundChannelAdapterParserTestsWithPollable-context.xml",
						EventOutboundChannelAdapterParserTests.class);
		final CyclicBarrier barrier = new CyclicBarrier(2);
		@SuppressWarnings("resource")
		ApplicationListener<?> listener = event -> {
			Object source = event.getSource();
			if (source instanceof Message) {
				String payload = (String) ((Message<?>) source).getPayload();
				if (payload.equals("hello")) {
					receivedEvent = true;
					try {
						barrier.await();
					}
					catch (InterruptedException e1) {
						Thread.currentThread().interrupt();
					}
					catch (BrokenBarrierException e2) {
						throw new IllegalStateException("broken barrier", e2);
					}
				}
			}
		};
		context.addApplicationListener(listener);
		QueueChannel channel = context.getBean("input", QueueChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		barrier.await();
		assertThat(this.receivedEvent).isTrue();
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
