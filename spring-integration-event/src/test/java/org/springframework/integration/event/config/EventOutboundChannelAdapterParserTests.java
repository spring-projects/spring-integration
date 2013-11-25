/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.event.config;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
		EventDrivenConsumer adapter = context.getBean("eventAdapter", EventDrivenConsumer.class);
		Assert.assertNotNull(adapter);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		MessageHandler handler = (MessageHandler) adapterAccessor.getPropertyValue("handler");
		Assert.assertTrue(handler instanceof ApplicationEventPublishingMessageHandler);
		Assert.assertEquals(context.getBean("input"), adapterAccessor.getPropertyValue("inputChannel"));
	}

	@Test
	public void validateUsage() {
		ApplicationListener<?> listener = new ApplicationListener<ApplicationEvent>() {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				Object source = event.getSource();
				if (source instanceof Message){
					String payload = (String) ((Message<?>) source).getPayload();
					if (payload.equals("hello")) {
						receivedEvent = true;
					}
				}
			}
		};
		context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("input", DirectChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		Assert.assertTrue(receivedEvent);
	}

	@Test
	public void withAdvice() {
		receivedEvent = false;
		ApplicationListener<?> listener = new ApplicationListener<ApplicationEvent>() {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				Object source = event.getSource();
				if (source instanceof Message){
					String payload = (String) ((Message<?>) source).getPayload();
					if (payload.equals("hello")) {
						receivedEvent = true;
					}
				}
			}
		};
		context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("inputAdvice", DirectChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		Assert.assertTrue(receivedEvent);
		Assert.assertEquals(1, adviceCalled);
	}

	@Test //INT-2275
	public void testInsideChain() {
		receivedEvent = false;
		ApplicationListener<?> listener = new ApplicationListener<ApplicationEvent>() {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				Object source = event.getSource();
				if (source instanceof Message){
					String payload = (String) ((Message<?>) source).getPayload();
					if (payload.equals("foobar")) {
						receivedEvent = true;
					}
				}
			}
		};
		context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("inputChain", DirectChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		Assert.assertTrue(receivedEvent);
	}

	@Test(timeout=10000)
	public void validateUsageWithPollableChannel() throws Exception {
		receivedEvent = false;
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("EventOutboundChannelAdapterParserTestsWithPollable-context.xml", EventOutboundChannelAdapterParserTests.class);
		final CyclicBarrier barier = new CyclicBarrier(2);
		ApplicationListener<?> listener = new ApplicationListener<ApplicationEvent>() {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				Object source = event.getSource();
				if (source instanceof Message){
					String payload = (String) ((Message<?>) source).getPayload();
					if (payload.equals("hello")){
						receivedEvent = true;
						try {
							barier.await();
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						catch (BrokenBarrierException e) {
							throw new IllegalStateException("broken barrier", e);
						}
					}
				}
			}
		};
		context.addApplicationListener(listener);
		QueueChannel channel = context.getBean("input", QueueChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		barier.await();
		Assert.assertTrue(receivedEvent);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
