/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.integration.handler;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class AsyncHandlerTests {

	private AbstractReplyProducingMessageHandler handler;

	private CountDownLatch latch;

	private int fail;

	private final QueueChannel output = new QueueChannel();

	@Before
	public void setup() {
		this.handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				final SettableListenableFuture<String> future = new SettableListenableFuture<String>();
				Executors.newSingleThreadExecutor().execute(new Runnable() {

					@Override
					public void run() {
						try {
							latch.await(10, TimeUnit.SECONDS);
							switch (fail) {
								case 0:
									future.set("reply");
									break;
								case 1:
									future.setException(new RuntimeException("foo"));
									break;
								case 2:
									future.setException(new MessagingException(requestMessage));
							}
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}

				});
				return future;
			}

		};
		this.handler.setAsyncReplySupported(true);
		this.handler.setOutputChannel(this.output);
		this.latch = new CountDownLatch(1);
	}

	@Test
	public void testGoodResult() {
		this.handler.handleMessage(new GenericMessage<String>("foo"));
		assertNull(this.output.receive(0));
		this.latch.countDown();
		Message<?> received = this.output.receive(10000);
		assertNotNull(received);
		assertEquals("reply", received.getPayload());
	}

	@Test
	public void testRuntimeException() {
		GenericMessage<String> message = new GenericMessage<String>("foo");
		this.handler.handleMessage(message);
		assertNull(this.output.receive(0));
		this.fail = 1;
		this.latch.countDown();
		Message<?> received = this.output.receive(10000);
		assertNotNull(received);
		assertThat(received.getPayload(), instanceOf(MessageHandlingException.class));
		assertEquals("foo", ((Throwable) received.getPayload()).getCause().getMessage());
		assertSame(message, ((MessagingException) received.getPayload()).getFailedMessage());
	}

	@Test
	public void testMessagingException() {
		GenericMessage<String> message = new GenericMessage<String>("foo");
		this.handler.handleMessage(message);
		assertNull(this.output.receive(0));
		this.fail = 2;
		this.latch.countDown();
		Message<?> received = this.output.receive(10000);
		assertNotNull(received);
		assertThat(received.getPayload(), instanceOf(MessagingException.class));
		assertSame(message, ((MessagingException) received.getPayload()).getFailedMessage());
	}

}
