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
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.cloudevents.transformer.util.CloudEventUtil;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * Transform CloudEvent format messages to Spring Integration messages.
 * <p>This transformer supports two deserialization modes:
 * <ul>
 *   <li><b>Serialized CloudEvent Mode:</b> When the input message contains a
 *   serialized CloudEvent (e.g., JSON, XML) and an {@link EventFormat} can be
 *   resolved from the {@link MessageHeaders#CONTENT_TYPE} header, the transformer
 *   deserializes the payload using that format. The CloudEvent attributes are
 *   extracted and mapped to message headers using the configured message header keys.
 *   CloudEvent extension attributes are also mapped to message headers with the
 *   configured prefix (default: {@code "ce-"}).</li>
 *   <li><b>Header-based CloudEvent Mode:</b> When no {@link EventFormat} can be
 *   resolved, the transformer reconstructs the CloudEvent from message headers.
 *   It expects CloudEvent attributes to be present in headers using the configured
 *   CloudEvent header keys (e.g., {@code "ce-id"}, {@code "ce-source"}, {@code "ce-type"}).
 *   The message payload is used as the CloudEvent data.</li>
 * </ul>
 * <p>This transformer provides separate configuration for:
 * <ul>
 *   <li><b>CloudEvent Header Keys:</b> Header keys used when reading CloudEvent
 *   attributes from incoming message headers in header-based mode (configured via
 *   {@code setCeHeader*Key} methods)</li>
 *   <li><b>Message Header Keys:</b> Header keys used when writing CloudEvent
 *   attributes to the output message headers (configured via {@code setMessageHeader*Key}
 *   methods)</li>
 * </ul>
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 *
 * @see ToCloudEventTransformer
 * @see io.cloudevents.CloudEvent
 * @see io.cloudevents.core.format.EventFormat
 */
public class FromCloudEventTransformer extends AbstractTransformer {

	private String cloudEventExtensionPrefix = "ce-";

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderIdKey;

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderTimeKey;

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderSourceKey;

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderDataContentTypeKey;

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderSubjectKey;

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderDataSchemaKey;

	@SuppressWarnings("NullAway.Init")
	private String ceHeaderTypeKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderIdKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderTimeKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderSourceKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderDataContentTypeKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderSubjectKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderDataSchemaKey;

	@SuppressWarnings("NullAway.Init")
	private String messageHeaderTypeKey;

	private final String[] extensionPatterns;

	/**
	 * Create a FromCloudEventTransformer with no extensionPatterns.
	 */
	public FromCloudEventTransformer() {
		this(new String[0]);
	}

	/**
	 * Create a FromCloudEventTransformer.
	 * @param extensionPatterns patterns to evaluate whether message headers should be added
	 * as extensions to the CloudEvent when extracting extensions from message headers
	 */
	public FromCloudEventTransformer(String... extensionPatterns) {
		this.extensionPatterns = Arrays.copyOf(extensionPatterns, extensionPatterns.length);
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.ceHeaderIdKey = this.messageHeaderIdKey = "ce-id";
		this.ceHeaderTimeKey = this.messageHeaderTimeKey = "ce-time";
		this.ceHeaderSourceKey = this.messageHeaderSourceKey = "ce-source";
		this.ceHeaderDataContentTypeKey = this.messageHeaderDataContentTypeKey = "ce-datacontenttype";
		this.ceHeaderSubjectKey = this.messageHeaderSubjectKey = "ce-subject";
		this.ceHeaderDataSchemaKey = this.messageHeaderDataSchemaKey = "ce-dataschema";
		this.ceHeaderTypeKey = this.messageHeaderTypeKey = "ce-type";
	}

	/**
	 * Set the header key for the CloudEvent ID attribute.
	 * @param ceHeaderIdKey the header key for the ID attribute
	 */
	public void setCeHeaderIdKey(String ceHeaderIdKey) {
		this.ceHeaderIdKey = ceHeaderIdKey;
	}

	/**
	 * Set the header key for the CloudEvent time attribute.
	 * @param ceHeaderTimeKey the header key for the time attribute
	 */
	public void setCeHeaderTimeKey(String ceHeaderTimeKey) {
		this.ceHeaderTimeKey = ceHeaderTimeKey;
	}

	/**
	 * Set the header key for the CloudEvent source attribute.
	 * @param ceHeaderSourceKey the header key for the source attribute
	 */
	public void setCeHeaderSourceKey(String ceHeaderSourceKey) {
		this.ceHeaderSourceKey = ceHeaderSourceKey;
	}

	/**
	 * Set the header key for the CloudEvent datacontenttype attribute.
	 * @param ceHeaderDataContentTypeKey the header key for the datacontenttype attribute
	 */
	public void setCeHeaderDataContentTypeKey(String ceHeaderDataContentTypeKey) {
		this.ceHeaderDataContentTypeKey = ceHeaderDataContentTypeKey;
	}

	/**
	 * Set the header key for the CloudEvent subject attribute.
	 * @param ceHeaderSubjectKey the header key for the subject attribute
	 */
	public void setCeHeaderSubjectKey(String ceHeaderSubjectKey) {
		this.ceHeaderSubjectKey = ceHeaderSubjectKey;
	}

	/**
	 * Set the header key for the CloudEvent dataschema attribute.
	 * @param ceHeaderDataSchemaKey the header key for the dataschema attribute
	 */
	public void setCeHeaderDataSchemaKey(String ceHeaderDataSchemaKey) {
		this.ceHeaderDataSchemaKey = ceHeaderDataSchemaKey;
	}

	/**
	 * Set the header key for the CloudEvent type attribute.
	 * @param ceHeaderTypeKey the message header key for the type attribute
	 */
	public void setCeHeaderTypeKey(String ceHeaderTypeKey) {
		this.ceHeaderTypeKey = ceHeaderTypeKey;
	}

	/**
	 * Set the prefix for CloudEvent extension headers.
	 * @param cloudEventExtensionPrefix the prefix for extension headers (default: "ce-")
	 */
	public void setCloudEventExtensionPrefix(String cloudEventExtensionPrefix) {
		this.cloudEventExtensionPrefix = cloudEventExtensionPrefix;
	}

	/**
	 * Set the output message header key for the CloudEvent ID attribute.
	 * @param messageHeaderIdKey the message header key for the ID attribute
	 */
	public void setMessageHeaderIdKey(String messageHeaderIdKey) {
		this.messageHeaderIdKey = messageHeaderIdKey;
	}

	/**
	 * Set the output message header key for the CloudEvent time attribute.
	 * @param messageHeaderTimeKey the message header key for the time attribute
	 */
	public void setMessageHeaderTimeKey(String messageHeaderTimeKey) {
		this.messageHeaderTimeKey = messageHeaderTimeKey;
	}

	/**
	 * Set the output message header key for the CloudEvent source attribute.
	 * @param messageHeaderSourceKey the message header key for the source attribute
	 */
	public void setMessageHeaderSourceKey(String messageHeaderSourceKey) {
		this.messageHeaderSourceKey = messageHeaderSourceKey;
	}

	/**
	 * Set the output message header key for the CloudEvent datacontenttype attribute.
	 * @param messageHeaderDataContentTypeKey the message header key for the datacontenttype
	 * attribute
	 */
	public void setMessageHeaderDataContentTypeKey(String messageHeaderDataContentTypeKey) {
		this.messageHeaderDataContentTypeKey = messageHeaderDataContentTypeKey;
	}

	/**
	 * Set the output message header key for the CloudEvent subject attribute.
	 * @param messageHeaderSubjectKey the message header key for the subject attribute
	 */
	public void setMessageHeaderSubjectKey(String messageHeaderSubjectKey) {
		this.messageHeaderSubjectKey = messageHeaderSubjectKey;
	}

	/**
	 * Set the output message header key for the CloudEvent dataschema attribute.
	 * @param messageHeaderDataSchemaKey the message header key for the dataschema attribute
	 */
	public void setMessageHeaderDataSchemaKey(String messageHeaderDataSchemaKey) {
		this.messageHeaderDataSchemaKey = messageHeaderDataSchemaKey;
	}

	/**
	 * Set the output message header key for the CloudEvent type attribute.
	 * @param messageHeaderTypeKey the message header key for the type attribute
	 */
	public void setMessageHeaderTypeKey(String messageHeaderTypeKey) {
		this.messageHeaderTypeKey = messageHeaderTypeKey;
	}

	@SuppressWarnings("NullAway")
	@Override
	protected Object doTransform(Message<?> message) {

		Assert.state(message.getPayload() instanceof byte[], "Message payload is not of type byte[]");
		byte[] payload = (byte[]) message.getPayload();

		MimeType mimeType = StaticMessageHeaderAccessor.getContentType(message);
		String contentType = (mimeType == null) ? "application/octet-stream" : mimeType.toString();
		EventFormat format = EventFormatProvider.getInstance().resolveFormat(contentType);

		CloudEvent cloudEvent;
		MessageBuilder<?> builder;
		if (format != null) {
			cloudEvent = format.deserialize(payload);
			builder = MessageBuilder.withPayload(cloudEvent.getData().toBytes());
			cloudEvent.getExtensionNames().forEach(name ->
					builder.setHeader(this.cloudEventExtensionPrefix + name, cloudEvent.getExtension(name)));
		}
		else {
			cloudEvent = getCloudEventFromHeaders(message);
			Map<String, Object> extensions = CloudEventUtil.getCloudEventExtensions(
					message.getHeaders(), this.extensionPatterns);
			builder = MessageBuilder.withPayload(payload);
			for (Map.Entry<String, Object> entry : extensions.entrySet()) {
				builder.setHeader(entry.getKey(), entry.getValue());
			}
		}

		String subject = cloudEvent.getSubject();
		OffsetDateTime time = cloudEvent.getTime();
		String dataContentType = cloudEvent.getDataContentType();
		URI dataSchema = cloudEvent.getDataSchema();

		builder.copyHeaders(message.getHeaders());
		builder.setHeader(this.messageHeaderSourceKey, cloudEvent.getSource());
		builder.setHeader(this.messageHeaderTypeKey, cloudEvent.getType());
		builder.setHeader(this.messageHeaderIdKey, cloudEvent.getId());
		builder.setHeader(MessageHeaders.CONTENT_TYPE, contentType);

		setHeaderIfNotNull(builder, this.messageHeaderSubjectKey, subject);
		setHeaderIfNotNull(builder, this.messageHeaderTimeKey, time);
		setHeaderIfNotNull(builder, this.messageHeaderDataContentTypeKey, dataContentType);
		setHeaderIfNotNull(builder, this.messageHeaderDataSchemaKey, dataSchema);

		return builder.build();
	}

	private CloudEvent getCloudEventFromHeaders(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1().newBuilder();
		Object id = headers.get(this.ceHeaderIdKey);
		Assert.state(id != null, "id must not be null");
		cloudEventBuilder.withId(id.toString());
		String type = (String) headers.get(this.ceHeaderTypeKey);
		Assert.state(StringUtils.hasText(type), "type must not be null or empty");
		cloudEventBuilder.withType(type);

		try {
			Object source = headers.get(this.ceHeaderSourceKey);
			Assert.state(source != null, "source must not be null");
			cloudEventBuilder.withSource(new URI(source.toString()));
		}
		catch (URISyntaxException uriException) {
			throw new MessageHandlingException(message, uriException);
		}
		String subject = (String) headers.get(this.ceHeaderSubjectKey);
		if (subject != null) {
			cloudEventBuilder.withSubject(subject);
		}
		String dataContentType = (String) headers.get(this.ceHeaderDataContentTypeKey);
		if (dataContentType != null) {
			cloudEventBuilder.withDataContentType(dataContentType);
		}

		Object dataSchema = headers.get(this.ceHeaderDataSchemaKey);
		if (dataSchema != null) {
			cloudEventBuilder.withDataSchema(URI.create(dataSchema.toString()));
		}

		Object time = headers.get(this.ceHeaderTimeKey);
		if (time != null) {
			cloudEventBuilder.withTime(OffsetDateTime.parse(time.toString()));
		}

		try {
			return cloudEventBuilder.build();
		}
		catch (IllegalStateException e) {
			throw new MessageHandlingException(message, e);
		}
	}

	private void setHeaderIfNotNull(MessageBuilder<?> builder, String key, Object value) {
		if (value != null) {
			builder.setHeader(key, value);
		}
	}

	@Override
	public String getComponentType() {
		return "from-cloudevent-transformer";
	}

}
