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

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
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
 * @see EventFormat
 */
public class FromCloudEventTransformer extends AbstractTransformer {

	private final EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	@Nullable
	private EventFormat eventFormat;

	/**
	 * Establish the {@link EventFormat} that will be used if the {@link EventFormatProvider} can not identify the
	 * {@link EventFormat} for the message's payload.
	 * @param eventFormat The fallback {@link EventFormat} to use if {@link EventFormatProvider} can not identify the
	 *                    {@link EventFormat} for the payload.
	 */
	public void setEventFormat(EventFormat eventFormat) {
		this.eventFormat = eventFormat;
	}

	@Override
	public String getComponentType() {
		return "ce:from-cloudevent-transformer";
	}

	@Override
	protected Object doTransform(Message<?> message) {

		Assert.state(message.getPayload() instanceof byte[] || message.getPayload() instanceof CloudEvent,
				"Payload did not contain CloudEvent nor could it be deserialized to a CloudEvent");
		CloudEvent cloudEvent;

		if (message.getPayload() instanceof byte[] payload) {
			MimeType mimeType = StaticMessageHeaderAccessor.getContentType(message);
			if (mimeType == null) {
				throw new MessageTransformationException(message, "No Content-Type header found");
			}
			String contentType = mimeType.toString();

			EventFormat format = this.eventFormatProvider.resolveFormat(contentType);
			if (format == null) {
				format = this.eventFormat;
			}
			if (format == null) {
				throw new MessageTransformationException(
						message, "No event format found for specified content type: " + contentType);
			}

			cloudEvent = format.deserialize(payload);

		}
		else {
			cloudEvent = (CloudEvent) message.getPayload();
		}
		CloudEventData cloudEventData = cloudEvent.getData();
		byte[] payload = (cloudEventData == null) ? new byte[0] : cloudEventData.toBytes();
		AbstractIntegrationMessageBuilder<byte[]> builder = getMessageBuilderFactory().withPayload(payload)
				.copyHeaders(message.getHeaders())
				.setHeader(CloudEventHeaders.EVENT_SOURCE, cloudEvent.getSource())
				.setHeader(CloudEventHeaders.EVENT_TYPE, cloudEvent.getType())
				.setHeader(CloudEventHeaders.EVENT_ID, cloudEvent.getId())
				.setHeader(MessageHeaders.CONTENT_TYPE, cloudEvent.getDataContentType())

				.setHeader(CloudEventHeaders.EVENT_SUBJECT, cloudEvent.getSubject())
				.setHeader(CloudEventHeaders.EVENT_TIME, cloudEvent.getTime())
				.setHeader(CloudEventHeaders.EVENT_DATA_CONTENT_TYPE, cloudEvent.getDataContentType())
				.setHeader(CloudEventHeaders.EVENT_DATA_SCHEMA, cloudEvent.getDataSchema());

		CloudEvent cloudEventInstance = cloudEvent;
		cloudEvent.getExtensionNames().forEach(name ->
				builder.setHeader(CloudEventHeaders.PREFIX + name, cloudEventInstance.getExtension(name)));

		return builder.build();
	}

}
