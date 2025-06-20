/*
 * Copyright 2019-present the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.util.Assert;

/**
 * Base class for {@link ReactiveMessageHandler} implementations.
 *
 * @author David Turanski
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 5.3
 */
public abstract class AbstractReactiveMessageHandler extends MessageHandlerSupport
		implements ReactiveMessageHandler {

	@Override
	public Mono<Void> handleMessage(final Message<?> message) {
		Assert.notNull(message, "message must not be null");
		if (isLoggingEnabled()) {
			this.logger.debug(LogMessage.format("%s received message: %s", this, message));
		}

		final Message<?> messageToUse;
		if (shouldTrack()) {
			messageToUse = MessageHistory.write(message, this, getMessageBuilderFactory());
		}
		else {
			messageToUse = message;
		}
		return handleMessageInternal(messageToUse)
				.doOnError((ex) -> this.logger.error(ex,
						LogMessage.format("An error occurred in message handler [%s] on message [%s]", this, messageToUse)));
	}

	protected abstract Mono<Void> handleMessageInternal(Message<?> message);

}
