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

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.CloudEventMessageConverter;
import org.springframework.integration.cloudevents.CloudEventsHeaders;
import org.springframework.integration.cloudevents.transformer.strategies.CloudEventMessageFormatStrategy;
import org.springframework.integration.cloudevents.transformer.strategies.FormatStrategy;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;

/**
 * A Spring Integration transformer that converts messages to CloudEvent format.
 * <p>
 * This transformer converts Spring Integration messages into CloudEvent compliant
 * messages, supporting various output formats including structured, XML, JSON, and Avro.
 * It handles CloudEvent extensions through configurable header pattern matching and provides
 * configuration through {@link CloudEventProperties}.
 * <p>
 * The transformer supports the following conversion types:
 * <ul>
 *   <li>DEFAULT - Standard CloudEvent message</li>
 *   <li>XML - CloudEvent serialized as XML content</li>
 *   <li>JSON - CloudEvent serialized as JSON content</li>
 *   <li>AVRO - CloudEvent serialized as Avro binary content</li>
 * </ul>
 * <p>
 * Header filtering and extension mapping is performed based on configurable patterns,
 * allowing control over which headers are preserved and which become CloudEvent extensions.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	private final MessageConverter messageConverter;

	private final @Nullable String cloudEventExtensionPatterns;

	private final FormatStrategy formatStrategy;

	private final CloudEventProperties cloudEventProperties;

	/**
	 * ToCloudEventTransformer Constructor
	 *
	 * @param cloudEventExtensionPatterns comma-delimited patterns for matching headers that should become CloudEvent extensions,
	 * supports wildcards and negation with '!' prefix   If a header matches one of the '!' it is excluded from
	 * cloud event headers and the message headers.   If a header does not match for a prefix or a exclusion, the header
	 * is left in the message headers.   . Null to disable extension mapping.
	 * @param formatStrategy The strategy that determines how the CloudEvent will be rendered
	 * @param cloudEventProperties configuration properties for CloudEvent metadata (id, source, type, etc.)
	 */
	public ToCloudEventTransformer(@Nullable String cloudEventExtensionPatterns,
			FormatStrategy formatStrategy, CloudEventProperties cloudEventProperties) {
		this.messageConverter = new CloudEventMessageConverter(cloudEventProperties.getCePrefix());
		this.cloudEventExtensionPatterns = cloudEventExtensionPatterns;
		this.formatStrategy = formatStrategy;
		this.cloudEventProperties = cloudEventProperties;
	}

	public ToCloudEventTransformer() {
		this(null, new CloudEventMessageFormatStrategy(CloudEventsHeaders.CE_PREFIX),
				new CloudEventProperties());
	}

	/**
	 * Transforms the input message into a CloudEvent message.
	 * <p>
	 * This method performs the core transformation logic:
	 * <ol>
	 *   <li>Extracts CloudEvent extensions from message headers using configured patterns</li>
	 *   <li>Builds a CloudEvent with the configured properties and message payload</li>
	 *   <li>Applies the specified conversion type to format the output</li>
	 *   <li>Filters headers to exclude those mapped to CloudEvent extensions</li>
	 * </ol>
	 *
	 * @param message the input Spring Integration message to transform
	 * @return transformed message as CloudEvent in the specified format
	 * @throws RuntimeException if serialization fails for XML, JSON, or Avro formats
	 */
	@Override
	protected Object doTransform(Message<?> message) {
		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(message.getHeaders(), this.cloudEventExtensionPatterns);
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId(this.cloudEventProperties.getId())
				.withSource(this.cloudEventProperties.getSource())
				.withType(this.cloudEventProperties.getType())
				.withTime(this.cloudEventProperties.getTime())
				.withDataContentType(this.cloudEventProperties.getDataContentType())
				.withDataSchema(this.cloudEventProperties.getDataSchema())
				.withSubject(this.cloudEventProperties.getSubject())
				.withData(getPayloadAsBytes(message.getPayload()))
				.withExtension(extensions)
				.build();
				return this.formatStrategy.convert(cloudEvent, new MessageHeaders(extensions.getFilteredHeaders()));
	}

	private byte[] getPayloadAsBytes(Object payload) {
		if (payload instanceof byte[] bytePayload) {
			return bytePayload;
		}
		else if (payload instanceof String stringPayload) {
			return stringPayload.getBytes();
		}
		else {
			return payload.toString().getBytes();
		}
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevents-transformer";
	}

}
