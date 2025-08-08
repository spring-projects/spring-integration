/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.transformer.support.ProtoHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ProtobufMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A Protocol Buffer transformer to instantiate {@link com.google.protobuf.Message} objects
 * from either {@code byte[]} if content type is {@code application/x-protobuf}
 * or from {@code String} in case of {@code application/json} content type.
 *
 * @author Christian Tzolov
 *
 * @since 6.1
 */
public class FromProtobufTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private final ProtobufMessageConverter protobufMessageConverter;

	private ClassLoader beanClassLoader;

	private Expression expectedTypeExpression =
			new FunctionExpression<Message<?>>((message) -> message.getHeaders().get(ProtoHeaders.TYPE));

	private EvaluationContext evaluationContext;

	/**
	 * Construct an instance with the supplied default type to create.
	 */
	public FromProtobufTransformer() {
		this(new ProtobufMessageConverter());
	}

	/**
	 * Construct an instance with the supplied default type and ProtobufMessageConverter instance.
	 * @param protobufMessageConverter the message converter used.
	 */
	public FromProtobufTransformer(ProtobufMessageConverter protobufMessageConverter) {
		Assert.notNull(protobufMessageConverter, "'protobufMessageConverter' must not be null");
		this.protobufMessageConverter = protobufMessageConverter;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set an expected protobuf class type.
	 * Mutually exclusive with {@link #setExpectedTypeExpression} and
	 * {@link #setExpectedTypeExpressionString}.
	 * @param expectedType expected protobuf class type.
	 * @return updated FromProtobufTransformer instance.
	 */
	public FromProtobufTransformer setExpectedType(Class<? extends com.google.protobuf.Message> expectedType) {
		return setExpectedTypeExpression(new ValueExpression<>(expectedType));
	}

	/**
	 * Set an expression to evaluate against the message to determine the type id.
	 * Defaults to{@code headers['proto_type']}. Mutually exclusive with
	 * {@link #setExpectedType} and {@link #setExpectedTypeExpression}.
	 * @param expression the expression.
	 * @return updated FromProtobufTransformer instance.
	 */
	public FromProtobufTransformer setExpectedTypeExpressionString(String expression) {
		return setExpectedTypeExpression(EXPRESSION_PARSER.parseExpression(expression));
	}

	/**
	 * Set an expression to evaluate against the message to determine the type.
	 * Default {@code headers['proto_type']}.
	 * Mutually exclusive with {@link #setExpectedType} and
	 * {@link #setExpectedTypeExpressionString}.
	 * @param expression the expression.
	 * @return updated FromProtobufTransformer instance.
	 */
	public FromProtobufTransformer setExpectedTypeExpression(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.expectedTypeExpression = expression;
		return this;
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
		if (value instanceof Class<?>) {
			targetClass = (Class<? extends com.google.protobuf.Message>) value;
		}
		else if (value instanceof String) {
			try {
				targetClass =
						(Class<? extends com.google.protobuf.Message>)
								ClassUtils.forName((String) value, this.beanClassLoader);
			}
			catch (ClassNotFoundException | LinkageError e) {
				throw new IllegalStateException(e);
			}
		}

		if (targetClass == null) {
			throw new MessageTransformationException(message,
					"The 'expectedTypeExpression' (" + this.expectedTypeExpression + ") returned 'null'.");
		}

		return this.protobufMessageConverter.fromMessage(message, targetClass);
	}

}
