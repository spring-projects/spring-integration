/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.support.ProtoHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ProtobufMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A Protocol Buffer transformer to instantiate {@link com.google.protobuf.Message} objects from {@code byte[]}.
 *
 * @author Christian Tzolov
 * @since 6.1
 */
public class FromProtobufTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private final ProtobufMessageConverter protobufMessageConverter;

	private final Class<? extends com.google.protobuf.Message> defaultType;

	private ClassLoader beanClassLoader;

	private Expression typeIdExpression = new FunctionExpression<Message<?>>(
			(message) -> message.getHeaders().get(ProtoHeaders.TYPE));

	private EvaluationContext evaluationContext;

	/**
	 * Construct an instance with the supplied default type to create.
	 * @param defaultType the type.
	 */
	public FromProtobufTransformer(Class<? extends com.google.protobuf.Message> defaultType) {
		this(defaultType, new ProtobufMessageConverter());
	}

	/**
	 * Construct an instance with the supplied default type and ProtobufMessageConverter instance.
	 * @param defaultType the type.
	 * @param protobufMessageConverter the message converter used.
	 */
	public FromProtobufTransformer(Class<? extends com.google.protobuf.Message> defaultType,
			ProtobufMessageConverter protobufMessageConverter) {
		Assert.notNull(defaultType, "'defaultType' must not be null");
		Assert.notNull(protobufMessageConverter, "'protobufMessageConverter' must not be null");
		this.defaultType = defaultType;
		this.protobufMessageConverter = protobufMessageConverter;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type. Default {@code headers['proto_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpression(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.typeIdExpression = expression;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id. Default
	 * {@code headers['proto_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpressionString(String expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
	}

	@Override
	protected void onInit() {
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doTransform(Message<?> message) {

		Class<? extends com.google.protobuf.Message> targetClass = null;
		Object value = this.typeIdExpression.getValue(this.evaluationContext, message);
		if (value instanceof Class) {
			targetClass = (Class<? extends com.google.protobuf.Message>) value;
		}
		else if (value instanceof String) {
			try {
				targetClass = (Class<? extends com.google.protobuf.Message>) ClassUtils.forName((String) value,
						this.beanClassLoader);
			}
			catch (ClassNotFoundException | LinkageError e) {
				throw new IllegalStateException(e);
			}
		}
		if (targetClass == null) {
			this.logger.trace("Empty SpEL type expression. Fallback to the defaultType!");
			targetClass = this.defaultType;
		}

		return this.protobufMessageConverter.fromMessage(message, targetClass);
	}
}
