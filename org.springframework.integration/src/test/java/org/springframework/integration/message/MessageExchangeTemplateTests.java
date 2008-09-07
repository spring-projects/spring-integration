/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractInOutEndpoint;

/**
 * @author Mark Fisher
 */
public class MessageExchangeTemplateTests {

	private QueueChannel requestChannel;


	@Before
	public void setUp() {
		this.requestChannel = new QueueChannel();
		this.requestChannel.setBeanName("requestChannel");
		AbstractInOutEndpoint endpoint = new AbstractInOutEndpoint() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage(message.getPayload().toString().toUpperCase());
			}		
		};
		MessageBus bus = new DefaultMessageBus();
		bus.registerChannel(requestChannel);
		endpoint.setBeanName("testEndpoint");
		endpoint.setInputChannel(requestChannel);
		bus.registerEndpoint(endpoint);
		bus.start();
	}


	@Test
	public void testSendAndReceive() {
		MessageExchangeTemplate template = new MessageExchangeTemplate();
		Message<?> reply = template.sendAndReceive(new StringMessage("test"), this.requestChannel);
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void testSendWithReturnAddress() throws InterruptedException {
		final List<String> replies = new ArrayList<String>(3);
		final CountDownLatch latch = new CountDownLatch(3);
		MessageChannel replyChannel = new MessageChannel() {
			public String getName() {
				return "testReplyChannel";
			}
			public boolean send(Message<?> replyMessage) {
				replies.add((String) replyMessage.getPayload());
				latch.countDown();
				return true;
			}
		};
		MessageExchangeTemplate template = new MessageExchangeTemplate();
		Message<String> message1 = MessageBuilder.fromPayload("test1").setReturnAddress(replyChannel).build();
		Message<String> message2 = MessageBuilder.fromPayload("test2").setReturnAddress(replyChannel).build();
		Message<String> message3 = MessageBuilder.fromPayload("test3").setReturnAddress(replyChannel).build();
		template.send(message1, this.requestChannel);
		template.send(message2, this.requestChannel);
		template.send(message3, this.requestChannel);
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertTrue(replies.contains("TEST1"));
		assertTrue(replies.contains("TEST2"));
		assertTrue(replies.contains("TEST3"));
	}

}
