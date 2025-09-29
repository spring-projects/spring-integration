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

package org.springframework.integration.cloudevents.transformer.strategies;

import io.cloudevents.CloudEvent;

import org.springframework.integration.cloudevents.CloudEventMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Implementation of {@link FormatStrategy} that converts CloudEvents to Spring
 * Integration messages.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class CloudEventMessageFormatStrategy implements FormatStrategy {

	private final CloudEventMessageConverter messageConverter;

	public CloudEventMessageFormatStrategy() {
		this.messageConverter = new CloudEventMessageConverter("ce-");
	}

	public CloudEventMessageFormatStrategy(String cePrefix) {
		this.messageConverter = new CloudEventMessageConverter(cePrefix);
	}

	/**
	 * Converts the CloudEvent to a Spring Integration Message.
	 *
	 * @param cloudEvent the CloudEvent to convert
	 * @param messageHeaders additional headers to include in the message
	 * @return a Spring Integration Message containing the CloudEvent data and headers
	 */
	@Override
	public Message<?> convert(CloudEvent cloudEvent, MessageHeaders messageHeaders) {
		return this.messageConverter.toMessage(cloudEvent, messageHeaders);
	}

}
