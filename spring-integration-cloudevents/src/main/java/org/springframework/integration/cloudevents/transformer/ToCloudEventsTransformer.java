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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
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
 * @since 7.0
 */
public class ToCloudEventsTransformer extends AbstractTransformer {

	private Expression eventIdExpression = new FunctionExpression<Message<?>>(
			msg -> Objects.requireNonNull(msg.getHeaders().getId()).toString());

	@SuppressWarnings("NullAway.Init")
	private Expression sourceExpression;

	private Expression typeExpression = new LiteralExpression("spring.message");

	@SuppressWarnings("NullAway.Init")
	private @Nullable Expression dataSchemaExpression;

	private @Nullable Expression subjectExpression;

	private final String [] extensionPatterns;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	private final EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	/**
	 * Construct a ToCloudEventsTransformer.
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 *                          to the CloudEvent
	 */
	public ToCloudEventsTransformer(String ... extensionPatterns) {
		this.extensionPatterns = extensionPatterns;
	}

	/**
	 * Construct a ToCloudEventsTransformer with no extensionPatterns.
	 */
	public ToCloudEventsTransformer() {
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
	 * Defaults to null.
	 * @param dataSchemaExpression the expression to create the dataSchema for each CloudEvent
	 */
	public void setDataSchemaExpression(@Nullable Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	/**
	 * Set the {@link Expression} for creating the subject for the CloudEvent.
	 * Defaults to null.
	 * @param subjectExpression the expression to create the subject for each CloudEvent
	 */
	public void setSubjectExpression(@Nullable Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
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

		String contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE, String.class);
		if (contentType == null) {
			throw new MessageTransformationException(message, "Missing 'Content-Type' header");
		}

		EventFormat eventFormat = this.eventFormatProvider.resolveFormat(contentType);
		if (eventFormat == null) {
			throw new MessageTransformationException("No EventFormat found for '" + contentType + "'");
		}

		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(message.getHeaders(),
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

		CloudEvent cloudEvent = cloudEventBuilder.withData((byte[])message.getPayload())
				.withExtension(extensions)
				.build();

		return MessageBuilder.withPayload(eventFormat.serialize(cloudEvent))
				.copyHeaders(message.getHeaders())
				.setHeader(MessageHeaders.CONTENT_TYPE, "application/cloudevents")
				.build();
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevents-transformer";
	}

	private static class ToCloudEventTransformerExtensions implements CloudEventExtension {

		/**
		 * Stores the CloudEvent extensions extracted from message headers.
		 */
		private final Map<String, Object> cloudEventExtensions;

		/**
		 * Construct CloudEvent extensions by processing a message using expressions.
		 *
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
			extensions.getExtensionNames()
					.forEach(key -> {
						Object value = extensions.getExtension(key);
						if (value != null) {
							this.cloudEventExtensions.put(key, value);
						}
					});
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

}
