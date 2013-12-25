/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ErrorMessageExceptionTypeRouterTests {

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private QueueChannel illegalArgumentChannel = new QueueChannel();

	private QueueChannel runtimeExceptionChannel = new QueueChannel();

	private QueueChannel messageHandlingExceptionChannel = new QueueChannel();

	private QueueChannel messageDeliveryExceptionChannel = new QueueChannel();

	private QueueChannel defaultChannel = new QueueChannel();

	@Before
	public void prepare(){
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
		Map<String, String> exceptionTypeChannelMap = new HashMap<String, String>();
		exceptionTypeChannelMap.put(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		exceptionTypeChannelMap.put(RuntimeException.class.getName(), "runtimeExceptionChannel");
		exceptionTypeChannelMap.put(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setChannelMappings(exceptionTypeChannelMap);

		router.setBeanFactory(beanFactory);

		router.setDefaultOutputChannel(defaultChannel);
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
		Map<String, String> exceptionTypeChannelMap = new HashMap<String, String>();
		exceptionTypeChannelMap.put(RuntimeException.class.getName(), "runtimeExceptionChannel");
		exceptionTypeChannelMap.put(MessageHandlingException.class.getName(), "runtimeExceptionChannel");
		router.setChannelMappings(exceptionTypeChannelMap);
		router.setBeanFactory(beanFactory);

		router.setDefaultOutputChannel(defaultChannel);
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
		Map<String, String> exceptionTypeChannelMap = new HashMap<String, String>();
		exceptionTypeChannelMap.put(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setChannelMappings(exceptionTypeChannelMap);
		router.setBeanFactory(beanFactory);
		router.setDefaultOutputChannel(defaultChannel);
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
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(defaultChannel.receive(1000));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test(expected = MessageDeliveryException.class)
	public void noMatchAndNoDefaultChannel() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<String, String> exceptionTypeChannelMap = new HashMap<String, String>();
		exceptionTypeChannelMap.put(MessageDeliveryException.class.getName(), "messageDeliveryExceptionChannel");
		router.setChannelMappings(exceptionTypeChannelMap);
		router.setBeanFactory(beanFactory);
		router.setResolutionRequired(true);
		router.handleMessage(message);
	}

	@Test
	public void exceptionPayloadButNotErrorMessage() {
		Message<?> failedMessage = new GenericMessage<String>("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		Message<?> message = new GenericMessage<Exception>(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<String, String> exceptionTypeChannelMap = new HashMap<String, String>();
		exceptionTypeChannelMap.put(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		exceptionTypeChannelMap.put(RuntimeException.class.getName(), "runtimeExceptionChannel");
		exceptionTypeChannelMap.put(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setChannelMappings(exceptionTypeChannelMap);
		router.setBeanFactory(beanFactory);
		router.setDefaultOutputChannel(defaultChannel);
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
		Map<String, String> exceptionTypeChannelMap = new HashMap<String, String>();
		exceptionTypeChannelMap.put(IllegalArgumentException.class.getName(), "illegalArgumentChannel");
		exceptionTypeChannelMap.put(MessageHandlingException.class.getName(), "messageHandlingExceptionChannel");
		router.setChannelMappings(exceptionTypeChannelMap);
		router.setBeanFactory(beanFactory);
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

}
