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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

/**
 * A Spring Integration transformer that converts messages to CloudEvent format.
 * Attribute and extension mapping is performed based on {@link Expression}s.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	private Expression idExpression = new FunctionExpression<Message<?>>(
			msg -> Objects.requireNonNull(msg.getHeaders().getId()).toString());

	@SuppressWarnings("NullAway.Init")
	private Expression sourceExpression;

	private Expression typeExpression = new LiteralExpression("spring.message");

	@SuppressWarnings("NullAway.Init")
	private Expression dataSchemaExpression;

	private Expression subjectExpression = new FunctionExpression<>((Function<Message<?>, @Nullable String>)
			message -> null);

	private final Expression @Nullable [] cloudEventExtensionExpressions;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	private final EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

	/**
	 * Construct a ToCloudEventTransformer.
	 *
	 * @param cloudEventExtensionExpressions an array of {@link Expression}s for establishing CloudEvent extensions
	 */
	public ToCloudEventTransformer(Expression @Nullable ... cloudEventExtensionExpressions) {
		this.cloudEventExtensionExpressions = cloudEventExtensionExpressions;
	}

	/**
	 * Construct a ToCloudEventTransformer with no {@link Expression}s for extensions.
	 *
	 */
	public ToCloudEventTransformer() {
		this((Expression[]) null);
	}

	/**
	 * Set the {@link Expression} for creating CloudEvent ids.
	 * Default expression extracts the id from the {@link MessageHeaders} of the message.
	 *
	 * @param idExpression the expression used to create the id for each CloudEvent
	 */
	public void setIdExpression(Expression idExpression) {
		this.idExpression = idExpression;
	}

	/**
	 * Set the {@link Expression} for creating CloudEvent source.
	 * Default expression is {@code "/spring/" + appName + "." + getBeanName())}.
	 *
	 * @param sourceExpression the expression used to create the source for each CloudEvent
	 */
	public void setSourceExpression(Expression sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

	/**
	 * Set the {@link Expression} for extracting the type for the CloudEvent.
	 * Default expression sets the default to "spring.message".
	 *
	 * @param typeExpression the expression used to create the type for each CloudEvent
	 */
	public void setTypeExpression(Expression typeExpression) {
		this.typeExpression = typeExpression;
	}

	/**
	 * Set the {@link Expression} for creating the dataSchema for the CloudEvent.
	 * Default {@link Expression} evaluates to a null.
	 *
	 * @param dataSchemaExpression the expression used to create the dataSchema for each CloudEvent
	 */
	public void setDataSchemaExpression(Expression dataSchemaExpression) {
		this.dataSchemaExpression = dataSchemaExpression;
	}

	/**
	 * Set the {@link Expression} for creating the subject for the CloudEvent.
	 * Default {@link Expression} evaluates to a null.
	 *
	 * @param subjectExpression the expression used to create the subject for each CloudEvent
	 */
	public void setSubjectExpression(Expression subjectExpression) {
		this.subjectExpression = subjectExpression;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		ApplicationContext applicationContext = getApplicationContext();
		if (this.sourceExpression == null) {  // in the case the user sets the value prior to onInit.
			this.sourceExpression = new FunctionExpression<>((Function<Message<?>, URI>) message -> {
				String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
				appName = appName == null ? "unknown" : appName;
				return URI.create("/spring/" + appName + "." + getBeanName());
			});
		}
		if (this.dataSchemaExpression == null) { // in the case the user sets the value prior to onInit.
			this.dataSchemaExpression = new FunctionExpression<>((Function<Message<?>, @Nullable URI>)
					message -> null);
		}
	}

	/**
	 * Transform the input message into a CloudEvent message.
	 *
	 * @param message the input Spring Integration message to transform
	 * @return CloudEvent message in the specified format
	 * @throws RuntimeException if serialization fails
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object doTransform(Message<?> message) {

		String id = this.idExpression.getValue(this.evaluationContext, message, String.class);
		if (!StringUtils.hasText(id)) {
			throw new MessageTransformationException(message, "No id was found with the specified expression");
		}

		URI source = this.sourceExpression.getValue(this.evaluationContext, message, URI.class);
		if (source == null) {
			throw new MessageTransformationException(message, "No source was found with the specified expression");
		}

		String type = this.typeExpression.getValue(this.evaluationContext, message, String.class);
		if (type == null) {
			throw new MessageTransformationException(message, "No type was found with the specified expression");
		}

		String contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE, String.class);
		if (contentType == null) {
			throw new MessageTransformationException(message, "Missing 'Content-Type' header");
		}

		EventFormat eventFormat = this.eventFormatProvider.resolveFormat(contentType);
		if (eventFormat == null) {
			throw new MessageTransformationException("No EventFormat found for '" + contentType + "'");
		}

		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(this.evaluationContext, (Message<byte[]>) message,
						this.cloudEventExtensionExpressions);

		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId(id)
				.withSource(source)
				.withType(type)
				.withTime(OffsetDateTime.now())
				.withDataContentType(contentType)
				.withDataSchema(this.dataSchemaExpression.getValue(this.evaluationContext, message, URI.class))
				.withSubject(this.subjectExpression.getValue(this.evaluationContext, message, String.class))
				.withData(getPayload(message))
				.withExtension(extensions)
				.build();

		return MessageBuilder.withPayload(eventFormat.serialize(cloudEvent))
				.copyHeaders(message.getHeaders())
				.build();
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevents-transformer";
	}

	private byte[] getPayload(Message<?> message) {
		if (message.getPayload() instanceof byte[] messagePayload) {
			return  messagePayload;
		}
		throw new MessageTransformationException("Message payload is not a byte array");
	}

	private static class ToCloudEventTransformerExtensions implements CloudEventExtension {

		/**
		 * Map storing the CloudEvent extensions extracted from message headers.
		 */
		private final Map<String, Object> cloudEventExtensions;

		/**
		 * Construct CloudEvent extensions by processing a message using expressions.
		 *
		 * @param message the Spring Integration message
		 * @param expressions an array of {@link Expression}s where each accepts a message and returns a
		 * {@code Map<String, Object>} of extensions
		 */
		@SuppressWarnings("unchecked")
		ToCloudEventTransformerExtensions(EvaluationContext evaluationContext, Message<byte[]> message,
				Expression @Nullable ... expressions) {
			this.cloudEventExtensions = new HashMap<>();
			if (expressions == null) {
				return;
			}
			for (Expression expression : expressions) {
				Map<String, Object> result = (Map<String, Object>) expression.getValue(evaluationContext, message,
						Map.class);
				if (result == null) {
					continue;
				}
				for (String key : result.keySet()) {
					this.cloudEventExtensions.put(key, result.get(key));
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
