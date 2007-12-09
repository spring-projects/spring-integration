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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.DocumentMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class GenericMessageEndpointTests {

	@Test
	public void testDefaultReplyChannel() throws Exception {
		MessageChannel channel = new PointToPointChannel();
		MessageChannel replyChannel = new PointToPointChannel();
		MessageHandler handler = new MessageHandler() {
			public Message handle(Message message) {
				return new DocumentMessage("123", "hello " + message.getPayload());
			}
		};
		GenericMessageEndpoint endpoint = new GenericMessageEndpoint();
		endpoint.setSource(channel);
		endpoint.setHandler(handler);
		endpoint.setTarget(replyChannel);
		MessageBus bus = new MessageBus();
		bus.registerChannel("testChannel", channel);
		bus.registerEndpoint("testEndpoint", endpoint);
		ConsumerPolicy policy = new ConsumerPolicy();
		policy.setPeriod(0);
		bus.activateSubscription("testChannel", "testEndpoint", policy);
		bus.start();
		DocumentMessage testMessage = new DocumentMessage(1, "test");
		channel.send(testMessage);
		Message reply = replyChannel.receive(10);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testExplicitReplyChannel() throws Exception {
		MessageChannel channel = new PointToPointChannel();
		final MessageChannel replyChannel = new PointToPointChannel();
		MessageHandler handler = new MessageHandler() {
			public Message handle(Message message) {
				return new DocumentMessage("123", "hello " + message.getPayload());
			}
		};
		ChannelResolver channelResolver = new ChannelResolver() {
			public MessageChannel resolve(String channelName) {
				if (channelName.equals("replyChannel")) {
					return replyChannel;
				}
				return null;
			}
		};
		GenericMessageEndpoint endpoint = new GenericMessageEndpoint();
		endpoint.setSource(channel);
		endpoint.setHandler(handler);
		endpoint.setChannelResolver(channelResolver);
		MessageBus bus = new MessageBus();
		bus.registerChannel("testChannel", channel);
		bus.registerEndpoint("testEndpoint", endpoint);
		ConsumerPolicy policy = new ConsumerPolicy();
		policy.setPeriod(0);
		bus.activateSubscription("testChannel", "testEndpoint", policy);
		bus.start();
		DocumentMessage testMessage = new DocumentMessage(1, "test");
		testMessage.getHeader().setReplyChannelName("replyChannel");
		channel.send(testMessage);
		Message reply = replyChannel.receive(10);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

}
