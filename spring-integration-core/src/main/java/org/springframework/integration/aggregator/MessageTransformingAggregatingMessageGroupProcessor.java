/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.messaging.Message;

/**
 * This implementation of MessageGroupProcessor will take the messages from the MessageGroup
 * and pass them on in a single message with a Collection of transformed Messages as a payload.
 *<p>
 * By default, this implementation does not transform the payload of the messages but simply removes
 * {@link IntegrationMessageHeaderAccessor#SEQUENCE_SIZE}, {@link IntegrationMessageHeaderAccessor#SEQUENCE_NUMBER} and
 * {@link IntegrationMessageHeaderAccessor#CORRELATION_ID}.
 *
 * @author Alen Turkovic
 * @since 5.1
 */
public class MessageTransformingAggregatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	private BiFunction<Message<?>, MessageBuilderFactory, Message<?>> projection = (m, factory) -> factory
			.fromMessage(m)
			.removeHeaders(
					IntegrationMessageHeaderAccessor.SEQUENCE_SIZE,
					IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER,
					IntegrationMessageHeaderAccessor.CORRELATION_ID)
			.build();

	/**
	 * Set the projection function used to convert messages when aggregating them. The function receives the {@link Message} to be
	 * converted as well as {@link MessageBuilderFactory} used to build new messages.
	 * @param projection  a {@link BiFunction} used to convert messages.
	 */
	public void setProjection(final BiFunction<Message<?>, MessageBuilderFactory, Message<?>> projection) {
		this.projection = projection;
	}

	/**
	 * This implementation simply returns no common headers since messages are aggregated with their headers.
	 * Subclasses may override this method with more advanced strategies if necessary.
	 *
	 * @param group The message group.
	 * @return The aggregated headers. Empty by default.
	 */
	@Override
	protected Map<String, Object> aggregateHeaders(final MessageGroup group) {
		return Collections.emptyMap();
	}

	@Override
	protected Object aggregatePayloads(final MessageGroup group, final Map<String, Object> headers) {
		return group.getMessages().stream()
				.map(m -> this.projection.apply(m, getMessageBuilderFactory()))
				.collect(Collectors.toList());
	}

}
