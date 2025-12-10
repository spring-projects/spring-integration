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

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.v1.CloudEventV1;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CloudEventMessageConverterTests {

	private static final String DEFAULT_PREFIX = "ce-";

	private static final String DEFAULT_SPECVERSION_KEY = DEFAULT_PREFIX + "specversion";

	private static final String DEFAULT_DATACONTENTTYPE_KEY = DEFAULT_PREFIX + "datacontenttype";

	@Test
	void binaryModeContainsAllCloudEventAttributes() {
		CloudEventMessageConverter cloudEventMessageConverter = new CloudEventMessageConverter(DEFAULT_PREFIX,
				DEFAULT_SPECVERSION_KEY, DEFAULT_DATACONTENTTYPE_KEY);
		Message<?> message = MessageBuilder.withPayload(new CloudEventV1("1", URI.create("http://localhost:8080/cloudevents"),
				"sampleType", "text/plain",
				URI.create("http://sample:8080/sample"), "sample subject", OffsetDateTime.now(),
				BytesCloudEventData.wrap(new byte[0]), Collections.emptyMap())).build();
		Message<?> convertedMessage = cloudEventMessageConverter.toMessage(message.getPayload(), message.getHeaders());
		assertThat(convertedMessage.getHeaders()).containsKeys(DEFAULT_DATACONTENTTYPE_KEY, DEFAULT_SPECVERSION_KEY, "ce-time", "ce-id");
	}

	@Test
	void fromMessageThrowsUnsupportedOperation() {
		CloudEventMessageConverter cloudEventMessageConverter = new CloudEventMessageConverter(DEFAULT_PREFIX,
				DEFAULT_SPECVERSION_KEY, DEFAULT_DATACONTENTTYPE_KEY);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				cloudEventMessageConverter.fromMessage(MessageBuilder.withPayload(new byte[0]).build(),
						CloudEvent.class)).withMessage(
				"CloudEventMessageConverter does not support fromMessage method");
	}
}
