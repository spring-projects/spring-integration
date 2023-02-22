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
 * A Protocol Buffer transformer to instantiate {@link com.google.protobuf.Message} objects from either {@code byte[]}
 * in case of application/x-protobuf content type or from {@code String} in case of application/json.
 *
 * @author Christian Tzolov
 * @since 6.1
 */
public class FromProtobufTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private final ProtobufMessageConverter protobufMessageConverter;

	private Class<? extends com.google.protobuf.Message> expectedType;

	private ClassLoader beanClassLoader;

	private Expression expectedTypeExpression = new FunctionExpression<Message<?>>(
			(message) -> message.getHeaders().get(ProtoHeaders.TYPE));

	private EvaluationContext evaluationContext;

	/**
	 * Construct an instance with the supplied default type to create.
	 */
	public FromProtobufTransformer() {
		this(null, new ProtobufMessageConverter());
	}

	/**
	 * Construct an instance with the supplied default type to create.
	 * @param expectedType the type.
	 */
	public FromProtobufTransformer(Class<? extends com.google.protobuf.Message> expectedType) {
		this(expectedType, new ProtobufMessageConverter());
	}

	/**
	 * Construct an instance with the supplied default type and ProtobufMessageConverter instance.
	 * @param expectedType the type.
	 * @param protobufMessageConverter the message converter used.
	 */
	public FromProtobufTransformer(Class<? extends com.google.protobuf.Message> expectedType,
			ProtobufMessageConverter protobufMessageConverter) {
		Assert.notNull(expectedType, "'defaultType' must not be null");
		Assert.notNull(protobufMessageConverter, "'protobufMessageConverter' must not be null");
		this.expectedType = expectedType;
		this.protobufMessageConverter = protobufMessageConverter;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set the expected protobuf class type.
	 * @param expectedType expected protobuf class type.
	 */
	public void setExpectedType(Class<? extends com.google.protobuf.Message> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * Get the Protobuf expected class, if configured.
	 * @return returns the expected Protocol Buffer class.
	 */
	public Class<? extends com.google.protobuf.Message> getExpectedType() {
		return expectedType;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type. Default {@code headers['proto_type']}.
	 * @param expression the expression.
	 */
	public void setExpectedTypeExpression(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.expectedTypeExpression = expression;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id. Default
	 * {@code headers['proto_type']}.
	 * @param expression the expression.
	 */
	public void setExpectedTypeExpressionString(String expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.expectedTypeExpression = EXPRESSION_PARSER.parseExpression(expression);
	}

	@Override
	protected void onInit() {
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doTransform(Message<?> message) {

		Class<? extends com.google.protobuf.Message> targetClass = null;
		Object value = this.expectedTypeExpression.getValue(this.evaluationContext, message);
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
			targetClass = this.expectedType;
		}

		if (targetClass == null) {
			this.logger.error(() -> "The 'expectedTypeExpression' (" + this.expectedTypeExpression
					+ ") returned 'null' for: "
					+ message + ". No falling back expectedType is configured. Consider setting the expectedType.");
			throw new RuntimeException("The 'expectedTypeExpression' (" + this.expectedTypeExpression
					+ ") returned 'null' for: "
					+ message + ". No falling back expectedType is configured. Consider setting the expectedType.");
		}

		return this.protobufMessageConverter.fromMessage(message, targetClass);
	}

	public void setEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}
}
