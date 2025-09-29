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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Strategy interface for converting CloudEvents to different message formats.
 *
 * <p>Implementations of this interface define how CloudEvents should be transformed
 * into message objects. This allows for pluggable conversion strategies to support different messaging formats and
 * protocols.
 *
 * @author Glenn Renfro
 * @since 7.0
 */
public interface FormatStrategy {

	/**
	 * Converts the {@link CloudEvent} to a message object.
	 *
	 * @param cloudEvent the CloudEvent to be converted to a {@link Message}
	 * @param messageHeaders the headers associated with the {@link Message}
	 * @return the converted {@link Message}
	 */
	Message<?> convert(CloudEvent cloudEvent, MessageHeaders messageHeaders);
}
