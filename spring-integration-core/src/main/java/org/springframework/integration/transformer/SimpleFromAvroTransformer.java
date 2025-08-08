/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.support.AvroHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An Apache Avro transformer to create generated {@link SpecificRecord} objects
 * from {@code byte[]}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public class SimpleFromAvroTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private final Class<? extends SpecificRecord> defaultType;

	private final DecoderFactory decoderFactory = new DecoderFactory();

	private Expression typeIdExpression =
			new FunctionExpression<Message<?>>((message) -> message.getHeaders().get(AvroHeaders.TYPE));

	private EvaluationContext evaluationContext;

	private ClassLoader beanClassLoader;

	/**
	 * Construct an instance with the supplied default type to create.
	 * @param defaultType the type.
	 */
	public SimpleFromAvroTransformer(Class<? extends SpecificRecord> defaultType) {
		Assert.notNull(defaultType, "'defaultType' must not be null");
		this.defaultType = defaultType;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type.
	 * Default {@code headers['avro_type']}.
	 * @param expression the expression.
	 * @return the transformer
	 */
	public SimpleFromAvroTransformer typeExpression(Expression expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = expression;
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id.
	 * Default {@code headers['avro_type']}.
	 * @param expression the expression.
	 * @return the transformer
	 */
	public SimpleFromAvroTransformer typeExpression(String expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type.
	 * Default {@code headers['avro_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpression(Expression expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = expression;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id.
	 * Default {@code headers['avro_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpressionString(String expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
	}

	private void assertExpressionNotNull(Object expression) {
		Assert.notNull(expression, "'expression' must not be null");
	}

	@Override
	protected void onInit() {
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doTransform(Message<?> message) {
		Assert.state(message.getPayload() instanceof byte[], "Payload must be a byte[]");
		Class<? extends SpecificRecord> type = null;
		Object value = this.typeIdExpression.getValue(this.evaluationContext, message);
		if (value instanceof Class) {
			type = (Class<? extends SpecificRecord>) value;
		}
		else if (value instanceof String) {
			try {
				type = (Class<? extends SpecificRecord>) ClassUtils.forName((String) value, this.beanClassLoader);
			}
			catch (ClassNotFoundException | LinkageError e) {
				throw new IllegalStateException(e);
			}
		}
		if (type == null) {
			type = this.defaultType;
		}
		DatumReader<?> reader = new SpecificDatumReader<>(type);
		try {
			return reader.read(null, this.decoderFactory.binaryDecoder((byte[]) message.getPayload(), null));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
