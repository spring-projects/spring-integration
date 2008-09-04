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

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RootCauseErrorMessageRouterTests {

	private QueueChannel illegalArgumentChannel = new QueueChannel();

	private QueueChannel runtimeExceptionChannel = new QueueChannel();

	private QueueChannel messageHandlingExceptionChannel = new QueueChannel();

	private QueueChannel messageDeliveryExceptionChannel = new QueueChannel();

	private QueueChannel defaultChannel = new QueueChannel();


	@Test
	public void testMostSpecificCause() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		Map<Class<? extends Throwable>, MessageChannel> channelMappings =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		channelMappings.put(IllegalArgumentException.class, illegalArgumentChannel);
		channelMappings.put(RuntimeException.class, runtimeExceptionChannel);
		channelMappings.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		endpoint.send(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void testFallbackToNextMostSpecificCause() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		Map<Class<? extends Throwable>, MessageChannel> channelMappings =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		channelMappings.put(RuntimeException.class, runtimeExceptionChannel);
		channelMappings.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		endpoint.send(message);
		assertNotNull(runtimeExceptionChannel.receive(1000));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(defaultChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void testFallbackToErrorMessageType() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		Map<Class<? extends Throwable>, MessageChannel> channelMappings =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		channelMappings.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		endpoint.send(message);
		assertNotNull(messageHandlingExceptionChannel.receive(1000));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(defaultChannel.receive(0));
	}

	@Test
	public void testFallbackToDefaultChannel() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		endpoint.send(message);
		assertNotNull(defaultChannel.receive(1000));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(illegalArgumentChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test(expected = MessageDeliveryException.class)
	public void testNoMatchAndNoDefaultChannel() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		Map<Class<? extends Throwable>, MessageChannel> channelMappings =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		channelMappings.put(MessageDeliveryException.class, messageDeliveryExceptionChannel);
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setResolutionRequired(true);
		endpoint.send(message);
	}

	@Test
	public void testExceptionPayloadButNotErrorMessage() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		Message<?> message = new GenericMessage<Exception>(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		Map<Class<? extends Throwable>, MessageChannel> channelMappings =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		channelMappings.put(IllegalArgumentException.class, illegalArgumentChannel);
		channelMappings.put(RuntimeException.class, runtimeExceptionChannel);
		channelMappings.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		endpoint.send(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

	@Test
	public void testIntermediateCauseHasNoMappingButMostSpecificCauseDoes() {
		Message<?> failedMessage = new StringMessage("foo");
		IllegalArgumentException rootCause = new IllegalArgumentException("bad argument");
		RuntimeException middleCause = new RuntimeException(rootCause);
		MessageHandlingException error = new MessageHandlingException(failedMessage, "failed", middleCause);
		ErrorMessage message = new ErrorMessage(error);
		RootCauseErrorMessageChannelResolver resolver = new RootCauseErrorMessageChannelResolver();
		Map<Class<? extends Throwable>, MessageChannel> channelMappings =
				new HashMap<Class<? extends Throwable>, MessageChannel>();
		channelMappings.put(IllegalArgumentException.class, illegalArgumentChannel);
		channelMappings.put(MessageHandlingException.class, messageHandlingExceptionChannel);
		resolver.setChannelMappings(channelMappings);
		RouterEndpoint endpoint = new RouterEndpoint(resolver);
		endpoint.setDefaultOutputChannel(defaultChannel);
		endpoint.send(message);
		assertNotNull(illegalArgumentChannel.receive(1000));
		assertNull(defaultChannel.receive(0));
		assertNull(runtimeExceptionChannel.receive(0));
		assertNull(messageHandlingExceptionChannel.receive(0));
	}

}
