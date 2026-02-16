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

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.cloudevents.CloudEventHeaders;
import org.springframework.integration.cloudevents.transformer.ToCloudEventTransformer;
import org.springframework.integration.dsl.ConsumerEndpointSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Spec for an {@link ToCloudEventTransformer}.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class ToCloudEventTransformerSpec
		extends ConsumerEndpointSpec<ToCloudEventTransformerSpec, MessageTransformingHandler> {

	private final ToCloudEventTransformer transformer;

	/**
	 * Create an instance with no extension patterns.
	 */
	protected ToCloudEventTransformerSpec() {
		this(new String[0]);
	}

	/**
	 * Create an instance with the provided extension patterns.
	 * <p>Extension patterns are used to match message headers that should be included
	 * as {@link CloudEvent} extensions. Patterns support wildcards (e.g., "myapp.*").
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 *                          to the {@link CloudEvent}.
	 */
	protected ToCloudEventTransformerSpec(String... extensionPatterns) {
		this.transformer = new ToCloudEventTransformer(extensionPatterns);
		this.handler = new MessageTransformingHandler(this.transformer);
	}

	/**
	 * Set the {@link EventFormat} to use for {@link io.cloudevents.CloudEvent} serialization.
	 * <p>If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has precedence.
	 * @param eventFormat the event format for serializing CloudEvents
	 * @return the spec for method chaining
	 */
	public ToCloudEventTransformerSpec eventFormat(EventFormat eventFormat) {
		this.transformer.setEventFormat(eventFormat);
		return this;
	}

	/**
	 * Set the {@link Expression} to create {@link io.cloudevents.CloudEvent} {@code id}.
	 * <p>Default is to extract the {@code id} from the {@link MessageHeaders} of the message.
	 * @param eventIdExpression the expression to create the {@code id} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventIdExpression(Expression eventIdExpression) {
		this.transformer.setEventIdExpression(eventIdExpression);
		return this;
	}

	/**
	 * Set the {@link Function} to create {@link io.cloudevents.CloudEvent} {@code id}.
	 * <p>Default is to extract the {@code id} from the {@link MessageHeaders} of the message.
	 * @param eventIdFunction the {@link Function} to create the {@code id} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventIdExpression(Function<Message<?>, String> eventIdFunction) {
		return eventIdExpression(new FunctionExpression<>(eventIdFunction));
	}

	/**
	 * Set the {@link Expression} to create {@link io.cloudevents.CloudEvent} {@code source}.
	 * <p>Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceExpression the expression to create the {@code source} for each {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec sourceExpression(Expression sourceExpression) {
		this.transformer.setSourceExpression(sourceExpression);
		return this;
	}

	/**
	 * Set the {@link Function} to create {@link io.cloudevents.CloudEvent} {@code source}.
	 * <p>Default is {@code "/spring/" + appName + "." + getBeanName())}.
	 * @param sourceFunction the {@link Function} to create the {@code source} for each
	 *                       {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec sourceExpression(Function<Message<?>, String> sourceFunction) {
		return sourceExpression(new FunctionExpression<>(sourceFunction));
	}

	/**
	 * Set the {@link Expression} to extract the {@code type} for the {@link io.cloudevents.CloudEvent}.
	 * @param typeExpression the expression to create the {@code type} for each {@link io.cloudevents.CloudEvent}
	 *                       <p>Default is {@code spring.message}.
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec typeExpression(Expression typeExpression) {
		this.transformer.setTypeExpression(typeExpression);
		return this;
	}

	/**
	 * Set the {@link Function} to extract the {@code type} for the {@link io.cloudevents.CloudEvent}.
	 * @param typeFunction the {@link Function} to create the {@code type} for each {@link io.cloudevents.CloudEvent}
	 *                     <p>Default is {@code spring.message}.
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec typeExpression(Function<Message<?>, String> typeFunction) {
		return typeExpression(new FunctionExpression<>(typeFunction));
	}

	/**
	 * Set the {@link Expression} to create the {@code dataSchema} for the {@link io.cloudevents.CloudEvent}.
	 * @param dataSchemaExpression the expression to create the {@code dataSchema} for each
	 *                             {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec dataSchemaExpression(Expression dataSchemaExpression) {
		this.transformer.setDataSchemaExpression(dataSchemaExpression);
		return this;
	}

	/**
	 * Set the {@link Function} to create the {@code dataSchema} for the {@link io.cloudevents.CloudEvent}.
	 * @param dataSchemaFunction the {@link Function} to create the {@code dataSchema} for each
	 *                           {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec dataSchemaExpression(Function<Message<?>, String> dataSchemaFunction) {
		return dataSchemaExpression(new FunctionExpression<>(dataSchemaFunction));
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
	 * Set the {@link Function} to create the {@code subject} for the {@link io.cloudevents.CloudEvent}.
	 * @param subjectFunction the {@link Function} to create the {@code subject} for each
	 * {@link io.cloudevents.CloudEvent}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec subjectExpression(Function<Message<?>, String> subjectFunction) {
		return subjectExpression(new FunctionExpression<>(subjectFunction));
	}

	/**
	 * Set the {@link Expression} to produce a cloud event format content type
	 * when {@link EventFormatProvider} is to be used to determine
	 * {@link EventFormat}.
	 * <p>If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has precedence.
	 * @param eventFormatContentTypeExpression the {@link Function} to create
	 *                                         content type for the {@link EventFormatProvider#resolveFormat(String)}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventFormatContentTypeExpression(Expression eventFormatContentTypeExpression) {
		this.transformer.setEventFormatContentTypeExpression(eventFormatContentTypeExpression);
		return this;
	}

	/**
	 * Set the {@link Function} to produce a cloud event format content type
	 * when {@link EventFormatProvider} is to be used to determine
	 * {@link EventFormat}.
	 * <p>If {@code eventFormat} and the {@code eventFormatContentTypeExpression} are provided,
	 * the {@code eventFormat} has precedence.
	 * @param eventFormatContentTypeFunction the {@link Function} to create
	 *                                       content type for the {@link EventFormatProvider#resolveFormat(String)}
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec eventFormatContentTypeExpression(
			Function<Message<?>, String> eventFormatContentTypeFunction) {
		return eventFormatContentTypeExpression(new FunctionExpression<>(eventFormatContentTypeFunction));
	}

	/**
	 * Set the prefix for {@link io.cloudevents.CloudEvent} headers in binary content mode.
	 * <p>Default is {@link CloudEventHeaders#PREFIX}.
	 * @param cloudEventPrefix the prefix to use for {@link CloudEvent} headers
	 * @return the spec
	 */
	public ToCloudEventTransformerSpec cloudEventPrefix(String cloudEventPrefix) {
		this.transformer.setCloudEventPrefix(cloudEventPrefix);
		return this;
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.singletonMap(this.transformer, null);
	}

	/**
	 * Get the {@link ToCloudEventTransformer} instance.
	 * <p>This method provides access to the transformer for advanced configuration
	 * or direct use outside of the DSL context.
	 * @return the configured {@link ToCloudEventTransformer}
	 */
	public ToCloudEventTransformer get() {
		return this.transformer;
	}

}
