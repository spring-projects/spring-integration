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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.TestHandlers;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.message.selector.MessageSelectorRejectedException;
import org.springframework.integration.util.ErrorHandler;

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
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setHandler(handler);
		endpoint.start();
		StringMessage testMessage = new StringMessage(1, "test");
		testMessage.getHeader().setReplyChannel(replyChannel);
		endpoint.handle(testMessage);
		endpoint.stop();
		Message<?> reply = replyChannel.receive(50);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testExplicitReplyChannelName() throws Exception {
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

	@Test
	public void testReplyChannelTakesPrecedenceOverReplyChannelName() throws Exception {
		final MessageChannel replyChannel1 = new SimpleChannel();
		final MessageChannel replyChannel2 = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel2", replyChannel2);
		MessageHandler handler = new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setHandler(handler);
		endpoint.start();
		StringMessage testMessage = new StringMessage(1, "test");
		testMessage.getHeader().setReplyChannel(replyChannel1);
		testMessage.getHeader().setReplyChannelName("replyChannel2");
		endpoint.handle(testMessage);
		endpoint.stop();
		Message<?> reply1 = replyChannel1.receive(50);
		assertNotNull(reply1);
		assertEquals("hello test", reply1.getPayload());
		Message<?> reply2 = replyChannel2.receive(0);
		assertNull(reply2);
	}

	@Test
	public void testCustomErrorHandler() throws InterruptedException {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setConcurrencyPolicy(new ConcurrencyPolicy(1, 1));
		final CountDownLatch latch = new CountDownLatch(2);
		endpoint.setErrorHandler(new ErrorHandler() {
			public void handle(Throwable t) {
				latch.countDown();
			}
		});
		endpoint.setHandler(TestHandlers.rejectingCountDownHandler(latch));
		endpoint.start();
		endpoint.handle(new StringMessage("test"));
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("both handler and errorHandler should have been invoked", 0, latch.getCount());
	}

	@Test
	public void testConcurrentHandlerWithDefaultReplyChannel() throws InterruptedException {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(new ConcurrentHandler(handler, createExecutor()));
		endpoint.setDefaultOutputChannelName("replyChannel");
		endpoint.start();
		endpoint.handle(new StringMessage(1, "test"));
		endpoint.stop();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(100);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testHandlerReturnsNull() throws InterruptedException {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(handler);
		endpoint.setDefaultOutputChannelName("replyChannel");
		endpoint.start();
		endpoint.handle(new StringMessage(1, "test"));
		endpoint.stop();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(0);
		assertNull(reply);
	}

	@Test
	public void testConcurrentHandlerReturnsNull() throws InterruptedException {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(new ConcurrentHandler(handler, createExecutor()));
		endpoint.setDefaultOutputChannelName("replyChannel");
		endpoint.start();
		endpoint.handle(new StringMessage(1, "test"));
		endpoint.stop();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(0);
		assertNull(reply);
	}

	@Test
	public void testConcurrentHandlerWithExplicitReplyChannel() throws InterruptedException {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(new ConcurrentHandler(handler, createExecutor()));
		endpoint.start();
		StringMessage message = new StringMessage(1, "test");
		message.getHeader().setReplyChannelName("replyChannel");
		endpoint.handle(message);
		endpoint.stop();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(100);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testGeneratedConcurrentHandlerWithDefaultReplyChannel() throws InterruptedException {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(handler);
		endpoint.setConcurrencyPolicy(new ConcurrencyPolicy(3, 14));
		endpoint.setDefaultOutputChannelName("replyChannel");
		endpoint.start();
		endpoint.handle(new StringMessage(1, "test"));
		endpoint.stop();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(100);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test
	public void testGeneratedConcurrentHandlerWithExplicitReplyChannel() throws InterruptedException {
		MessageChannel replyChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("replyChannel", replyChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		MessageHandler handler = new MessageHandler() {
			public Message<String> handle(Message<?> message) {
				latch.countDown();
				return new StringMessage("123", "hello " + message.getPayload());
			}
		};
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.setChannelRegistry(channelRegistry);
		endpoint.setHandler(handler);
		endpoint.setConcurrencyPolicy(new ConcurrencyPolicy(3, 14));
		endpoint.start();
		StringMessage message = new StringMessage(1, "test");
		message.getHeader().setReplyChannelName("replyChannel");
		endpoint.handle(message);
		endpoint.stop();
		latch.await(500, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked within allotted time", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(100);
		assertNotNull(reply);
		assertEquals("hello test", reply.getPayload());
	}

	@Test(expected=MessageHandlerNotRunningException.class)
	public void testEndpointDoesNotHandleMessagesWhenNotYetStarted() {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.handle(new StringMessage("test"));
	}

	@Test
	public void testEndpointDoesNotHandleMessagesAfterBeingStopped() {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		AtomicInteger counter = new AtomicInteger();
		boolean exceptionThrown = false;
		try {
			endpoint.start();
			endpoint.setHandler(TestHandlers.countingHandler(counter));
			endpoint.handle(new StringMessage("test1"));
			endpoint.stop();
			endpoint.handle(new StringMessage("test2"));
		}
		catch (MessageHandlerNotRunningException e) {
			exceptionThrown = true;
		}
		assertEquals("handler should have been invoked exactly once", 1, counter.get());
		assertTrue(exceptionThrown);
	}

	@Test(expected=MessageSelectorRejectedException.class)
	public void testEndpointWithSelectorRejecting() {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		endpoint.start();
		endpoint.handle(new StringMessage("test"));
	}

	@Test
	public void testEndpointWithSelectorAccepting() throws InterruptedException {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		});
		CountDownLatch latch = new CountDownLatch(1);
		endpoint.setHandler(TestHandlers.countDownHandler(latch));
		endpoint.start();
		endpoint.handle(new StringMessage("test"));
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked", 0, latch.getCount());
		endpoint.stop();
	}

	@Test
	public void testEndpointWithMultipleSelectorsAndFirstRejects() {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		boolean exceptionThrown = false;
		final AtomicInteger counter = new AtomicInteger();
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return false;
			}
		});
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		endpoint.setHandler(TestHandlers.countingHandler(counter));
		endpoint.start();
		try {
			endpoint.handle(new StringMessage("test"));
		}
		catch (MessageSelectorRejectedException e) {
			exceptionThrown = true;
		}
		assertEquals("only the first selector should have been invoked", 1, counter.get());
		assertTrue(exceptionThrown);
		endpoint.stop();
	}

	@Test
	public void testEndpointWithMultipleSelectorsAndFirstAccepts() {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		boolean exceptionThrown = false;
		final AtomicInteger counter = new AtomicInteger();
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return false;
			}
		});
		endpoint.setHandler(TestHandlers.countingHandler(counter));
		endpoint.start();
		try {
			endpoint.handle(new StringMessage("test"));
		}
		catch (MessageSelectorRejectedException e) {
			exceptionThrown = true;
		}
		assertEquals("both selectors should have been invoked but not the handler", 2, counter.get());
		assertTrue(exceptionThrown);
		endpoint.stop();
	}

	@Test
	public void testEndpointWithMultipleSelectorsAndBothAccept() {
		DefaultMessageEndpoint endpoint = new DefaultMessageEndpoint();
		final AtomicInteger counter = new AtomicInteger();
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		endpoint.addMessageSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				counter.incrementAndGet();
				return true;
			}
		});
		endpoint.setHandler(TestHandlers.countingHandler(counter));
		endpoint.start();
		endpoint.handle(new StringMessage("test"));
		assertEquals("both selectors and handler should have been invoked", 3, counter.get());
		endpoint.stop();
	}


	private static ExecutorService createExecutor() {
		return new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}

}
