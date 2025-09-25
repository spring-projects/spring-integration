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

package org.springframework.integration.cloudevents.v1.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.cloudevents.CloudEvent;
import io.cloudevents.avro.compact.AvroCompactFormat;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.xml.XMLFormat;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.v1.CloudEventMessageConverter;
import org.springframework.integration.cloudevents.v1.transformer.utils.HeaderPatternMatcher;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

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

	/**
	 * Enumeration of supported CloudEvent conversion types.
	 * <p>
	 * Defines the different output formats supported by the transformer:
	 * <ul>
	 *   <li>DEFAULT - No format conversion, uses standard CloudEvent message structure</li>
	 *   <li>XML - Serializes CloudEvent as XML in the message payload</li>
	 *   <li>JSON - Serializes CloudEvent as JSON in the message payload</li>
	 *   <li>AVRO - Serializes CloudEvent as compact Avro binary in the message payload</li>
	 * </ul>
	 */
	public enum ConversionType { DEFAULT, XML, JSON, AVRO }

	private final MessageConverter messageConverter;

	private final @Nullable String cloudEventExtensionPatterns;

	private final ConversionType conversionType;

	private final CloudEventProperties cloudEventProperties;

	/**
	 * ToCloudEventTransformer Constructor
	 *
	 * @param cloudEventExtensionPatterns comma-delimited patterns for matching headers that should become CloudEvent extensions,
	 * supports wildcards and negation with '!' prefix   If a header matches one of the '!' it is excluded from
	 * cloud event headers and the message headers.   If a header does not match for a prefix or a exclusion, the header
	 * is left in the message headers.   . Null to disable extension mapping.
	 * @param conversionType the output format for the CloudEvent (DEFAULT, XML, JSON, or AVRO)
	 * @param cloudEventProperties configuration properties for CloudEvent metadata (id, source, type, etc.)
	 */
	public ToCloudEventTransformer(@Nullable String cloudEventExtensionPatterns,
			ConversionType conversionType, CloudEventProperties cloudEventProperties) {
		this.messageConverter = new CloudEventMessageConverter(cloudEventProperties.getCePrefix());
		this.cloudEventExtensionPatterns = cloudEventExtensionPatterns;
		this.conversionType = conversionType;
		this.cloudEventProperties = cloudEventProperties;
	}

	public ToCloudEventTransformer() {
		this(null, ConversionType.DEFAULT, new CloudEventProperties());
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

		switch (this.conversionType) {
			case XML:
				return convertToXmlMessage(cloudEvent, message.getHeaders());
			case JSON:
				return convertToJsonMessage(cloudEvent, message.getHeaders());
			case AVRO:
				return convertToAvroMessage(cloudEvent, message.getHeaders());
			default:
				var result = this.messageConverter.toMessage(cloudEvent, filterHeaders(message.getHeaders()));
				Assert.state(result != null, "Payload result must not be null");
				return result;
		}
	}

	private Message<String> convertToXmlMessage(CloudEvent cloudEvent, MessageHeaders originalHeaders) {
		XMLFormat xmlFormat = new XMLFormat();
		String xmlContent = new String(xmlFormat.serialize(cloudEvent));
		return buildStringMessage(xmlContent, originalHeaders, "application/xml");
	}

	private Message<String> convertToJsonMessage(CloudEvent cloudEvent, MessageHeaders originalHeaders) {
		JsonFormat jsonFormat = new JsonFormat();
		String jsonContent = new String(jsonFormat.serialize(cloudEvent));
		return buildStringMessage(jsonContent, originalHeaders, "application/json");
	}

	private Message<String> buildStringMessage(String serializedCloudEvent,
			MessageHeaders originalHeaders, String contentType) {
		try {
			return MessageBuilder.withPayload(serializedCloudEvent)
					.copyHeaders(filterHeaders(originalHeaders))
					.setHeader("content-type", contentType)
					.build();
		}
		catch (Exception e) {
			throw new MessageConversionException("Failed to convert CloudEvent to " + contentType, e);
		}
	}

	private Message<byte[]> convertToAvroMessage(CloudEvent cloudEvent, MessageHeaders originalHeaders) {
		try {
			AvroCompactFormat avroFormat = new AvroCompactFormat();
			byte[] avroBytes = avroFormat.serialize(cloudEvent);
			return MessageBuilder.withPayload(avroBytes)
					.copyHeaders(filterHeaders(originalHeaders))
					.setHeader("content-type", "application/avro")
					.build();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to convert CloudEvent to application/avro", e);
		}
	}

	/**
	 * This method creates a {@link MessageHeaders} that were not placed in the CloudEvent and were not excluded via the
	 * categorization mechanism.
	 * @param headers The {@link MessageHeaders} to be filtered.
	 * @return {@link MessageHeaders} that have been filtered.
	 */
	private MessageHeaders filterHeaders(MessageHeaders headers) {

		Map<String, Object> filteredHeaders = new HashMap<>();
		headers.keySet().forEach(key -> {
			if (HeaderPatternMatcher.categorizeHeader(key, this.cloudEventExtensionPatterns) == null) {
				filteredHeaders.put(key, Objects.requireNonNull(headers.get(key)));
			}
		});
		return new MessageHeaders(filteredHeaders);
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
		return "to-cloud-transformer";
	}

}
