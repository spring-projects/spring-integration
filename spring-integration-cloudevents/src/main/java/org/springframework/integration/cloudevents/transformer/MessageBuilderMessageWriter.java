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

import java.util.HashMap;
import java.util.Map;

import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Adapt CloudEvents to Spring Integration {@link Message}s using the CloudEvents SDK
 * {@link MessageWriter} abstraction.
 * Write CloudEvent attributes as message headers with a configurable prefix for
 * binary content mode serialization. Used internally by {@link CloudEventMessageConverter}
 * to convert CloudEvent objects into Spring Integration messages.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 *
 * @see CloudEventMessageConverter
 */
class MessageBuilderMessageWriter
		implements CloudEventWriter<Message<byte[]>>, MessageWriter<MessageBuilderMessageWriter, Message<byte[]>> {

	private final String cloudEventPrefix;

	private final String specVersionKey;

	private final String dataContentTypeKey;

	private final Map<String, Object> headers = new HashMap<>();

	/**
	 * Construct a MessageBuilderMessageWriter with the specified configuration.
	 * @param cloudEventPrefix the prefix to prepend to CloudEvent attribute names in message headers
	 * @param specVersionKey the header name for the CloudEvent specification version
	 * @param dataContentTypeKey the header name for the data content type
	 * @param headers the base message headers to include in the output message
	 */
	MessageBuilderMessageWriter(String cloudEventPrefix, String specVersionKey, String dataContentTypeKey, Map<String, Object> headers) {
		this.headers.putAll(headers);
		this.cloudEventPrefix = cloudEventPrefix;
		this.specVersionKey = specVersionKey;
		this.dataContentTypeKey = dataContentTypeKey;
	}

	/**
	 * Set the event in structured content mode.
	 * Create a message with the serialized CloudEvent as the payload and set the
	 * data content type header to the format's serialized content type.
	 * @param format the event format used to serialize the CloudEvent
	 * @param value the serialized CloudEvent bytes
	 * @return the Spring Integration message containing the serialized CloudEvent
	 * @throws CloudEventRWException if an error occurs during message creation
	 */
	@Override
	public Message<byte[]> setEvent(EventFormat format, byte[] value) throws CloudEventRWException {
		this.headers.put(this.dataContentTypeKey, format.serializedContentType());
		return MessageBuilder.withPayload(value).copyHeaders(this.headers).build();
	}

	/**
	 * Complete the message creation with CloudEvent data.
	 * Create a message with the CloudEvent data as the payload. CloudEvent attributes
	 * are already set as headers via {@link #withContextAttribute(String, String)}.
	 * @param value the CloudEvent data to use as the message payload
	 * @return the Spring Integration message with CloudEvent data and attributes
	 * @throws CloudEventRWException if an error occurs during message creation
	 */
	@Override
	public Message<byte[]> end(CloudEventData value) throws CloudEventRWException {
		return MessageBuilder.withPayload(value.toBytes()).copyHeaders(this.headers).build();
	}

	/**
	 * Complete the message creation without CloudEvent data.
	 * Create a message with an empty payload when the CloudEvent contains no data.
	 * CloudEvent attributes are set as headers via {@link #withContextAttribute(String, String)}.
	 * @return the Spring Integration message with an empty payload and CloudEvent attributes as headers
	 */
	@Override
	public Message<byte[]> end() {
		return MessageBuilder.withPayload(new byte[0]).copyHeaders(this.headers).build();
	}

	/**
	 * Add a CloudEvent context attribute to the message headers.
	 * Map the CloudEvent attribute to a message header by prepending the configured prefix
	 * to the attribute name (e.g., "id" becomes "ce-id" with default prefix).
	 * @param name the CloudEvent attribute name
	 * @param value the CloudEvent attribute value
	 * @return this writer for method chaining
	 * @throws CloudEventRWException if an error occurs while setting the attribute
	 */
	@Override
	public CloudEventContextWriter withContextAttribute(String name, String value) throws CloudEventRWException {
		this.headers.put(this.cloudEventPrefix + name, value);
		return this;
	}

	/**
	 * Initialize the writer with the CloudEvent specification version.
	 * Set the specification version as a message header using the configured version key.
	 * @param version the CloudEvent specification version
	 * @return this writer for method chaining
	 */
	@Override
	public MessageBuilderMessageWriter create(SpecVersion version) {
		this.headers.put(this.specVersionKey, version.toString());
		return this;
	}

}
