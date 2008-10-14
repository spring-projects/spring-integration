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

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.TestHandlers;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.message.selector.MessageSelectorChain;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class ServiceActivatorEndpointTests {

	@Test
	public void outputChannel() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = this.createEndpoint();
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.onMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test
	public void outputChannelTakesPrecedence() {
		QueueChannel channel1 = new QueueChannel(1);
		QueueChannel channel2 = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = this.createEndpoint();
		endpoint.setOutputChannel(channel1);
		Message<?> message = MessageBuilder.withPayload("foo").setReturnAddress(channel2).build();
		endpoint.onMessage(message);
		Message<?> reply1 = channel1.receive(0);
		assertNotNull(reply1);
		assertEquals("FOO", reply1.getPayload());
		Message<?> reply2 = channel2.receive(0);
		assertNull(reply2);
	}

	@Test
	public void returnAddressHeader() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").setReturnAddress(channel).build();
		endpoint.onMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test
	public void returnAddressHeaderWithChannelName() {
		QueueChannel channel = new QueueChannel(1);
		channel.setBeanName("testChannel");
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel(channel);
		ServiceActivatingConsumer endpoint = this.createEndpoint();
		endpoint.setChannelResolver(channelResolver);
		Message<?> message = MessageBuilder.withPayload("foo").setReturnAddress("testChannel").build();
		endpoint.onMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test
	public void dynamicReplyChannel() throws Exception {
		final QueueChannel replyChannel1 = new QueueChannel();
		final QueueChannel replyChannel2 = new QueueChannel();
		replyChannel2.setBeanName("replyChannel2");
		Object handler = new Object() {
			@SuppressWarnings("unused")
			public Message<?> handle(Message<?> message) {
				return new StringMessage("foo" + message.getPayload());
			}
		};
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(handler, "handle");
		TestChannelResolver channelResolver = new TestChannelResolver();
		channelResolver.addChannel(replyChannel2);
		endpoint.setChannelResolver(channelResolver);
		Message<String> testMessage1 = MessageBuilder.withPayload("bar")
				.setReturnAddress(replyChannel1).build();
		endpoint.onMessage(testMessage1);
		Message<?> reply1 = replyChannel1.receive(50);
		assertNotNull(reply1);
		assertEquals("foobar", reply1.getPayload());
		Message<?> reply2 = replyChannel2.receive(0);
		assertNull(reply2);
		Message<String> testMessage2 = MessageBuilder.fromMessage(testMessage1)
				.setReturnAddress("replyChannel2").build();
		endpoint.onMessage(testMessage2);
		reply1 = replyChannel1.receive(0);
		assertNull(reply1);
		reply2 = replyChannel2.receive(0);
		assertNotNull(reply2);	
		assertEquals("foobar", reply2.getPayload());	
	}

	@Test
	public void noOutputChannelFallsBackToReturnAddress() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").setReturnAddress(channel).build();
		endpoint.onMessage(message);
		Message<?> reply = channel.receive(0);
		assertNotNull(reply);
		assertEquals("FOO", reply.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void noReplyTarget() {
		ServiceActivatingConsumer endpoint = this.createEndpoint();
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.onMessage(message);
	}

	@Test
	public void noReplyMessage() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				new TestNullReplyBean(), "handle");
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.onMessage(message);
		assertNull(channel.receive(0));
	}

	@Test(expected = MessageHandlingException.class)
	public void noReplyMessageWithRequiresReply() {
		QueueChannel channel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				new TestNullReplyBean(), "handle");
		endpoint.setRequiresReply(true);
		endpoint.setOutputChannel(channel);
		Message<?> message = MessageBuilder.withPayload("foo").build();
		endpoint.onMessage(message);
	}

	@Test(expected=MessageRejectedException.class)
	public void endpointWithSelectorRejecting() {
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				TestHandlers.nullHandler(), "handle");
		endpoint.setSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		endpoint.onMessage(new StringMessage("test"));
	}

	@Test
	public void endpointWithSelectorAccepting() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				TestHandlers.countDownHandler(latch), "handle");
		endpoint.setSelector(new MessageSelector() {
			public boolean accept(Message<?> message) {
				return true;
			}
		});
		endpoint.onMessage(new StringMessage("test"));
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals("handler should have been invoked", 0, latch.getCount());
	}

	@Test
	public void endpointWithMultipleSelectorsAndFirstRejects() {
		final AtomicInteger counter = new AtomicInteger();
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				TestHandlers.countingHandler(counter), "handle");
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
		endpoint.setSelector(selectorChain);
		boolean exceptionWasThrown = false;
		try {
			endpoint.onMessage(new StringMessage("test"));
		}
		catch (MessageRejectedException e) {
			exceptionWasThrown = true;
		}
		assertTrue(exceptionWasThrown);
		assertEquals("only the first selector should have been invoked", 1, counter.get());
	}

	@Test
	public void endpointWithMultipleSelectorsAndFirstAccepts() {
		final AtomicInteger selectorCounter = new AtomicInteger();
		AtomicInteger handlerCounter = new AtomicInteger();
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				TestHandlers.countingHandler(handlerCounter), "handle");
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
		endpoint.setSelector(selectorChain);
		boolean exceptionWasThrown = false;
		try {
			endpoint.onMessage(new StringMessage("test"));
		}
		catch (MessageRejectedException e) {
			exceptionWasThrown = true;
		}
		assertTrue(exceptionWasThrown);
		assertEquals("both selectors should have been invoked", 2, selectorCounter.get());
		assertEquals("the handler should not have been invoked", 0, handlerCounter.get());
	}

	@Test
	public void endpointWithMultipleSelectorsAndBothAccept() {
		final AtomicInteger counter = new AtomicInteger();
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(
				TestHandlers.countingHandler(counter), "handle");
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
		endpoint.setSelector(selectorChain);
		endpoint.onMessage(new StringMessage("test"));
		assertEquals("both selectors and handler should have been invoked", 3, counter.get());
	}

	@Test
	public void correlationIdNotSetIfMessageIsReturnedUnaltered() {
		QueueChannel replyChannel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(new Object() {
			@SuppressWarnings("unused")
			public Message<?> handle(Message<?> message) {
				return message;
			}
		}, "handle");
		Message<String> message = MessageBuilder.withPayload("test")
				.setReturnAddress(replyChannel).build();
		endpoint.onMessage(message);
		Message<?> reply = replyChannel.receive(500);
		assertNull(reply.getHeaders().getCorrelationId());
	}

	@Test
	public void correlationIdSetByHandlerTakesPrecedence() {
		QueueChannel replyChannel = new QueueChannel(1);
		ServiceActivatingConsumer endpoint = new ServiceActivatingConsumer(new Object() {
			@SuppressWarnings("unused")
			public Message<?> handle(Message<?> message) {
				return MessageBuilder.fromMessage(message)
						.setCorrelationId("ABC-123").build();
			}
		}, "handle");
		Message<String> message = MessageBuilder.withPayload("test")
				.setReturnAddress(replyChannel).build();
		endpoint.onMessage(message);
		Message<?> reply = replyChannel.receive(500);
		Object correlationId = reply.getHeaders().getCorrelationId();
		assertFalse(message.getHeaders().getId().equals(correlationId));
		assertEquals("ABC-123", correlationId);
	}


	private ServiceActivatingConsumer createEndpoint() {
		 return new ServiceActivatingConsumer(new TestBean(), "handle");
	}


	private static class TestBean {

		public Message<?> handle(Message<?> message) {
			return new StringMessage(message.getPayload().toString().toUpperCase());
		}
	}


	private static class TestNullReplyBean {

		public Message<?> handle(Message<?> message) {
			return null;
		}
	}

}
