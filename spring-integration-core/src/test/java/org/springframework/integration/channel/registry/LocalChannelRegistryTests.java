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

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author David Turanski
 * @author Mark Fisher
 * @since 3.0
 */
public class LocalChannelRegistryTests {

	private LocalChannelRegistry registry = new LocalChannelRegistry();

	private GenericApplicationContext context = new GenericApplicationContext();

	@Before
	public void setUp() {
		registry.setApplicationContext(context);
		context.refresh();
	}

	@Test
	public void testInbound() {
		DirectChannel channel = new DirectChannel();
		registry.inbound("inbound", channel);
		assertTrue(context.containsBean("inbound"));

		final AtomicBoolean messageReceived = new AtomicBoolean();
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});
		SubscribableChannel registeredChannel = context.getBean("inbound", SubscribableChannel.class);
		registeredChannel.send(new GenericMessage<String>("hello"));
		assertTrue(messageReceived.get());
	}

	@Test
	public void testOutbound() {
		DirectChannel channel = new DirectChannel();
		registry.outbound("outbound", channel);
		assertTrue(context.containsBean("outbound"));

		final AtomicBoolean messageReceived = new AtomicBoolean();
		SubscribableChannel registeredChannel = context.getBean("outbound", SubscribableChannel.class);
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

	@Test(expected = IllegalArgumentException.class)
	public void testOutboundTapShouldFail() {
		DirectChannel channel = new DirectChannel();
		registry.outbound("outbound", channel);
		DirectChannel tapChannel = new DirectChannel();
		registry.tap("outbound", tapChannel);
	}

	@Test
	public void testInboundTap() {
		DirectChannel channel = new DirectChannel();
		registry.inbound("inbound", channel);
		DirectChannel tapChannel = new DirectChannel();
		registry.tap("inbound", tapChannel);
		final AtomicBoolean originalMessageReceived = new AtomicBoolean();
		final AtomicBoolean tapMessageReceived = new AtomicBoolean();
		tapChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				tapMessageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				originalMessageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});
		MessageChannel registeredChannel = context.getBean("inbound", MessageChannel.class);
		registeredChannel.send(new GenericMessage<String>("hello"));
		assertTrue(originalMessageReceived.get());
		assertTrue(tapMessageReceived.get());
	}

	@Test
	public void testFlowThroughRegisteredChannelFromOutboundToInbound() {
		DirectChannel outbound = new DirectChannel();
		DirectChannel inbound = new DirectChannel();
		registry.outbound("foo", outbound);
		registry.inbound("foo", inbound);
		final AtomicBoolean messageReceived = new AtomicBoolean();
		inbound.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});
		outbound.send(new GenericMessage<String>("hello"));
		assertTrue(messageReceived.get());
	}

	@Test
	public void testFlowThroughRegisteredChannelFromOutboundToInboundWithTap() {
		DirectChannel outbound = new DirectChannel();
		DirectChannel inbound = new DirectChannel();
		DirectChannel tap = new DirectChannel();
		registry.outbound("foo", outbound);
		registry.inbound("foo", inbound);
		registry.tap("foo", tap);
		final AtomicBoolean originalMessageReceived = new AtomicBoolean();
		final AtomicBoolean tapMessageReceived = new AtomicBoolean();
		inbound.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				originalMessageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});
		tap.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				tapMessageReceived.set(true);
				assertEquals("hello", message.getPayload());
			}
		});
		outbound.send(new GenericMessage<String>("hello"));
		assertTrue(originalMessageReceived.get());
		assertTrue(tapMessageReceived.get());
	}

}
