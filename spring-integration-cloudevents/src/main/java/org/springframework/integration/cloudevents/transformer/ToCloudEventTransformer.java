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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;
import io.cloudevents.rw.CloudEventWriterFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * Transform messages to CloudEvent format with attribute and extension mapping.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	private static final String DEFAULT_PREFIX = "ce-";

	private final String[] extensionPatterns;

	private final EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	private String cloudEventPrefix = DEFAULT_PREFIX;

	private boolean failOnNoFormat;

	private Expression eventIdExpression = new FunctionExpression<Message<?>>(
			msg -> Objects.requireNonNull(msg.getHeaders().getId()).toString());

	@SuppressWarnings("NullAway.Init")
	private Expression sourceExpression;

	private Expression typeExpression = new LiteralExpression("spring.message");

	private @Nullable Expression dataSchemaExpression;

	private @Nullable Expression subjectExpression;

	private @Nullable Expression eventFormatContentTypeExpression;

	private @Nullable EventFormat eventFormat;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	/**
	 * Create a ToCloudEventTransformer.
	 * @param eventFormat the event format to use for serialization
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 *                          to the CloudEvent
	 */
	public ToCloudEventTransformer(@Nullable EventFormat eventFormat, String... extensionPatterns) {
		this.eventFormat = eventFormat;
		this.extensionPatterns = Arrays.copyOf(extensionPatterns, extensionPatterns.length);
	}

	/**
	 * Create a ToCloudEventTransformer with no extensionPatterns.
	 * @param eventFormat the event format to use for serialization
	 */
	public ToCloudEventTransformer(@Nullable EventFormat eventFormat) {
		this(eventFormat, new String[0]);
	}

	/**
	 * Create a ToCloudEventTransformer with no extensionPatterns and no {@link EventFormat}.
	 */
	public ToCloudEventTransformer() {
		this(null);
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (this.sourceExpression == null) {
			ApplicationContext applicationContext = getApplicationContext();
			String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
			if (!StringUtils.hasText(appName)) {
				logger.warn("'spring.application.name' is not set. " +
						"CloudEvent source URIs will use 'null' as the application name. " +
						"Consider setting 'spring.application.name'");
			}
			this.sourceExpression = new ValueExpression<>(URI.create("/spring/" + appName + "." + getBeanName()));
		}
	}

	/**
	 * Set the {@link Expression} to create CloudEvent ids.
	 * Default extracts the id from the {@link MessageHeaders} of the message.
	 * @param eventIdExpression the expression to create the id for each CloudEvent
	 */
	public void setEventIdExpression(Expression eventIdExpression) {
		this.eventIdExpression = eventIdExpression;
	}

	/**
	 * Set the {@link Expression} to create CloudEvent source.
	 * Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceExpression the expression to create the source for each CloudEvent
	 */
	public void setSourceExpression(Expression sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

	/**
	 * Set the {@link Expression} to extract the type for the CloudEvent.
	 * Default is "spring.message".
	 * @param typeExpression the expression to create the type for each CloudEvent
	 */
	public void setTypeExpression(Expression typeExpression) {
		this.typeExpression = typeExpression;
	}

	/**
	 * Set the {@link Expression} to create the dataSchema for the CloudEvent.
	 * @param dataSchemaExpression the expression to create the dataSchema for each CloudEvent
	 */
	public void setDataSchemaExpression(Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	/**
	 * Set the {@link Expression} to create the subject for the CloudEvent.
	 * @param subjectExpression the expression to create the subject for each CloudEvent
	 */
	public void setSubjectExpression(Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
	}

	/**
	 * Set the {@link Expression} to create for the content type to use
	 * when {@link EventFormatProvider} is to be used to determine
	 * {@link EventFormat}.
	 * @param eventFormatContentTypeExpression the expression to create
	 * content type for the {@link EventFormatProvider}
	 */
	public void setEventFormatContentTypeExpression(Expression eventFormatContentTypeExpression) {
		this.eventFormatContentTypeExpression = eventFormatContentTypeExpression;
	}

	/**
	 * Set to {@code true} to fail if no {@link EventFormat} is found for message content type.
	 * When {@code false} and no {@link EventFormat} is found, then a {@link CloudEvent}' body is
	 * set as an output message's payload, and its attributes are set into headers.
	 * @param failOnNoFormat true to disable format serialization
	 */
	public void setFailOnNoFormat(boolean failOnNoFormat) {
		this.failOnNoFormat = failOnNoFormat;
	}

	/**
	 * Return whether the transformer will fail when no {@link EventFormat}
	 * is available for the content type.
	 * @return {@code true} if transformation should fail when no suitable
	 * {@link EventFormat} is found; {@code false} otherwise
	 */
	public boolean isFailOnNoFormat() {
		return this.failOnNoFormat;
	}

	/**
	 * Set the prefix for CloudEvent headers in binary content mode.
	 * @param cloudEventPrefix the prefix to use for CloudEvent headers
	 */
	public void setCloudEventPrefix(String cloudEventPrefix) {
		this.cloudEventPrefix = cloudEventPrefix;
	}

	/**
	 * Return the prefix used for CloudEvent headers in binary content mode.
	 * @return the CloudEvent header prefix
	 */
	public String getCloudEventPrefix() {
		return this.cloudEventPrefix;
	}

	/**
	 * Transform the input message into a CloudEvent message.
	 * @param message the input Spring Integration message to transform
	 * @return CloudEvent message in the specified format
	 * @throws RuntimeException if serialization fails
	 */
	@Override
	protected Object doTransform(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isInstanceOf(byte[].class, payload, "Message payload must be byte[]");

		String id = this.eventIdExpression.getValue(this.evaluationContext, message, String.class);
		URI source = this.sourceExpression.getValue(this.evaluationContext, message, URI.class);
		String type = this.typeExpression.getValue(this.evaluationContext, message, String.class);
		MessageHeaders headers = message.getHeaders();
		MimeType mimeType = StaticMessageHeaderAccessor.getContentType(message);
		String contentType;
		if (mimeType == null) {
			contentType = "application/octet-stream";
		}
		else {
			contentType = mimeType.toString();
		}

		Map<String, Object> cloudEventExtensions = getCloudEventExtensions(headers);

		ToCloudEventTransformerExtension extensions =
				new ToCloudEventTransformerExtension(cloudEventExtensions);

		CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
				.withId(id)
				.withSource(source)
				.withType(type)
				.withTime(OffsetDateTime.now())
				.withDataContentType(contentType);

		if (this.subjectExpression != null) {
			cloudEventBuilder.withSubject(
					this.subjectExpression.getValue(this.evaluationContext, message, String.class));
		}
		if (this.dataSchemaExpression != null) {
			cloudEventBuilder.withDataSchema(
					this.dataSchemaExpression.getValue(this.evaluationContext, message, URI.class));
		}

		CloudEvent cloudEvent = cloudEventBuilder.withData((byte[]) payload)
				.withExtension(extensions)
				.build();

		if (this.eventFormatContentTypeExpression != null) {
			this.eventFormat = this.eventFormatProvider.resolveFormat(
					this.eventFormatContentTypeExpression.getValue(this.evaluationContext, message, String.class));
		}

		if (this.eventFormat == null && this.failOnNoFormat) {
			throw new MessageTransformationException("No EventFormat found for '" + contentType + "'");
		}

		if (this.eventFormat != null) {
			return MessageBuilder.withPayload(this.eventFormat.serialize(cloudEvent))
					.copyHeaders(headers)
					.setHeader(MessageHeaders.CONTENT_TYPE, this.eventFormat.serializedContentType())
					.build();
		}
		HashMap<String, Object> messageMap = new HashMap<>(headers);
		return CloudEventUtils.toReader(cloudEvent)
				.read(new MessageBuilderMessageWriter(this.cloudEventPrefix, messageMap));
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevent-transformer";
	}

	/**
	 * Extract CloudEvent extensions from message headers based on pattern matching.
	 * @param headers the message headers to extract extensions from
	 * @return a map of header key-value pairs that match the extension patterns;
	 * an empty map if no headers match the patterns
	 */
	private Map<String, Object> getCloudEventExtensions(MessageHeaders headers) {
		Map<String, Object> cloudEventExtensions = new HashMap<>();
		for (Map.Entry<String, Object> header : headers.entrySet()) {
			String headerKey = header.getKey();
			Boolean patternResult = PatternMatchUtils.smartMatch(headerKey, this.extensionPatterns);
			if (Boolean.TRUE.equals(patternResult)) {
				cloudEventExtensions.put(headerKey, header.getValue());
			}
		}
		return cloudEventExtensions;
	}

	/**
	 * Custom CloudEvent extension implementation that wraps a map of headers
	 * as CloudEvent extension attributes.
	 */
	private static class ToCloudEventTransformerExtension implements CloudEventExtension {

		private final Map<String, Object> cloudEventExtensions;

		ToCloudEventTransformerExtension(Map<String, Object> headers) {
			this.cloudEventExtensions = headers;
		}

		@Override
		public void readFrom(CloudEventExtensions extensions) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable Object getValue(String key) throws IllegalArgumentException {
			return this.cloudEventExtensions.get(key);
		}

		@Override
		public Set<String> getKeys() {
			return this.cloudEventExtensions.keySet();
		}

	}

	/**
	 * CloudEvent writer implementation that converts CloudEvent objects into
	 * Spring Integration {@link Message} instances with CloudEvent attributes as headers.
	 */
	private static class MessageBuilderMessageWriter implements CloudEventWriter<Message<byte[]>>,
			CloudEventWriterFactory<MessageBuilderMessageWriter, Message<byte[]>> {

		private final String cloudEventPrefix;

		private final Map<String, Object> headers;

		MessageBuilderMessageWriter(String cloudEventPrefix, Map<String, Object> headers) {
			this.headers = headers;
			this.cloudEventPrefix = cloudEventPrefix;
		}

		/**
		 * Complete the message creation with CloudEvent data.
		 * Create a message with the CloudEvent data as the payload. Set CloudEvent attributes
		 * as headers via {@link #withContextAttribute(String, String)}.
		 * @param value the CloudEvent data to use as the message payload
		 * @return the Spring Integration message with CloudEvent data and attributes
		 * @throws CloudEventRWException if an error occurs during message creation
		 */
		@Override
		public Message<byte[]> end(CloudEventData value) throws CloudEventRWException {
			return MessageBuilder
					.withPayload(value.toBytes())
					.copyHeaders(this.headers)
					.build();
		}

		/**
		 * Complete the message creation without CloudEvent data.
		 * Create a message with an empty payload when the CloudEvent contains no data.
		 * Set CloudEvent attributes as headers via {@link #withContextAttribute(String, String)}.
		 * @return the Spring Integration message with an empty payload and CloudEvent attributes as headers
		 */
		@Override
		public Message<byte[]> end() {
			return MessageBuilder
					.withPayload(new byte[0])
					.copyHeaders(this.headers)
					.build();
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
			this.headers.put(this.cloudEventPrefix + "specversion", version.toString());
			return this;
		}

	}

}
