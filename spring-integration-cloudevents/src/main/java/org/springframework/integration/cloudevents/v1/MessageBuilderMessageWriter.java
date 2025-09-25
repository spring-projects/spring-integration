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

package org.springframework.integration.cloudevents.v1;

import java.util.HashMap;
import java.util.Map;

import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Internal utility class for copying <code>CloudEvent</code> context to a map (message
 * headers).
 *
 * @author Dave Syer
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class MessageBuilderMessageWriter
		implements CloudEventWriter<Message<byte[]>>, MessageWriter<MessageBuilderMessageWriter, Message<byte[]>> {

	private final String cePrefix;

	private final Map<String, Object> headers = new HashMap<>();

	public MessageBuilderMessageWriter(Map<String, Object> headers, String cePrefix) {
		this.headers.putAll(headers);
		this.cePrefix = cePrefix;
	}

	public MessageBuilderMessageWriter() {
		this.cePrefix = CloudEventsHeaders.CE_PREFIX;
	}

	@Override
	public Message<byte[]> setEvent(EventFormat format, byte[] value) throws CloudEventRWException {
		this.headers.put(CloudEventsHeaders.CONTENT_TYPE, format.serializedContentType());
		return MessageBuilder.withPayload(value).copyHeaders(this.headers).build();
	}

	@Override
	public Message<byte[]> end(CloudEventData value) throws CloudEventRWException {
		return MessageBuilder.withPayload(value == null ? new byte[0] : value.toBytes()).copyHeaders(this.headers).build();
	}

	@Override
	public Message<byte[]> end() {
		return MessageBuilder.withPayload(new byte[0]).copyHeaders(this.headers).build();
	}

	@Override
	public CloudEventContextWriter withContextAttribute(String name, String value) throws CloudEventRWException {
		this.headers.put(this.cePrefix + name, value);
		return this;
	}

	@Override
	public MessageBuilderMessageWriter create(SpecVersion version) {
		this.headers.put(this.cePrefix + "specversion", version.toString());
		return this;
	}

}
