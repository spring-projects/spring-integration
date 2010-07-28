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

package org.springframework.integration.router;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.ErrorMessage;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageDeliveryException;
import org.springframework.integration.core.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ErrorMessageExceptionTypeRouterTests {

	private QueueChannel illegalArgumentChannel = new QueueChannel();

	private QueueChannel runtimeExceptionChannel = new QueueChannel();

	private QueueChannel messageHandlingExceptionChannel = new QueueChannel();

	private QueueChannel messageDeliveryExceptionChannel = new QueueChannel();

	private QueueChannel defaultChannel = new QueueChannel();


	@Test
	public void mostSpecificCause() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<Class<? extends Throwable>, MessageChannel> exceptionTypeChannelMap =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		exceptionTypeChannelMap.put(IllegalArgumentException.class, illegalArgumentChannel);
		exceptionTypeChannelMap.put(RuntimeException.class, runtimeExceptionChannel);
		exceptionTypeChannelMap.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		router.setExceptionTypeChannelMap(exceptionTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void fallbackToNextMostSpecificCause() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<Class<? extends Throwable>, MessageChannel> exceptionTypeChannelMap =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		exceptionTypeChannelMap.put(RuntimeException.class, runtimeExceptionChannel);
		exceptionTypeChannelMap.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		router.setExceptionTypeChannelMap(exceptionTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(runtimeExceptionChannel.receive(1000));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(defaultChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void fallbackToErrorMessageType() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<Class<? extends Throwable>, MessageChannel> exceptionTypeChannelMap =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		exceptionTypeChannelMap.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		router.setExceptionTypeChannelMap(exceptionTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(messageHandlingExceptionChannel.receive(1000));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void fallbackToDefaultChannel() {
		Message<?> failedMessage = new StringMessage("foo");
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
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<Class<? extends Throwable>, MessageChannel> exceptionTypeChannelMap =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		exceptionTypeChannelMap.put(MessageDeliveryException.class, messageDeliveryExceptionChannel);
		router.setExceptionTypeChannelMap(exceptionTypeChannelMap);
		router.setResolutionRequired(true);
		router.handleMessage(message);
	}

	@Test
	public void exceptionPayloadButNotErrorMessage() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		Message<?> message = new GenericMessage<Exception>(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<Class<? extends Throwable>, MessageChannel> exceptionTypeChannelMap =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		exceptionTypeChannelMap.put(IllegalArgumentException.class, illegalArgumentChannel);
		exceptionTypeChannelMap.put(RuntimeException.class, runtimeExceptionChannel);
		exceptionTypeChannelMap.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		router.setExceptionTypeChannelMap(exceptionTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void intermediateCauseHasNoMappingButMostSpecificCauseDoes() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		ErrorMessageExceptionTypeRouter router = new ErrorMessageExceptionTypeRouter();
		Map<Class<? extends Throwable>, MessageChannel> exceptionTypeChannelMap =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		exceptionTypeChannelMap.put(IllegalArgumentException.class, illegalArgumentChannel);
		exceptionTypeChannelMap.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		router.setExceptionTypeChannelMap(exceptionTypeChannelMap);
		router.setDefaultOutputChannel(defaultChannel);
		router.handleMessage(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

}
