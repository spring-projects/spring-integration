/*
 * Copyright 2019 the original author or authors.
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

import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

/**
 * Base class for {@link ReactiveMessageHandler} implementations.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 5.3
 */
public abstract class AbstractReactiveMessageHandler extends MessageHandlerSupport
		implements ReactiveMessageHandler {

	@Override
	public Mono<Void> handleMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		if (isLoggingEnabled() && this.logger.isDebugEnabled()) {
			this.logger.debug(this + " received message: " + message);
		}

		if (shouldTrack()) {
			message = MessageHistory.write(message, this, getMessageBuilderFactory());
		}
		final Message<?> msg = message;
		return handleMessageInternal(msg)
				.doOnError(e -> this.logger.error(
						"An error occurred in message handler [" + this + "] on message [" + msg + "]", e));
	}

	protected abstract Mono<Void> handleMessageInternal(Message<?> message);

}
