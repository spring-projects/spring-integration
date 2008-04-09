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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHeader;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.Subscription;

/**
 * @author Mark Fisher
 */
public class RequestReplyTemplateTests {

	private final SimpleChannel requestChannel = new SimpleChannel();


	public RequestReplyTemplateTests() {
		MessageHandler testHandler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage(message.getPayload().toString().toUpperCase());
			}		
		};
		MessageBus bus = new MessageBus();
		bus.registerChannel("requestChannel", requestChannel);
		bus.registerHandler("testHandler", testHandler, new Subscription(requestChannel));
		bus.start();
	}


	@Test
	public void testSynchronousRequestReply() {
		RequestReplyTemplate template = new RequestReplyTemplate(requestChannel);
		Message<?> reply = template.request(new StringMessage("test"));
		assertEquals("TEST", reply.getPayload());
	}

	@Test
	public void testAsynchronousRequestAndReply() throws InterruptedException {
		final List<String> replies = new ArrayList<String>(3);
		final CountDownLatch latch = new CountDownLatch(3);
		ReplyHandler replyHandler = new ReplyHandler() {
			public void handle(Message<?> replyMessage, MessageHeader originalMessageHeader) {
				replies.add((String) replyMessage.getPayload());
				latch.countDown();
			}
		};
		RequestReplyTemplate template = new RequestReplyTemplate(requestChannel);
		template.request(new StringMessage("test1"), replyHandler);
		template.request(new StringMessage("test2"), replyHandler);
		template.request(new StringMessage("test3"), replyHandler);
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertTrue(replies.contains("TEST1"));
		assertTrue(replies.contains("TEST2"));
		assertTrue(replies.contains("TEST3"));
	}

}
