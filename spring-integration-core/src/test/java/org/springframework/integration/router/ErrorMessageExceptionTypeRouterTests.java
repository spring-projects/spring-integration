/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.router;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ErrorMessageExceptionTypeRouterTests {

	private final TestUtils.TestApplicationContext context = TestUtils.createTestApplicationContext();

	private final QueueChannel illegalArgumentChannel = new QueueChannel();

	private final QueueChannel runtimeExceptionChannel = new QueueChannel();

	private final QueueChannel messageHandlingExceptionChannel = new QueueChannel();

	private final QueueChannel messageDeliveryExceptionChannel = new QueueChannel();

	private final QueueChannel defaultChannel = new QueueChannel();

	@Before
	public void prepare() {
		this.context.registerBean("illegalArgumentChannel", this.illegalArgumentChannel);
		this.context.registerBean("runtimeExceptionChannel", this.runtimeExceptionChannel);
		this.context.registerBean("messageHandlingExceptionChannel", this.messageHandlingExceptionChannel);
		this.context.registerBean("messageDeliveryExceptionChannel", this.messageDeliveryExceptionChannel);
		this.context.registerBean("defaultChannel", this.defaultChannel);
		this.context.refresh();
	}

	@After
	public void terDown() {
		this.context.close();
	}

	@Test
	public void mostSpecificCause() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(this.context);
		router.setApplicationContext(this.context);
		router.setChannelMapping(RuntimeException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMapping(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(this.defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(illegalArgumentChannel.receive(1000)).isNotNull();
		assertThat(defaultChannel.receive(0)).isNull();
		assertThat(runtimeExceptionChannel.receive(0)).isNull();
		assertThat(messageHandlingExceptionChannel.receive(0)).isNull();
	}

	@Test
	public void fallbackToNextMostSpecificCause() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(this.context);
		router.setApplicationContext(this.context);
		router.setChannelMapping(RuntimeException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(this.defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(runtimeExceptionChannel.receive(1000)).isNotNull();
		assertThat(illegalArgumentChannel.receive(0)).isNull();
		assertThat(defaultChannel.receive(0)).isNull();
		assertThat(messageHandlingExceptionChannel.receive(0)).isNull();
	}

	@Test
	public void fallbackToErrorMessageType() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(this.context);
		router.setApplicationContext(this.context);
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(this.defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(messageHandlingExceptionChannel.receive(1000)).isNotNull();
		assertThat(runtimeExceptionChannel.receive(0)).isNull();
		assertThat(illegalArgumentChannel.receive(0)).isNull();
		assertThat(defaultChannel.receive(0)).isNull();
	}

	@Test
	public void fallbackToDefaultChannel() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setApplicationContext(this.context);
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(defaultChannel.receive(1000)).isNotNull();
		assertThat(runtimeExceptionChannel.receive(0)).isNull();
		assertThat(illegalArgumentChannel.receive(0)).isNull();
		assertThat(messageHandlingExceptionChannel.receive(0)).isNull();
	}

	@Test
	public void noMatchAndNoDefaultChannel() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setApplicationContext(this.context);
		router.setChannelMapping(MessageDeliveryException.class.getName(), "messageDeliveryExceptionChannel");
		router.setResolutionRequired(true);
		router.setBeanName("fooRouter");
		router.afterPropertiesSet();

		try {
			router.handleMessage(message);
			fail("MessageDeliveryException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessageDeliveryException.class);
			assertThat(e.getMessage()).contains("'fooRouter'");
		}
	}

	@Test
	public void exceptionPayloadButNotErrorMessage() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		Message<?> message = new GenericMessage<Exception>(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(this.context);
		router.setApplicationContext(this.context);
		router.setChannelMapping(RuntimeException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMapping(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(this.defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(illegalArgumentChannel.receive(1000)).isNotNull();
		assertThat(defaultChannel.receive(0)).isNull();
		assertThat(runtimeExceptionChannel.receive(0)).isNull();
		assertThat(messageHandlingExceptionChannel.receive(0)).isNull();
	}

	@Test
	public void intermediateCauseHasNoMappingButMostSpecificCauseDoes() {
		Message<?> failedMessage = new GenericMessage<>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(this.context);
		router.setApplicationContext(this.context);
		router.setChannelMapping(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(illegalArgumentChannel.receive(1000)).isNotNull();
		assertThat(defaultChannel.receive(0)).isNull();
		assertThat(runtimeExceptionChannel.receive(0)).isNull();
		assertThat(messageHandlingExceptionChannel.receive(0)).isNull();
	}

	@Test
	public void testHierarchicalMapping() {
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		MessageHandlingException error =
				new MessageRejectedException(new GenericMessage<Object>("foo"), "failed", rootCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(this.context);
		router.setApplicationContext(this.context);
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertThat(messageHandlingExceptionChannel.receive(1000)).isNotNull();
		assertThat(defaultChannel.receive(0)).isNull();
	}

	@Test
	public void testInvalidMapping() {
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setApplicationContext(this.context);
		router.afterPropertiesSet();
		try {
			router.setChannelMapping("foo", "fooChannel");
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(IllegalStateException.class);
			assertThat(e.getCause()).isInstanceOf(ClassNotFoundException.class);
		}
	}

	@Test
	public void testLateClassBinding() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		ctx.getBean(ErrorMessageExceptionTypeRouter.class)
				.handleMessage(new GenericMessage<>(new NullPointerException()));
		assertThat(ctx.getBean("channel", PollableChannel.class).receive(0)).isNotNull();
		ctx.close();
	}

	public static class Config {

		@Bean
		public ErrorMessageExceptionTypeRouter errorMessageExceptionTypeRouter() {
			ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
			router.setChannelMapping(NullPointerException.class.getName(), "channel");
			return router;
		}

		@Bean
		public PollableChannel channel() {
			return new QueueChannel();
		}

	}

}
