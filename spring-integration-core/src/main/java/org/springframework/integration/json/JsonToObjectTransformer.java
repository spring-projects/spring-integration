/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.json;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.ResolvableType;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * Transformer implementation that converts a JSON string payload into an instance of the
 * provided target Class. By default this transformer uses
 * {@linkplain org.springframework.integration.support.json.JsonObjectMapperProvider}
 * factory to get an instance of Jackson JSON-processor
 * if jackson-databind lib is present on the classpath. Any other {@linkplain JsonObjectMapper}
 * implementation can be provided.
 * <p> Since version 3.0, you can omit the target class and the target type can be
 * determined by the {@link JsonHeaders} type entries - including the contents of a
 * one-level container or map type.
 * <p> The type headers can be classes or fully-qualified class names.
 * <p> Since version 5.2.6, a SpEL expression option is provided to let to build a target
 *{@link ResolvableType} somehow externally.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @see JsonObjectMapper
 * @see org.springframework.integration.support.json.JsonObjectMapperProvider
 * @see ResolvableType
 */
public class JsonToObjectTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private final ResolvableType targetType;

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	private ClassLoader classLoader;

	private Expression valueTypeExpression =
			new FunctionExpression<Message<?>>((message) ->
					obtainResolvableTypeFromHeadersIfAny(message.getHeaders(), this.classLoader));

	private EvaluationContext evaluationContext;

	public JsonToObjectTransformer() {
		this((Class<?>) null);
	}

	public JsonToObjectTransformer(@Nullable Class<?> targetClass) {
		this(ResolvableType.forClass(targetClass));
	}

	/**
	 * Construct an instance based on the provided {@link ResolvableType}.
	 * @param targetType the {@link ResolvableType} to use.
	 * @since 5.2
	 */
	public JsonToObjectTransformer(ResolvableType targetType) {
		this(targetType, null);
	}

	public JsonToObjectTransformer(@Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {
		this((Class<?>) null, jsonObjectMapper);
	}

	public JsonToObjectTransformer(@Nullable Class<?> targetClass, @Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {
		this(ResolvableType.forClass(targetClass), jsonObjectMapper);
	}

	/**
	 * Construct an instance based on the provided {@link ResolvableType} and {@link JsonObjectMapper}.
	 * @param targetType the {@link ResolvableType} to use.
	 * @param jsonObjectMapper  the {@link JsonObjectMapper} to use.
	 * @since 5.2
	 */
	public JsonToObjectTransformer(ResolvableType targetType, @Nullable JsonObjectMapper<?, ?> jsonObjectMapper) {
		Assert.notNull(targetType, "'targetType' must not be null");
		this.targetType = targetType;
		this.jsonObjectMapper = (jsonObjectMapper != null) ? jsonObjectMapper : JsonObjectMapperProvider.newInstance();
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		if (this.jsonObjectMapper instanceof BeanClassLoaderAware) {
			((BeanClassLoaderAware) this.jsonObjectMapper).setBeanClassLoader(classLoader);
		}
	}

	/**
	 * Configure a SpEL expression to evaluate a {@link ResolvableType}
	 * to instantiate the payload from the incoming JSON.
	 * By default this transformer consults {@link JsonHeaders} in the request message.
	 * If this expression returns {@code null} or {@link ResolvableType} building throws a
	 * {@link ClassNotFoundException}, this transformer falls back to the provided {@link #targetType}.
	 * This logic is present as an expression because {@link JsonHeaders} may not have real class values,
	 * but rather some type ids which have to be mapped to target classes according some external registry.
	 * @param valueTypeExpressionString the SpEL expression to use.
	 * @since 5.2.6
	 */
	public void setValueTypeExpressionString(String valueTypeExpressionString) {
		setValueTypeExpression(EXPRESSION_PARSER.parseExpression(valueTypeExpressionString));
	}

	/**
	 * Configure a SpEL {@link Expression} to evaluate a {@link ResolvableType}
	 * to instantiate the payload from the incoming JSON.
	 * By default this transformer consults {@link JsonHeaders} in the request message.
	 * If this expression returns {@code null} or {@link ResolvableType} building throws a
	 * {@link ClassNotFoundException}, this transformer falls back to the provided {@link #targetType}.
	 * This logic is present as an expression because {@link JsonHeaders} may not have real class values,
	 * but rather some type ids which have to be mapped to target classes according some external registry.
	 * @param valueTypeExpression the SpEL {@link Expression} to use.
	 * @since 5.2.6
	 */
	public void setValueTypeExpression(Expression valueTypeExpression) {
		this.valueTypeExpression = valueTypeExpression;
	}

	@Override
	public String getComponentType() {
		return "json-to-object-transformer";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected Object doTransform(Message<?> message) {
		ResolvableType valueType = obtainResolvableType(message);

		boolean removeHeaders = false;
		if (valueType != null) {
			removeHeaders = true;
		}
		else {
			valueType = this.targetType;
		}

		Object result;
		try {
			result = this.jsonObjectMapper.fromJson(message.getPayload(), valueType);

		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (removeHeaders) {
			return getMessageBuilderFactory()
					.withPayload(result)
					.copyHeaders(message.getHeaders())
					.removeHeaders(JsonHeaders.HEADERS.toArray(new String[0]))
					.build();
		}
		else {
			return result;
		}
	}

	@Nullable
	private ResolvableType obtainResolvableType(Message<?> message) {
		try {
			return this.valueTypeExpression.getValue(this.evaluationContext, message, ResolvableType.class);
		}
		catch (Exception ex) {
			if (ex.getCause() instanceof ClassNotFoundException) {
				logger.debug("Cannot build a ResolvableType from the request message '" + message +
						"' evaluating expression '" + this.valueTypeExpression.getExpressionString() + "'", ex);
				return null;
			}
			else {
				throw ex;
			}
		}
	}

	@Nullable
	private static ResolvableType obtainResolvableTypeFromHeadersIfAny(MessageHeaders headers,
			ClassLoader classLoader) {

		Object valueType = headers.get(JsonHeaders.RESOLVABLE_TYPE);
		Object typeIdHeader = headers.get(JsonHeaders.TYPE_ID);
		if (!(valueType instanceof ResolvableType) && typeIdHeader != null) {
			valueType =
					JsonHeaders.buildResolvableType(classLoader, typeIdHeader,
							headers.get(JsonHeaders.CONTENT_TYPE_ID), headers.get(JsonHeaders.KEY_TYPE_ID));
		}
		return valueType instanceof ResolvableType
				? (ResolvableType) valueType
				: null;
	}

}
