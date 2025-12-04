/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.cloudevents.transformer;

import java.util.Objects;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Convert Spring Integration {@link Message}s into CloudEvent messages.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
class CloudEventMessageConverter implements MessageConverter {

	private final String cloudEventPrefix;

	private final String specVersionKey;

	private final String dataContentTypeKey;

	/**
	 * Construct a CloudEventMessageConverter with the specified configuration.
	 * @param cloudEventPrefix the prefix for CloudEvent headers in binary content mode
	 * @param specVersionKey the header name for the specification version
	 * @param dataContentTypeKey the header name for the data content type
	 */
	CloudEventMessageConverter(String cloudEventPrefix, String specVersionKey, String dataContentTypeKey) {
		this.cloudEventPrefix = cloudEventPrefix;
		this.specVersionKey = specVersionKey;
		this.dataContentTypeKey = dataContentTypeKey;
	}

	/**
	 * This converter only supports CloudEvent to Message conversion.
	 * @throws UnsupportedOperationException always, as this operation is not supported
	 */
	@Override
	public @Nullable Object fromMessage(Message<?> message, Class<?> targetClass) {
		throw new UnsupportedOperationException("CloudEventMessageConverter does not support fromMessage method");
	}

	@Override
	public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		if (payload instanceof CloudEvent event) {
			return CloudEventUtils.toReader(event).read(new MessageBuilderMessageWriter(this.cloudEventPrefix,
					this.specVersionKey, this.dataContentTypeKey, Objects.requireNonNull(headers)));
		}
		throw new MessageTransformationException("Unsupported payload type. Should be CloudEvent but was: " +
				payload.getClass());
	}

}
