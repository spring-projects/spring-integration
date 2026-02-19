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

package org.springframework.integration.cloudevents.dsl;

import java.net.URI;
import java.util.function.Function;

import io.cloudevents.core.format.EventFormat;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.cloudevents.transformer.ToCloudEventTransformer;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.Message;

/**
 * Spec for a {@link ToCloudEventTransformer}.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
public class ToCloudEventTransformerSpec {

	protected static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final ToCloudEventTransformer transformer;

	/**
	 * Create an instance with no extension patterns.
	 */
	protected ToCloudEventTransformerSpec() {
		this(new String[0]);
	}

	/**
	 * Create an instance with the provided extension patterns.
	 * <p>
	 * Extension patterns are used to match message headers that should be included
	 * as {@link io.cloudevents.CloudEvent} extensions. Patterns support wildcards (e.g., "myapp.*").
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 * to the {@link io.cloudevents.CloudEvent}.
	 */
	protected ToCloudEventTransformerSpec(String... extensionPatterns) {
		this.transformer = new ToCloudEventTransformer(extensionPatterns);
	}

	/**
	 * Set the {@link EventFormat} to use for {@link io.cloudevents.CloudEvent} serialization.
	 * <p>
	 * If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has precedence.
	 * @param eventFormat the event format for serializing CloudEvents
	 * @return the spec for method chaining
	 */
	public ToCloudEventTransformerSpec eventFormat(EventFormat eventFormat) {
		this.transformer.setEventFormat(eventFormat);
		return this;
	}

	/**
	 * Set the SpEL expression to create {@link io.cloudevents.CloudEvent} {@code id}.
	 * <p>
	 * Default is to extract the {@code id}
	 * from the {@link org.springframework.messaging.MessageHeaders} of the message.
	 * @param eventIdExpression the SpEL expression to create the {@code id} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventIdExpression(String eventIdExpression) {
		return eventIdExpression(PARSER.parseExpression(eventIdExpression));
	}

	/**
	 * Set the {@link Function} to create {@link io.cloudevents.CloudEvent} {@code id}.
	 * <p>
	 * Default is to extract the {@code id}
	 * from the {@link org.springframework.messaging.MessageHeaders} of the message.
	 * @param eventIdFunction the {@link Function} to create the {@code id} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventIdFunction(Function<Message<?>, String> eventIdFunction) {
		return eventIdExpression(new FunctionExpression<>(eventIdFunction));
	}

	/**
	 * Set the {@link Expression} to create {@link io.cloudevents.CloudEvent} {@code id}.
	 * <p>
	 * Default is to extract the {@code id}
	 * from the {@link org.springframework.messaging.MessageHeaders} of the message.
	 * @param eventIdExpression the expression to create the {@code id} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventIdExpression(Expression eventIdExpression) {
		this.transformer.setEventIdExpression(eventIdExpression);
		return this;
	}

	/**
	 * Set the SpEL expression to create {@link io.cloudevents.CloudEvent} {@code source}.
	 * <p>
	 * Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceExpression the SpEL expression to create the {@code source} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec sourceExpression(String sourceExpression) {
		return sourceExpression(PARSER.parseExpression(sourceExpression));
	}

	/**
	 * Set the {@link Function} to create {@link io.cloudevents.CloudEvent} {@code source}.
	 * <p>
	 * Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceFunction the {@link Function} to create the {@code source} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec sourceFunction(Function<Message<?>, URI> sourceFunction) {
		return sourceExpression(new FunctionExpression<>(sourceFunction));
	}

	/**
	 * Set the {@link io.cloudevents.CloudEvent} {@code source}.
	 * <p>
	 * Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param source the {@code source} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec source(URI source) {
		return sourceExpression(new ValueExpression<>(source));
	}

	/**
	 * Set the {@link Expression} to create {@link io.cloudevents.CloudEvent} {@code source}.
	 * <p>
	 * Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceExpression the expression to create the {@code source} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec sourceExpression(Expression sourceExpression) {
		this.transformer.setSourceExpression(sourceExpression);
		return this;
	}

	/**
	 * Set the SpEL expression to extract the {@code type} for the {@link io.cloudevents.CloudEvent}.
	 * <p>
	 * Default is {@code spring.message}.
	 * @param typeExpression the SpEL expression to create the {@code type} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec typeExpression(String typeExpression) {
		return typeExpression(PARSER.parseExpression(typeExpression));
	}

	/**
	 * Set the {@link Function} to extract the {@code type} for the {@link io.cloudevents.CloudEvent}.
	 * <p>
	 * Default is {@code spring.message}.
	 * @param typeFunction the {@link Function} to create the {@code type} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec typeFunction(Function<Message<?>, String> typeFunction) {
		return typeExpression(new FunctionExpression<>(typeFunction));
	}

	/**
	 * Set the {@code type} for the {@link io.cloudevents.CloudEvent}.
	 * <p>
	 * Default is {@code spring.message}.
	 * @param type the {@code type} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec type(String type) {
		return typeExpression(new LiteralExpression(type));
	}

	/**
	 * Set the {@link Expression} to extract the {@code type} for the {@link io.cloudevents.CloudEvent}.
	 * <p>
	 * Default is {@code spring.message}.
	 * @param typeExpression the expression to create the {@code type} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec typeExpression(Expression typeExpression) {
		this.transformer.setTypeExpression(typeExpression);
		return this;
	}

	/**
	 * Set the SpEL expression to create the {@code dataSchema} for the {@link io.cloudevents.CloudEvent}.
	 * @param dataSchemaExpression the SpEL expression to create the {@code dataSchema} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec dataSchemaExpression(String dataSchemaExpression) {
		return dataSchemaExpression(PARSER.parseExpression(dataSchemaExpression));
	}

	/**
	 * Set the {@link Function} to create the {@code dataSchema} for the {@link io.cloudevents.CloudEvent}.
	 * @param dataSchemaFunction the {@link Function} to create the {@code dataSchema} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec dataSchemaFunction(Function<Message<?>, URI> dataSchemaFunction) {
		return dataSchemaExpression(new FunctionExpression<>(dataSchemaFunction));
	}

	/**
	 * Set the {@code dataSchema} for the {@link io.cloudevents.CloudEvent}.
	 * @param dataSchema the {@code dataSchema} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec dataSchema(URI dataSchema) {
		return dataSchemaExpression(new ValueExpression<>(dataSchema));
	}

	/**
	 * Set the {@link Expression} to create the {@code dataSchema} for the {@link io.cloudevents.CloudEvent}.
	 * @param dataSchemaExpression the expression to create the {@code dataSchema} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec dataSchemaExpression(Expression dataSchemaExpression) {
		this.transformer.setDataSchemaExpression(dataSchemaExpression);
		return this;
	}

	/**
	 * Set the SpEL expression to create the {@code subject} for the {@link io.cloudevents.CloudEvent}.
	 * @param subjectExpression the SpEL expression to create the {@code subject} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec subjectExpression(String subjectExpression) {
		return subjectExpression(PARSER.parseExpression(subjectExpression));
	}

	/**
	 * Set the {@link Function} to create the {@code subject} for the {@link io.cloudevents.CloudEvent}.
	 * @param subjectFunction the {@link Function} to create the {@code subject} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec subjectFunction(Function<Message<?>, String> subjectFunction) {
		return subjectExpression(new FunctionExpression<>(subjectFunction));
	}

	/**
	 * Set the {@code subject} for the {@link io.cloudevents.CloudEvent}.
	 * @param subject the {@code subject} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec subject(String subject) {
		return subjectExpression(new LiteralExpression(subject));
	}

	/**
	 * Set the {@link Expression} to create the {@code subject} for the {@link io.cloudevents.CloudEvent}.
	 * @param subjectExpression the expression to create the {@code subject} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec subjectExpression(Expression subjectExpression) {
		this.transformer.setSubjectExpression(subjectExpression);
		return this;
	}

	/**
	 * Set the SpEL expression to produce a cloud event format content type
	 * when {@link io.cloudevents.core.provider.EventFormatProvider} is to be used to determine {@link EventFormat}.
	 * <p>
	 * If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has precedence.
	 * @param eventFormatContentTypeExpression the SpEL expression to evaluate a content type
	 * for the {@link io.cloudevents.core.provider.EventFormatProvider#resolveFormat(String)}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventFormatContentTypeExpression(String eventFormatContentTypeExpression) {
		return eventFormatContentTypeExpression(PARSER.parseExpression(eventFormatContentTypeExpression));
	}

	/**
	 * Set the {@link Function} to produce a cloud event format content type
	 * when {@link io.cloudevents.core.provider.EventFormatProvider} is to be used to determine {@link EventFormat}.
	 * <p>
	 * If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided, the {@code
	 * eventFormat} has precedence.
	 * @param eventFormatContentTypeFunction the {@link Function} to evaluate a content type
	 * for the {@link io.cloudevents.core.provider.EventFormatProvider#resolveFormat(String)}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventFormatContentTypeFunction(
			Function<Message<?>, String> eventFormatContentTypeFunction) {

		return eventFormatContentTypeExpression(new FunctionExpression<>(eventFormatContentTypeFunction));
	}

	/**
	 * Set the cloud event format content type
	 * when {@link io.cloudevents.core.provider.EventFormatProvider} is to be used to determine {@link EventFormat}.
	 * <p>
	 * If {@code eventFormat} and the {@code eventFormatContentType} are provided, the {@code eventFormat} has
	 * precedence.
	 * @param eventFormatContentType the {@link Function} to evaluate a content type
	 * for the {@link io.cloudevents.core.provider.EventFormatProvider#resolveFormat(String)}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventFormatContentType(String eventFormatContentType) {
		return eventFormatContentTypeExpression(new LiteralExpression(eventFormatContentType));
	}

	/**
	 * Set the {@link Expression} to produce a cloud event format content type
	 * when {@link io.cloudevents.core.provider.EventFormatProvider} is to be used to determine {@link EventFormat}.
	 * <p>
	 * If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has precedence.
	 * @param eventFormatContentTypeExpression the {@link Expression} to evaluate a content type
	 * for the {@link io.cloudevents.core.provider.EventFormatProvider#resolveFormat(String)}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventFormatContentTypeExpression(Expression eventFormatContentTypeExpression) {
		this.transformer.setEventFormatContentTypeExpression(eventFormatContentTypeExpression);
		return this;
	}

	/**
	 * Set the prefix for {@link io.cloudevents.CloudEvent} headers in binary content mode.
	 * <p>
	 * Default is {@link org.springframework.integration.cloudevents.CloudEventHeaders#PREFIX}.
	 * @param cloudEventPrefix the prefix to use for {@link io.cloudevents.CloudEvent} headers
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec cloudEventPrefix(String cloudEventPrefix) {
		this.transformer.setCloudEventPrefix(cloudEventPrefix);
		return this;
	}

	/**
	 * Get the {@link ToCloudEventTransformer} instance created and configured by this builder.
	 * <p>
	 * This method provides access to the transformer for advanced configuration or direct use outside the DSL
	 * context.
	 * @return the configured {@link ToCloudEventTransformer}
	 */
	public ToCloudEventTransformer get() {
		return this.transformer;
	}

}
