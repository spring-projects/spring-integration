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
 * Transform messages to CloudEvent format with attributes and extensions mapping.
 * <p>This transformer supports two output modes:
 * <ul>
 *   <li><b>Structured Content Mode:</b> When an {@link EventFormat} is available
 *   (either configured via {@link #setEventFormat(EventFormat)} or resolved via
 *   {@link #setEventFormatContentTypeExpression(Expression)}), the CloudEvent is
 *   serialized into the message payload using that format (e.g., JSON, XML).
 *   The output message contains the serialized CloudEvent as payload with
 *   {@link MessageHeaders#CONTENT_TYPE} set to the format's serialized content type.</li>
 *   <li><b>Binary Content Mode:</b> When no {@link EventFormat} is available,
 *   the transformer uses {@link MessageBuilderMessageWriter} to convert the CloudEvent
 *   into a message where CloudEvent attributes are mapped to headers with a configurable
 *   prefix (default: {@code "ce-"}), and the CloudEvent data becomes the payload.
 *   The output message contains the original data as payload with
 *   {@link MessageHeaders#CONTENT_TYPE} set to {@code "application/cloudevents"}.</li>
 * </ul>
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	private static final String DEFAULT_PREFIX = "ce-";

	private final String[] extensionPatterns;

	private final EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	private String cloudEventPrefix = DEFAULT_PREFIX;

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
	 * Create a ToCloudEventTransformer with no extensionPatterns.
	 */
	public ToCloudEventTransformer() {
		this(new String[0]);
	}

	/**
	 * Create a ToCloudEventTransformer.
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 *                          to the CloudEvent
	 */
	public ToCloudEventTransformer(String... extensionPatterns) {
		this.extensionPatterns = Arrays.copyOf(extensionPatterns, extensionPatterns.length);
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
						"CloudEvent source URIs will use 'null' as the application name. ");
			}
			this.sourceExpression = new ValueExpression<>(URI.create("/spring/" + appName + "." + getBeanName()));
		}
		if (this.eventFormat != null && this.eventFormatContentTypeExpression != null) {
			logger.warn("'eventFormat' and 'eventFormatContentTypeExpression' have both been set, " +
					"the 'eventFormatContentTypeExpression' will be ignored.");
		}
	}

	/**
	 * Set the {@link Expression} to create CloudEvent {@code id}.
	 * Default extracts the {@code id} from the {@link MessageHeaders} of the message.
	 * @param eventIdExpression the expression to create the {@code id} for each CloudEvent
	 */
	public void setEventIdExpression(Expression eventIdExpression) {
		this.eventIdExpression = eventIdExpression;
	}

	/**
	 * Set the {@link Expression} to create CloudEvent {@code source}.
	 * Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceExpression the expression to create the {@code source} for each CloudEvent
	 */
	public void setSourceExpression(Expression sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

	/**
	 * Set the {@link Expression} to extract the {@code type} for the CloudEvent.
	 * The default is {@code spring.message}.
	 * @param typeExpression the expression to create the {@code type} for each CloudEvent
	 */
	public void setTypeExpression(Expression typeExpression) {
		this.typeExpression = typeExpression;
	}

	/**
	 * Set the {@link Expression} to create the {@code dataSchema} for the CloudEvent.
	 * @param dataSchemaExpression the expression to create the {@code dataSchema} for each CloudEvent
	 */
	public void setDataSchemaExpression(Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	/**
	 * Set the {@link Expression} to create the {@code subject} for the CloudEvent.
	 * @param subjectExpression the expression to create the {@code subject} for each CloudEvent
	 */
	public void setSubjectExpression(Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
	}

	/**
	 * Set the {@link EventFormat} to use for CloudEvent serialization.
	 * If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has a precedence.
	 * @param eventFormat the event format for serializing CloudEvents
	 */
	public void setEventFormat(EventFormat eventFormat) {
		this.eventFormat = eventFormat;
	}

	/**
	 * Set the {@link Expression} to produce a cloud event format content type
	 * when {@link EventFormatProvider} is to be used to determine
	 * {@link EventFormat}.
	 * If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has a precedence.
	 * @param eventFormatContentTypeExpression the expression to create
	 * content type for the {@link EventFormatProvider#resolveFormat(String)}
	 * @see io.cloudevents.core.format.ContentType
	 */
	public void setEventFormatContentTypeExpression(Expression eventFormatContentTypeExpression) {
		this.eventFormatContentTypeExpression = eventFormatContentTypeExpression;
	}

	/**
	 * Set the prefix for CloudEvent headers in binary content mode.
	 * Defaults to {@code ce-}.
	 * @param cloudEventPrefix the prefix to use for CloudEvent headers
	 */
	public void setCloudEventPrefix(String cloudEventPrefix) {
		this.cloudEventPrefix = cloudEventPrefix;
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isInstanceOf(byte[].class, payload, "Message payload must be 'byte[]'");

		String id = this.eventIdExpression.getValue(this.evaluationContext, message, String.class);
		URI source = this.sourceExpression.getValue(this.evaluationContext, message, URI.class);
		String type = this.typeExpression.getValue(this.evaluationContext, message, String.class);

		MessageHeaders headers = message.getHeaders();
		MimeType mimeType = StaticMessageHeaderAccessor.getContentType(message);
		String contentType = (mimeType == null) ? "application/octet-stream" : mimeType.toString();

		Map<String, Object> cloudEventExtensions = getCloudEventExtensions(headers);
		ToCloudEventTransformerExtension extensions = new ToCloudEventTransformerExtension(cloudEventExtensions);

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

		CloudEvent cloudEvent =
				cloudEventBuilder.withData((byte[]) payload)
						.withExtension(extensions)
						.build();

		EventFormat selectedEventFormat = this.eventFormat;
		if (selectedEventFormat == null && this.eventFormatContentTypeExpression != null) {
			String expressionContentType =
					this.eventFormatContentTypeExpression.getValue(this.evaluationContext, message, String.class);
			Assert.hasText(expressionContentType, "The 'eventFormatContentTypeExpression' must not evaluate to null.");
			selectedEventFormat = this.eventFormatProvider.resolveFormat(expressionContentType);
			if (selectedEventFormat == null) {
				throw new MessageTransformationException("No EventFormat found for content type of '" +
						expressionContentType + "' provided by the expression '" +
						this.eventFormatContentTypeExpression.getExpressionString() + "'");
			}
		}

		if (selectedEventFormat != null) {
			return MessageBuilder.withPayload(selectedEventFormat.serialize(cloudEvent))
					.copyHeaders(headers)
					.setHeader(MessageHeaders.CONTENT_TYPE, selectedEventFormat.serializedContentType())
					.build();
		}

		return CloudEventUtils.toReader(cloudEvent)
				.read(new MessageBuilderMessageWriter(this.cloudEventPrefix, headers));
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
	private record ToCloudEventTransformerExtension(Map<String, Object> cloudEventExtensions)
			implements CloudEventExtension {

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
			this.headers = new HashMap<>(headers);
			this.cloudEventPrefix = cloudEventPrefix;
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
			this.headers.put(MessageHeaders.CONTENT_TYPE, "application/cloudevents");
			return this;
		}

		/**
		 * Add a CloudEvent context attribute to the message headers.
		 * Map the CloudEvent attribute to a message header by prepending the configured prefix
		 * to the attribute name (e.g., "id" becomes "ce-id" with the default prefix).
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
		 * Complete the message creation with CloudEvent data.
		 * @param value the CloudEvent data to use as the message payload
		 * @return the Spring Integration message with CloudEvent data and attributes
		 * @throws CloudEventRWException if an error occurs during message creation
		 */
		@Override
		public Message<byte[]> end(CloudEventData value) throws CloudEventRWException {
			return doEnd(value.toBytes());
		}

		/**
		 * Complete the message creation without CloudEvent data.
		 * Create a message with an empty payload when the CloudEvent contains no data.
		 * @return the Spring Integration message with an empty payload and CloudEvent attributes as headers
		 */
		@Override
		public Message<byte[]> end() {
			return doEnd(new byte[0]);
		}

		private Message<byte[]> doEnd(byte[] payload) {
			return MessageBuilder
					.withPayload(payload)
					.copyHeaders(this.headers)
					.build();
		}

	}

}
