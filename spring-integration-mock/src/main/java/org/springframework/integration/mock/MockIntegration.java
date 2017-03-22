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

package org.springframework.integration.mock;

import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;

import org.springframework.integration.core.MessageSource;
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
	 * @param payload the payload to return by mocked {@link MessageSource}
	 * @param <T>  the payload type
	 * @return the mocked {@link MessageSource}
	 */
	@SuppressWarnings("unchecked")
	public static <T> MessageSource<T> mockMessageSource(T payload) {
		return (MessageSource<T>) mockMessageSource(new GenericMessage<>(payload));
	}

	/**
	 * Build a mock for the {@link MessageSource} based on the provided payloads.
	 * @param payload the first payload to return by mocked {@link MessageSource}
	 * @param payloads the next payloads to return by mocked {@link MessageSource}
	 * @param <T>  the payload type
	 * @return the mocked {@link MessageSource}
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
	 * @param message the message to return by mocked {@link MessageSource}
	 * @return the mocked {@link MessageSource}
	 */
	@SuppressWarnings("rawtypes")
	public static MessageSource<?> mockMessageSource(Message<?> message) {
		MessageSource messageSource = Mockito.mock(MessageSource.class);

		given(messageSource.receive())
				.<Message<?>>willReturn(message);

		return messageSource;
	}

	/**
	 * Build a mock for the {@link MessageSource} based on the provided messages.
	 * @param message the first message to return by mocked {@link MessageSource}
	 * @param messages the next messages to return by mocked {@link MessageSource}
	 * @return the mocked {@link MessageSource}
	 */
	@SuppressWarnings("rawtypes")
	public static MessageSource<?> mockMessageSource(Message<?> message, Message<?>... messages) {
		MessageSource messageSource = Mockito.mock(MessageSource.class);

		given(messageSource.receive())
				.willReturn(message, messages);

		return messageSource;
	}

	private MockIntegration() {
	}

}
