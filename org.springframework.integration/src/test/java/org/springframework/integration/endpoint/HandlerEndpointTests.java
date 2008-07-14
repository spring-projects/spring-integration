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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.message.selector.MessageSelectorChain;

/**
 * @author Mark Fisher
 */
public class HandlerEndpointTests {

	@Test
	public void testDefaultReplyChannel() throws Exception {
		MessageChannel replyChannel = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setOutputChannelName("replyChannel");
		endpoint.send(new StringMessage(1, "test"));
		Message<?> reply = replyChannel.receive(50);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testExplicitReplyChannel() throws Exception {
		final MessageChannel replyChannel = new QueueChannel();
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		StringMessage testMessage = new StringMessage(1, "test");
		testMessage.getHeader().setReturnAddress(replyChannel);
		endpoint.send(testMessage);
		Message<?> reply = replyChannel.receive(50);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testExplicitReplyChannelName() throws Exception {
		final MessageChannel replyChannel = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		endpoint.setChannelRegistry(channelRegistry);
		StringMessage testMessage = new StringMessage(1, "test");
		testMessage.getHeader().setReturnAddress("replyChannel");
		endpoint.send(testMessage);
		Message<?> reply = replyChannel.receive(50);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testDynamicReplyChannel() throws Exception {
		final MessageChannel replyChannel1 = new QueueChannel();
		final MessageChannel replyChannel2 = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel2", replyChannel2);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		endpoint.setChannelRegistry(channelRegistry);
		StringMessage testMessage = new StringMessage("test");
		testMessage.getHeader().setReturnAddress(replyChannel1);
		endpoint.send(testMessage);
		Message<?> reply1 = replyChannel1.receive(50);
		assertNotNull(reply1);
		assertEquals("hello test", reply1.getPayload());
		Message<?> reply2 = replyChannel2.receive(0);
		assertNull(reply2);
		testMessage.getHeader().setReturnAddress("replyChannel2");
		endpoint.send(testMessage);
		reply1 = replyChannel1.receive(0);
		assertNull(reply1);
		reply2 = replyChannel2.receive(0);
		assertNotNull(reply2);	
		assertEquals("hello test", reply2.getPayload());	
	}

	@Test
	public void testHandlerReturnsNull() throws InterruptedException {
		MessageChannel replyChannel = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setOutputChannelName("replyChannel");
		endpoint.send(new StringMessage(1, "test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(0);
		assertNull(reply);
	}

	@Test(expected=MessageRejectedException.class)
	public void testEndpointWithSelectorRejecting() {
		HandlerEndpoint endpoint = new HandlerEndpoint(TestHandlers.nullHandler());
		endpoint.setMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		endpoint.send(new StringMessage("test"));
	}

	@Test
	public void testEndpointWithSelectorAccepting() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		HandlerEndpoint endpoint = new HandlerEndpoint(TestHandlers.countDownHandler(latch));
		endpoint.setMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		});
		endpoint.send(new StringMessage("test"));
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked", 0, latch.getCount());
	}

	@Test
	public void testEndpointWithMultipleSelectorsAndFirstRejects() {
		final AtomicInteger counter = new AtomicInteger();
		HandlerEndpoint endpoint = new HandlerEndpoint(TestHandlers.countingHandler(counter));
		MessageSelectorChain selectorChain = new MessageSelectorChain();
		selectorChain.add(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return false;
			}
		});
		selectorChain.add(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		endpoint.setMessageSelector(selectorChain);
		boolean exceptionWasThrown = false;
		try {
			endpoint.send(new StringMessage("test"));
		}
		catch (MessageRejectedException e) {
			exceptionWasThrown = true;
		}
		assertTrue(exceptionWasThrown);
		assertEquals("only the first selector should have been invoked", 1, counter.get());
	}

	@Test
	public void testEndpointWithMultipleSelectorsAndFirstAccepts() {
		final AtomicInteger selectorCounter = new AtomicInteger();
		AtomicInteger handlerCounter = new AtomicInteger();
		HandlerEndpoint endpoint = new HandlerEndpoint(TestHandlers.countingHandler(handlerCounter));
		MessageSelectorChain selectorChain = new MessageSelectorChain();
		selectorChain.add(new MessageSelector() {
			public boolean accept(Message<?> message) {
				selectorCounter.incrementAndGet();
				return true;
			}
		});
		selectorChain.add(new MessageSelector() {
			public boolean accept(Message<?> message) {
				selectorCounter.incrementAndGet();
				return false;
			}
		});
		endpoint.setMessageSelector(selectorChain);
		boolean exceptionWasThrown = false;
		try {
			endpoint.send(new StringMessage("test"));
		}
		catch (MessageRejectedException e) {
			exceptionWasThrown = true;
		}
		assertTrue(exceptionWasThrown);
		assertEquals("both selectors should have been invoked", 2, selectorCounter.get());
		assertEquals("the handler should not have been invoked", 0, handlerCounter.get());
	}

	@Test
	public void testEndpointWithMultipleSelectorsAndBothAccept() {
		final AtomicInteger counter = new AtomicInteger();
		HandlerEndpoint endpoint = new HandlerEndpoint(TestHandlers.countingHandler(counter));
		MessageSelectorChain selectorChain = new MessageSelectorChain();
		selectorChain.add(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		selectorChain.add(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		endpoint.setMessageSelector(selectorChain);
		assertTrue(endpoint.send(new StringMessage("test")));
		assertEquals("both selectors and handler should have been invoked", 3, counter.get());
	}

	@Test
	public void testCorrelationId() {
		QueueChannel replyChannel = new QueueChannel(1);
		HandlerEndpoint endpoint = new HandlerEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return message;
			}
		});
		Message<?> message = new StringMessage("test");
		message.getHeader().setReturnAddress(replyChannel);
		endpoint.send(message);
		Message<?> reply = replyChannel.receive(500);
		assertEquals(message.getId(), reply.getHeader().getCorrelationId());
	}

	@Test
	public void testCorrelationIdSetByHandlerTakesPrecedence() {
		QueueChannel replyChannel = new QueueChannel(1);
		HandlerEndpoint endpoint = new HandlerEndpoint(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				message.getHeader().setCorrelationId("ABC-123");
				return message;
			}
		});
		Message<?> message = new StringMessage("test");
		message.getHeader().setReturnAddress(replyChannel);
		endpoint.send(message);
		Message<?> reply = replyChannel.receive(500);
		Object correlationId = reply.getHeader().getCorrelationId();
		assertFalse(message.getId().equals(correlationId));
		assertEquals("ABC-123", correlationId);
	}

}
