/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.SourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageBusTests {

	@Test
	public void testChannelsConnectedWithEndpoint() {
		MessageBus bus = new MessageBus();
		MessageChannel sourceChannel = new SimpleChannel();
		MessageChannel targetChannel = new SimpleChannel();
		bus.registerChannel("sourceChannel", sourceChannel);
		sourceChannel.send(new StringMessage("123", "test"));
		bus.registerChannel("targetChannel", targetChannel);
		GenericMessageEndpoint<String> endpoint = new GenericMessageEndpoint<String>();
		endpoint.setInputChannelName("sourceChannel");
		endpoint.setDefaultOutputChannelName("targetChannel");
		bus.registerEndpoint("endpoint", endpoint);
		bus.start();
		Message<?> result = targetChannel.receive(100);
		assertEquals("test", result.getPayload());
		bus.stop();
	}

	@Test
	public void testChannelsWithoutEndpoint() {
		MessageBus bus = new MessageBus();
		MessageChannel sourceChannel = new SimpleChannel();
		sourceChannel.send(new StringMessage("123", "test"));
		MessageChannel targetChannel = new SimpleChannel();
		bus.registerChannel("sourceChannel", sourceChannel);
		bus.registerChannel("targetChannel", targetChannel);
		bus.start();
		Message<?> result = targetChannel.receive(100);
		assertNull(result);
		bus.stop();
	}

	@Test
	public void testAutodetectionWithApplicationContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		context.start();
		MessageChannel sourceChannel = (MessageChannel) context.getBean("sourceChannel");
		sourceChannel.send(new GenericMessage<String>("123", "test"));		
		MessageChannel targetChannel = (MessageChannel) context.getBean("targetChannel");
		MessageBus bus = (MessageBus) context.getBean("bus");
		ConsumerPolicy policy = new ConsumerPolicy();
		Subscription subscription = new Subscription();
		subscription.setChannel("sourceChannel");
		subscription.setReceiver("endpoint");
		subscription.setPolicy(policy);
		bus.activateSubscription(subscription);
		Message<?> result = targetChannel.receive(100);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void testExactlyOneEndpointReceivesUnicastMessage() {
		SimpleChannel inputChannel = new SimpleChannel();
		SimpleChannel outputChannel1 = new SimpleChannel();
		SimpleChannel outputChannel2 = new SimpleChannel();
		GenericMessageEndpoint<String> endpoint1 = new GenericMessageEndpoint<String>();
		endpoint1.setDefaultOutputChannelName("output1");
		endpoint1.setInputChannelName("input");
		GenericMessageEndpoint<String> endpoint2 = new GenericMessageEndpoint<String>();
		endpoint2.setDefaultOutputChannelName("output2");
		endpoint2.setInputChannelName("input");
		MessageBus bus = new MessageBus();
		bus.registerChannel("input", inputChannel);
		bus.registerChannel("output1", outputChannel1);
		bus.registerChannel("output2", outputChannel2);
		bus.registerEndpoint("endpoint1", endpoint1);
		bus.registerEndpoint("endpoint2", endpoint2);
		bus.start();
		inputChannel.send(new StringMessage(1, "testing"));
		Message<?> message1 = outputChannel1.receive(100);
		Message<?> message2 = outputChannel2.receive(0);
		bus.stop();
		assertTrue("exactly one message should be null", message1 == null ^ message2 == null);
	}

	@Test
	public void testInvalidMessageChannelWithFailedDispatch() {
		MessageBus bus = new MessageBus();
		SourceAdapter sourceAdapter = new FailingSourceAdapter();
		bus.registerSourceAdapter("testAdapter", sourceAdapter);
		bus.start();
		Message<?> message = bus.getInvalidMessageChannel().receive(100);
		assertNotNull("message should not be null", message);
		assertTrue(message instanceof ErrorMessage);
		assertEquals("intentional test failure", ((ErrorMessage) message).getPayload().getMessage());
		bus.stop();
	}


	private static class FailingSourceAdapter implements SourceAdapter, MessageDispatcher {

		public void setChannel(MessageChannel channel) {
		}

		public int dispatch() {
			throw new RuntimeException("intentional test failure");
		}

		public ConsumerPolicy getConsumerPolicy() {
			return ConsumerPolicy.newPollingPolicy(1000);
		}

		public boolean isRunning() {
			return true;
		}

		public void start() {
		}

		public void stop() {
		}
	}
}
