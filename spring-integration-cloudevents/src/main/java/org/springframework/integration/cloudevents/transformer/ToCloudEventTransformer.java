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
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.rw.CloudEventContextWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
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

/**
 * Converts messages to CloudEvent format.
 * Performs attribute and extension mapping based on {@link Expression}s.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	private static final String DEFAULT_PREFIX = "ce-";

	private static final String DEFAULT_SPECVERSION_KEY = "specversion";

	private static final String DEFAULT_DATACONTENTTYPE_KEY = "datacontenttype";

	private String cloudEventPrefix = DEFAULT_PREFIX;

	private boolean failOnNoFormat = false;

	private Expression eventIdExpression = new FunctionExpression<Message<?>>(
			msg -> Objects.requireNonNull(msg.getHeaders().getId()).toString());

	@SuppressWarnings("NullAway.Init")
	private Expression sourceExpression;

	private Expression typeExpression = new LiteralExpression("spring.message");

	@SuppressWarnings("NullAway.Init")
	private @Nullable Expression dataSchemaExpression;

	private @Nullable Expression subjectExpression;

	private final String[] extensionPatterns;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	private static final EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	/**
	 * Construct a ToCloudEventTransformer.
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 *                          to the CloudEvent
	 */
	public ToCloudEventTransformer(String ... extensionPatterns) {
		this.extensionPatterns = extensionPatterns == null ? new String[0] :
				Arrays.copyOf(extensionPatterns, extensionPatterns.length);
	}

	/**
	 * Construct a ToCloudEventTransformer with no extensionPatterns.
	 */
	public ToCloudEventTransformer() {
		this.extensionPatterns = new String[0];
	}

	/**
	 * Set the {@link Expression} for creating CloudEvent ids.
	 * Defaults to extracting the id from the {@link MessageHeaders} of the message.
	 * @param eventIdExpression the expression to create the id for each CloudEvent
	 */
	public void setEventIdExpression(Expression eventIdExpression) {
		this.eventIdExpression = eventIdExpression;
	}

	/**
	 * Set the {@link Expression} for creating CloudEvent source.
	 * Defaults to {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceExpression the expression to create the source for each CloudEvent
	 */
	public void setSourceExpression(Expression sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

	/**
	 * Set the {@link Expression} for extracting the type for the CloudEvent.
	 * Defaults to "spring.message".
	 * @param typeExpression the expression to create the type for each CloudEvent
	 */
	public void setTypeExpression(Expression typeExpression) {
		this.typeExpression = typeExpression;
	}

	/**
	 * Set the {@link Expression} for creating the dataSchema for the CloudEvent.
	 * @param dataSchemaExpression the expression to create the dataSchema for each CloudEvent
	 */
	public void setDataSchemaExpression(Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	/**
	 * Set the {@link Expression} for creating the subject for the CloudEvent.
	 * @param subjectExpression the expression to create the subject for each CloudEvent
	 */
	public void setSubjectExpression(Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
	}

	/**
	 * Set to {@code true} to fail if no {@link EventFormat} is found for message's content type.
	 * When {@code false} and no {@link EventFormat} is found, then a {@link CloudEvent}' body is
	 * set as an output message's payload, and its attributes are set into headers.
	 * @param failOnNoFormat true to disable format serialization
	 */
	public void setFailOnNoFormat(boolean failOnNoFormat) {
		this.failOnNoFormat = failOnNoFormat;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		ApplicationContext applicationContext = getApplicationContext();
		if (this.sourceExpression == null) {  // in the case the user sets the value prior to onInit.
			String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
			appName = appName == null ? "unknown" : appName;
			this.sourceExpression = new ValueExpression<>(URI.create("/spring/" + appName + "." + getBeanName()));
		}
	}

	/**
	 * Transform the input message into a CloudEvent message.
	 * @param message the input Spring Integration message to transform
	 * @return CloudEvent message in the specified format
	 * @throws RuntimeException if serialization fails
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object doTransform(Message<?> message) {
		Assert.isInstanceOf(byte[].class, message.getPayload(), "Message payload must be of type byte[]");

		String id = this.eventIdExpression.getValue(this.evaluationContext, message, String.class);
		URI source = this.sourceExpression.getValue(this.evaluationContext, message, URI.class);
		String type = this.typeExpression.getValue(this.evaluationContext, message, String.class);
		MessageHeaders headers = message.getHeaders();

		String contentType = headers.get(MessageHeaders.CONTENT_TYPE, String.class);
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(headers,
						this.extensionPatterns);

		CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
				.withId(id)
				.withSource(source)
				.withType(type)
				.withTime(OffsetDateTime.now())
				.withDataContentType(contentType);

		if (this.subjectExpression != null) {
			cloudEventBuilder.withSubject(this.subjectExpression.getValue(this.evaluationContext, message, String.class));
		}
		if (this.dataSchemaExpression != null) {
			cloudEventBuilder.withDataSchema(this.dataSchemaExpression.getValue(this.evaluationContext, message, URI.class));
		}

		CloudEvent cloudEvent = cloudEventBuilder.withData((byte[]) message.getPayload())
				.withExtension(extensions)
				.build();

		EventFormat eventFormat = eventFormatProvider.resolveFormat(contentType);

		if (eventFormat == null && this.failOnNoFormat) {
			throw new MessageTransformationException("No EventFormat found for '" + contentType + "'");
		}

		if (eventFormat != null) {
			return MessageBuilder.withPayload(eventFormat.serialize(cloudEvent))
					.copyHeaders(headers)
					.setHeader(MessageHeaders.CONTENT_TYPE, contentType)
					.build();
		}

		Message<byte[]> result = CloudEventUtils.toReader(cloudEvent).read(
				new MessageBuilderMessageWriter(this.cloudEventPrefix, Objects.requireNonNull(headers)));
		return MessageBuilder.withPayload(result.getPayload())
				.copyHeaders(result.getHeaders())
				.setHeader(MessageHeaders.CONTENT_TYPE, contentType)
				.build();
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevent-transformer";
	}

	/**
	 * Indicates whether the transformer will transform the message
	 * when no {@link EventFormat} is available for the content type.
	 * @return {@code true} if transformation should fail
	 *         when no suitable {@link EventFormat} is found;
	 *         {@code false} otherwise
	 */
	public boolean isFailOnNoFormat() {
		return this.failOnNoFormat;
	}

	/**
	 * Return the prefix used for CloudEvent headers in binary content mode.
	 * @return the CloudEvent header prefix
	 */
	public String getCloudEventPrefix() {
		return this.cloudEventPrefix;
	}

	/**
	 * Set the prefix for CloudEvent headers in binary content mode.
	 * @param cloudEventPrefix the prefix to use for CloudEvent headers
	 */
	public void setCloudEventPrefix(String cloudEventPrefix) {
		this.cloudEventPrefix = cloudEventPrefix;
	}

	private static class ToCloudEventTransformerExtensions implements CloudEventExtension {

		/**
		 * Stores the CloudEvent extensions extracted from message headers.
		 */
		private final Map<String, Object> cloudEventExtensions;

		/**
		 * Construct CloudEvent extensions by processing a message using expressions.
		 * @param headers the headers from the Spring Integration message
		 * @param extensionPatterns patterns to determine whether message headers are extensions
		 */
		@SuppressWarnings("unchecked")
		ToCloudEventTransformerExtensions(Map<String, Object> headers, String ... extensionPatterns) {
			this.cloudEventExtensions = new HashMap<>();
			Boolean result = null;
			for (Map.Entry<String, Object> header : headers.entrySet()) {
				result = PatternMatchUtils.smartMatch(header.getKey(), extensionPatterns);
				if (result != null && result) {
					this.cloudEventExtensions.put(header.getKey(), header.getValue());
				}
			}
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

	private static class MessageBuilderMessageWriter
			implements CloudEventWriter<Message<byte[]>>, MessageWriter<MessageBuilderMessageWriter, Message<byte[]>> {

		private final String cloudEventPrefix;

		private final String specVersionKey;

		private final String dataContentTypeKey;

		private final Map<String, Object> headers = new HashMap<>();

		/**
		 * Construct a MessageBuilderMessageWriter with the specified configuration.
		 * @param cloudEventPrefix the prefix to prepend to CloudEvent attribute names in message headers
		 * @param headers the base message headers to include in the output message
		 */
		MessageBuilderMessageWriter(String cloudEventPrefix, Map<String, Object> headers) {
			this.headers.putAll(headers);
			this.cloudEventPrefix = cloudEventPrefix;
			this.specVersionKey = this.cloudEventPrefix + DEFAULT_SPECVERSION_KEY;
			this.dataContentTypeKey = this.cloudEventPrefix + DEFAULT_DATACONTENTTYPE_KEY;
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
			return org.springframework.integration.support.MessageBuilder.withPayload(value).copyHeaders(this.headers).build();
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
			return org.springframework.integration.support.MessageBuilder.withPayload(value.toBytes()).copyHeaders(this.headers).build();
		}

		/**
		 * Complete the message creation without CloudEvent data.
		 * Create a message with an empty payload when the CloudEvent contains no data.
		 * CloudEvent attributes are set as headers via {@link #withContextAttribute(String, String)}.
		 * @return the Spring Integration message with an empty payload and CloudEvent attributes as headers
		 */
		@Override
		public Message<byte[]> end() {
			return org.springframework.integration.support.MessageBuilder.withPayload(new byte[0]).copyHeaders(this.headers).build();
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
}
