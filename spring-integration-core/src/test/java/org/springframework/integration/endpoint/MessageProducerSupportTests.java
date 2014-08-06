/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Kris Jacyna
 * @since 2.0.1
 */
public class MessageProducerSupportTests {

	@Test(expected=MessageDeliveryException.class)
	public void validateExceptionIfNoErrorChannel() {
		DirectChannel outChannel = new DirectChannel();

		outChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("problems");
			}
		});
		MessageProducerSupport mps = new MessageProducerSupport() {};
		mps.setOutputChannel(outChannel);
		mps.setBeanFactory(TestUtils.createTestApplicationContext());
		mps.afterPropertiesSet();
		mps.start();
		mps.sendMessage(new GenericMessage<String>("hello"));
	}

	@Test(expected=MessageDeliveryException.class)
	public void validateExceptionIfSendToErrorChannelFails() {
		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("problems");
			}
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		errorChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("ooops");
			}
		});
		MessageProducerSupport mps = new MessageProducerSupport() {};
		mps.setOutputChannel(outChannel);
		mps.setErrorChannel(errorChannel);
		mps.setBeanFactory(TestUtils.createTestApplicationContext());
		mps.afterPropertiesSet();
		mps.start();
		mps.sendMessage(new GenericMessage<String>("hello"));
	}

	@Test
	public void validateSuccessfulErrorFlowDoesNotThrowErrors() {
		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("problems");
			}
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		SuccessfulErrorService errorService = new SuccessfulErrorService();
		ServiceActivatingHandler handler  = new ServiceActivatingHandler(errorService);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		errorChannel.subscribe(handler);
		MessageProducerSupport mps = new MessageProducerSupport() {};
		mps.setOutputChannel(outChannel);
		mps.setErrorChannel(errorChannel);
		mps.setBeanFactory(TestUtils.createTestApplicationContext());
		mps.afterPropertiesSet();
		mps.start();
		Message<?> message = new GenericMessage<String>("hello");
		mps.sendMessage(message);
		assertEquals(ErrorMessage.class, errorService.lastMessage.getClass());
		ErrorMessage errorMessage = (ErrorMessage) errorService.lastMessage;
		assertEquals(MessageDeliveryException.class, errorMessage.getPayload().getClass());
		MessageDeliveryException exception = (MessageDeliveryException) errorMessage.getPayload();
		assertEquals(message, exception.getFailedMessage());
	}

	@Test
	public void customDoStop() {
		final CustomEndpoint endpoint = new CustomEndpoint();
		assertEquals(0, endpoint.getCount());
		assertTrue(endpoint.isStopped());
		endpoint.start();
		assertFalse(endpoint.isStopped());
		endpoint.stop(new Runnable() {
			@Override
			public void run() {
				// Do nothing
			}
		});
		assertEquals(1, endpoint.getCount());
		assertTrue(endpoint.isStopped());
	}

	private static class SuccessfulErrorService {

		private volatile Message<?> lastMessage;

		@SuppressWarnings("unused")
		public void handleErrorMessage(Message<?> errorMessage) {
			this.lastMessage = errorMessage;
		}
	}

	private static class CustomEndpoint extends AbstractEndpoint {

		private final AtomicInteger count = new AtomicInteger(0);
		private final AtomicBoolean stopped = new AtomicBoolean(true);

		public int getCount() {
			return this.count.get();
		}

		public boolean isStopped() {
			return this.stopped.get();
		}

		@Override
		protected void doStop(final Runnable callback) {
			this.count.incrementAndGet();
			super.doStop(callback);
		}

		@Override
		protected void doStart() {
			this.stopped.set(false);
		}

		@Override
		protected void doStop() {
			this.stopped.set(true);
		}

	}

}
