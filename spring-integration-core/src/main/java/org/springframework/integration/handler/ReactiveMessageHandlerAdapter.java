/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.handler;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation to adapt a {@link ReactiveMessageHandler}
 * for synchronous invocations.
 * A subscription to the returned reactive type from {@link ReactiveMessageHandler#handleMessage(Message)}
 * call is done directly in the {@link #handleMessage} implementation.
 * <p>
 * The framework wraps a target {@link ReactiveMessageHandler} into this instance automatically
 * for XML and Annotation configuration. For Java DSL it is recommended to wrap for generic usage
 * ({@code .handle(MessageHandle)}) or it has to be done in the
 * {@link org.springframework.integration.dsl.MessageHandlerSpec}
 * implementation for protocol-specif {@link ReactiveMessageHandler}.
 * <p>
 * The framework unwraps a delegate {@link ReactiveMessageHandler} whenever it can compose
 * reactive streams, e.g. {@link org.springframework.integration.endpoint.ReactiveStreamsConsumer}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 *
 * @see org.springframework.integration.endpoint.ReactiveStreamsConsumer
 */
public class ReactiveMessageHandlerAdapter implements MessageHandler {

	private final ReactiveMessageHandler delegate;

	/**
	 * Instantiate based on the provided {@link ReactiveMessageHandler}.
	 * @param reactiveMessageHandler the {@link ReactiveMessageHandler} to delegate to.
	 */
	public ReactiveMessageHandlerAdapter(ReactiveMessageHandler reactiveMessageHandler) {
		Assert.notNull(reactiveMessageHandler, "'reactiveMessageHandler' must not be null");
		this.delegate = reactiveMessageHandler;
	}

	/**
	 * Get access to the delegate {@link ReactiveMessageHandler}.
	 * Typically used in the framework internally in components which can compose reactive streams internally
	 * and allow us to avoid an explicit {@code subscribe()} call.
	 * @return the {@link ReactiveMessageHandler} this instance is delegating to.
	 */
	public ReactiveMessageHandler getDelegate() {
		return this.delegate;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		this.delegate.handleMessage(message).subscribe();
	}

}
