/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.channel.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author David Turanski
 * @since 3.0
 *
 */
public class LocalChannelRegistryTests {
	private LocalChannelRegistry registry = new LocalChannelRegistry();
	private ApplicationContext context = new GenericApplicationContext();

	@Before
	public void setUp() {
		registry.setApplicationContext(context);
	}

	@Test
	public void testInbound() {
		DirectChannel channel = new DirectChannel();
		registry.inbound("inbound", channel);
		assertTrue(context.containsBean("inbound"));

		SubscribableChannel registeredChannel = context.getBean("inbound", SubscribableChannel.class);

		final AtomicBoolean messageReceived = new AtomicBoolean();

		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});

		registeredChannel.send(new GenericMessage<String>("hello"));

		assertTrue(messageReceived.get());

	}

	@Test
	public void testOutbound() {
		DirectChannel channel = new DirectChannel();
		registry.outbound("outbound", channel);
		assertTrue(context.containsBean("outbound"));

		SubscribableChannel registeredChannel = context.getBean("outbound", SubscribableChannel.class);

		final AtomicBoolean messageReceived = new AtomicBoolean();

		registeredChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});

		channel.send(new GenericMessage<String>("hello"));

		assertTrue(messageReceived.get());

	}

	@Test
	public void testTap() {
		DirectChannel channel = new DirectChannel();
		registry.tap("tap", channel);
		assertTrue(context.containsBean("tap"));

		SubscribableChannel registeredChannel = context.getBean("tap", SubscribableChannel.class);

		final AtomicBoolean messageReceived = new AtomicBoolean();

		registeredChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});

		//avoid DHNS error
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {

			}
		});

		channel.send(new GenericMessage<String>("hello"));

		assertTrue(messageReceived.get());

	}
}
