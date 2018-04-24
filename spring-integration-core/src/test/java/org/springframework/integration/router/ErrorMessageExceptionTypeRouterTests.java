/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.router;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ErrorMessageExceptionTypeRouterTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final QueueChannel illegalArgumentChannel = new QueueChannel();

	private final QueueChannel runtimeExceptionChannel = new QueueChannel();

	private final QueueChannel messageHandlingExceptionChannel = new QueueChannel();

	private final QueueChannel messageDeliveryExceptionChannel = new QueueChannel();

	private final QueueChannel defaultChannel = new QueueChannel();

	@Before
	public void prepare() {
		beanFactory.registerSingleton("illegalArgumentChannel", illegalArgumentChannel);
		beanFactory.registerSingleton("runtimeExceptionChannel", runtimeExceptionChannel);
		beanFactory.registerSingleton("messageHandlingExceptionChannel", messageHandlingExceptionChannel);
		beanFactory.registerSingleton("messageDeliveryExceptionChannel", messageDeliveryExceptionChannel);
		beanFactory.registerSingleton("defaultChannel", defaultChannel);
	}


	@Test
	public void mostSpecificCause() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		router.setChannelMapping(RuntimeException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void fallbackToNextMostSpecificCause() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(RuntimeException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "runtimeExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(runtimeExceptionChannel.receive(1000));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(defaultChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void fallbackToErrorMessageType() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(messageHandlingExceptionChannel.receive(1000));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void fallbackToDefaultChannel() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(defaultChannel.receive(1000));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void noMatchAndNoDefaultChannel() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(MessageDeliveryException.class.getName(), "messageDeliveryExceptionChannel");
		router.setResolutionRequired(true);
		router.setBeanName("fooRouter");
		router.afterPropertiesSet();

		try {
			router.handleMessage(message);
			fail("MessageDeliveryException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getMessage(), containsString("'fooRouter'"));
		}
	}

	@Test
	public void exceptionPayloadButNotErrorMessage() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		Message<?> message = new GenericMessage<Exception>(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		router.setChannelMapping(RuntimeException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void intermediateCauseHasNoMappingButMostSpecificCauseDoes() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void testHierarchicalMapping() {
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		MessageHandlingException error = new MessageRejectedException(new GenericMessage<Object>("foo"), "failed", rootCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.setChannelMapping(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setDefaultOutputChannel(defaultChannel);
		router.afterPropertiesSet();

		router.handleMessage(message);

		assertNotNull(messageHandlingExceptionChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void testInvalidMapping() {
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		router.setBeanFactory(beanFactory);
		router.setApplicationContext(TestUtils.createTestApplicationContext());
		router.afterPropertiesSet();
		try {
			router.setChannelMapping("foo", "fooChannel");
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getCause(), instanceOf(ClassNotFoundException.class));
		}
	}

	@Test
	public void testLateClassBinding() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		ctx.getBean(ErrorMessageExceptionTypeRouter.class).handleMessage(new GenericMessage<>(new NullPointerException()));
		assertNotNull(ctx.getBean("channel", PollableChannel.class).receive(0));
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
