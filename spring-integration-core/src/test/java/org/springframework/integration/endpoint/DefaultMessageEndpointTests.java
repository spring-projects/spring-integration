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

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultMessageEndpointTests {

	@Test
	public void testDefaultReplyChannel() throws Exception {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(handler);
		endpoint.setDefaultOutputChannelName("replyChannel");
		endpoint.start();
		endpoint.handle(new StringMessage(1, "test"));
		endpoint.stop();
		Message<?> reply = replyChannel.receive(50);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testExplicitReplyChannel() throws Exception {
		final MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(handler);
		endpoint.start();
		StringMessage testMessage = new StringMessage(1, "test");
		testMessage.getHeader().setReplyChannelName("replyChannel");
		endpoint.handle(testMessage);
		endpoint.stop();
		Message<?> reply = replyChannel.receive(50);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

}
