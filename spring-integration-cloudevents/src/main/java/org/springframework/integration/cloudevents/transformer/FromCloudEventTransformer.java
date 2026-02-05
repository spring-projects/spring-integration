/*
 * Copyright 2026-present the original author or authors.
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

import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Transform CloudEvent format messages to Spring Integration messages.
 * <p>This transformer supports two payload types:
 * <ul>
 *   <li><b>CloudEvent Object Type:</b> When the message payload is a {@link CloudEvent} instance,
 *   the transformer extracts the CloudEvent data from the message payload and maps CloudEvent
 *   attributes to message headers using {@link CloudEventHeaders} constants. CloudEvent
 *   extensions are also mapped to message headers with the {@value CloudEventHeaders#PREFIX} prefix.</li>
 *   <li><b>Serialized CloudEvent Type:</b> When the message payload is a {@code byte[]} containing
 *   a serialized CloudEvent (e.g., JSON, XML), the transformer uses the {@link MessageHeaders#CONTENT_TYPE}
 *   header to resolve an {@link EventFormat} via {@link EventFormatProvider}. The CloudEvent is
 *   deserialized, and the data is extracted from the {@link CloudEvent} with its attributes mapped to headers.</li>
 * </ul>
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 *
 * @see ToCloudEventTransformer
 * @see CloudEventHeaders
 * @see io.cloudevents.CloudEvent
 * @see io.cloudevents.core.format.EventFormat
 */
public class FromCloudEventTransformer extends AbstractTransformer {

	@Override
	protected Object doTransform(Message<?> message) {

		Assert.state(message.getPayload() instanceof byte[] || message.getPayload() instanceof CloudEvent,
				"Payload did not contain CloudEvent nor could it be deserialized to a CloudEvent");
		CloudEvent cloudEvent;

		if (message.getPayload() instanceof byte[] payload) {
			MimeType mimeType = StaticMessageHeaderAccessor.getContentType(message);
			if (mimeType == null) {
				throw new MessageHandlingException(message, "No Content-Type header found");
			}
			String contentType = mimeType.toString();
			EventFormat format = EventFormatProvider.getInstance().resolveFormat(contentType);

			if (format == null) {
				throw new MessageHandlingException(message, "No event format found for specified content type: "
						+ contentType);
			}
			cloudEvent = format.deserialize(payload);

		}
		else {
			cloudEvent = (CloudEvent) message.getPayload();
		}

		Assert.state(cloudEvent.getData() != null, "CloudEvent data can not be null");

		MessageBuilder<?> builder = MessageBuilder.withPayload(cloudEvent.getData().toBytes());
		builder.copyHeaders(message.getHeaders());

		String subject = cloudEvent.getSubject();
		OffsetDateTime time = cloudEvent.getTime();
		String dataContentType = cloudEvent.getDataContentType();
		URI dataSchema = cloudEvent.getDataSchema();

		builder.setHeader(CloudEventHeaders.EVENT_SOURCE, cloudEvent.getSource());
		builder.setHeader(CloudEventHeaders.EVENT_TYPE, cloudEvent.getType());
		builder.setHeader(CloudEventHeaders.EVENT_ID, cloudEvent.getId());
		builder.setHeader(MessageHeaders.CONTENT_TYPE, dataContentType);

		setHeaderIfNotNull(builder, CloudEventHeaders.EVENT_SUBJECT, subject);
		setHeaderIfNotNull(builder, CloudEventHeaders.EVENT_TIME, time);
		setHeaderIfNotNull(builder, CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, dataContentType);
		setHeaderIfNotNull(builder, CloudEventHeaders.EVENT_DATA_SCHEMA, dataSchema);

		CloudEvent cloudEventInstance = cloudEvent;
		cloudEvent.getExtensionNames().forEach(name ->
				builder.setHeader(CloudEventHeaders.PREFIX + name, cloudEventInstance.getExtension(name)));

		return builder.build();
	}

	private void setHeaderIfNotNull(MessageBuilder<?> builder, String key, @Nullable Object value) {
		if (value != null) {
			builder.setHeader(key, value);
		}
	}

	@Override
	public String getComponentType() {
		return "from-cloudevent-transformer";
	}

}
