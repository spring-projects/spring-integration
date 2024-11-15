/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Kris Jacyna
 * @author Artem Bilan
 *
 * @since 2.0.1
 */
public class MessageProducerSupportTests {

	private final TestApplicationContext context = TestUtils.createTestApplicationContext();

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test
	public void validateExceptionIfNoErrorChannel() {
		DirectChannel outChannel = new DirectChannel();

		outChannel.subscribe(message -> {
			throw new RuntimeException("problems");
		});
		MessageProducerSupport mps = new MessageProducerSupport() {

		};
		mps.setOutputChannel(outChannel);
		mps.setBeanFactory(this.context);
		mps.afterPropertiesSet();
		mps.start();
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> mps.sendMessage(new GenericMessage<>("hello")));
	}

	@Test
	public void validateExceptionIfSendToErrorChannelFails() {
		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(message -> {
			throw new RuntimeException("problems");
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		errorChannel.subscribe(message -> {
			throw new RuntimeException("ooops");
		});
		MessageProducerSupport mps = new MessageProducerSupport() {

		};
		mps.setOutputChannel(outChannel);
		mps.setErrorChannel(errorChannel);
		mps.setBeanFactory(this.context);
		mps.afterPropertiesSet();
		mps.start();
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> mps.sendMessage(new GenericMessage<>("hello")));
	}

	@Test
	public void validateSuccessfulErrorFlowDoesNotThrowErrors() {
		this.context.refresh();
		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(message -> {
			throw new RuntimeException("problems");
		});
		PublishSubscribeChannel errorChannel = new PublishSubscribeChannel();
		SuccessfulErrorService errorService = new SuccessfulErrorService();
		ServiceActivatingHandler handler = new ServiceActivatingHandler(errorService);
		handler.setBeanFactory(this.context);
		handler.afterPropertiesSet();
		errorChannel.subscribe(handler);
		MessageProducerSupport mps = new MessageProducerSupport() {

		};
		mps.setOutputChannel(outChannel);
		mps.setErrorChannel(errorChannel);
		mps.setBeanFactory(this.context);
		mps.afterPropertiesSet();
		mps.start();
		Message<?> message = new GenericMessage<>("hello");
		mps.sendMessage(message);
		assertThat(errorService.lastMessage).isInstanceOf(ErrorMessage.class);
		ErrorMessage errorMessage = (ErrorMessage) errorService.lastMessage;
		assertThat(errorMessage.getPayload().getClass()).isEqualTo(MessageDeliveryException.class);
		MessageDeliveryException exception = (MessageDeliveryException) errorMessage.getPayload();
		assertThat(exception.getFailedMessage()).isEqualTo(message);
	}

	@Test
	public void testWithChannelName() {
		DirectChannel outChannel = new DirectChannel();
		MessageProducerSupport mps = new MessageProducerSupport() {

		};
		mps.setOutputChannelName("foo");
		this.context.registerBean("foo", outChannel);
		this.context.refresh();
		mps.setBeanFactory(this.context);
		mps.afterPropertiesSet();
		mps.start();
		assertThat(mps.getOutputChannel()).isSameAs(outChannel);
	}

	@Test
	public void customDoStop() {
		final CustomEndpoint endpoint = new CustomEndpoint();
		assertThat(endpoint.getCount()).isEqualTo(0);
		assertThat(endpoint.isStopped()).isTrue();
		endpoint.start();
		assertThat(endpoint.isStopped()).isFalse();
		endpoint.stop(() -> {
			// Do nothing
		});
		assertThat(endpoint.getCount()).isEqualTo(1);
		assertThat(endpoint.isStopped()).isTrue();
	}

	private static class SuccessfulErrorService {

		private volatile Message<?> lastMessage;

		SuccessfulErrorService() {
			super();
		}

		@SuppressWarnings("unused")
		public void handleErrorMessage(Message<?> errorMessage) {
			this.lastMessage = errorMessage;
		}

	}

	private static class CustomEndpoint extends AbstractEndpoint {

		private final AtomicInteger count = new AtomicInteger(0);

		private final AtomicBoolean stopped = new AtomicBoolean(true);

		CustomEndpoint() {
			super();
		}

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
