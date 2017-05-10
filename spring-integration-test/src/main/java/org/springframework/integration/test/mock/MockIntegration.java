/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.test.mock;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.test.matcher.PayloadAndHeaderMatcher;
import org.springframework.integration.test.matcher.PayloadMatcher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * The factory for integration specific mock components.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class MockIntegration {

	/**
	 * Build a mock for the {@link MessageSource} based on the provided payload.
	 * The returned instance is ordinary Mockito mock that is capable of
	 * recording interactions with it and further verification.
	 * @param payload the payload to return by mocked {@link MessageSource}
	 * @param <T>  the payload type
	 * @return the mocked {@link MessageSource}
	 * @see Mockito#mock(Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> MessageSource<T> mockMessageSource(T payload) {
		return (MessageSource<T>) mockMessageSource(new GenericMessage<>(payload));
	}

	/**
	 * Build a mock for the {@link MessageSource} based on the provided payloads.
	 * The returned instance is ordinary Mockito mock that is capable of
	 * recording interactions with it and further verification.
	 * @param payload the first payload to return by mocked {@link MessageSource}
	 * @param payloads the next payloads to return by mocked {@link MessageSource}
	 * @param <T>  the payload type
	 * @return the mocked {@link MessageSource}
	 * @see Mockito#mock(Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> MessageSource<T> mockMessageSource(T payload, T... payloads) {
		List<Message<T>> messages = null;

		if (payloads != null) {
			messages = new ArrayList<>(payloads.length);
			for (T p : payloads) {
				messages.add(new GenericMessage<>(p));
			}
		}

		return (MessageSource<T>) mockMessageSource(new GenericMessage<>(payload),
				(messages != null
						? messages.toArray(new Message<?>[messages.size()])
						: null));
	}

	/**
	 * Build a mock for the {@link MessageSource} based on the provided message.
	 * The returned instance is ordinary Mockito mock that is capable of
	 * recording interactions with it and further verification.
	 * @param message the message to return by mocked {@link MessageSource}
	 * @return the mocked {@link MessageSource}
	 * @see Mockito#mock(Class)
	 */
	public static MessageSource<?> mockMessageSource(Message<?> message) {
		MessageSource messageSource = Mockito.mock(MessageSource.class);

		BDDMockito.given(messageSource.receive())
				.willReturn(message);

		return messageSource;
	}

	/**
	 * Build a mock for the {@link MessageSource} based on the provided messages.
	 * The returned instance is ordinary Mockito mock that is capable of
	 * recording interactions with it and further verification.
	 * @param message the first message to return by mocked {@link MessageSource}
	 * @param messages the next messages to return by mocked {@link MessageSource}
	 * @return the mocked {@link MessageSource}
	 * @see Mockito#mock(Class)
	 */
	public static MessageSource<?> mockMessageSource(Message<?> message, Message<?>... messages) {
		MessageSource messageSource = Mockito.mock(MessageSource.class);

		BDDMockito.given(messageSource.receive())
				.willReturn(message, messages);

		return messageSource;
	}

	/**
	 * Build a Mockito spy over the {@link MockMessageHandler} instance
	 * based on the provided payload to assert incoming messages.
	 * @param payload the to assert incoming messages
	 * @return the MockMessageHandler instance ready for interaction
	 * @see Mockito#spy(Object)
	 * @see MockMessageHandler
	 */
	public static MockMessageHandler.MockMessageHandlerWithReply mockMessageHandler(Object payload) {
		return mockMessageHandler(PayloadMatcher.hasPayload(payload));
	}

	/**
	 * Build a Mockito spy over the {@link MockMessageHandler} instance
	 * based on the provided message to assert incoming messages.
	 * @param message the to assert incoming messages
	 * @return the MockMessageHandler instance ready for interaction
	 * @see Mockito#spy(Object)
	 * @see MockMessageHandler
	 * @see PayloadAndHeaderMatcher
	 */
	public static MockMessageHandler.MockMessageHandlerWithReply mockMessageHandler(Message<?> message) {
		return mockMessageHandler(PayloadAndHeaderMatcher.sameExceptIgnorableHeaders(message));
	}

	/**
	 * Build a Mockito spy over the {@link MockMessageHandler} instance
	 * based on the provided message to assert incoming messages.
	 * @param message the to assert incoming messages
	 * @param headersToIgnore the headers to ignore during assertion
	 * @return the MockMessageHandler instance ready for interaction
	 * @see Mockito#spy(Object)
	 * @see MockMessageHandler
	 * @see PayloadAndHeaderMatcher
	 */
	public static MockMessageHandler.MockMessageHandlerWithReply mockMessageHandler(Message<?> message,
			String... headersToIgnore) {
		return mockMessageHandler(PayloadAndHeaderMatcher.sameExceptIgnorableHeaders(message, headersToIgnore));
	}

	/**
	 * Build a Mockito spy over the {@link MockMessageHandler} instance
	 * based on the provided message to assert incoming messages.
	 * @param matcher the matcher to assert incoming messages
	 * @return the MockMessageHandler instance ready for interaction
	 * @see Mockito#spy(Object)
	 * @see MockMessageHandler
	 */
	public static MockMessageHandler.MockMessageHandlerWithReply mockMessageHandler(Matcher<Message<?>> matcher) {
		return Mockito.spy(new MockMessageHandler.MockMessageHandlerWithReply(matcher));
	}

	private MockIntegration() {
	}

}
