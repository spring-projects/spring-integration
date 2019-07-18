/*
 * Copyright 2019 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.support.AvroHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An Apache Avro transformer for generated {@link SpecificRecord} objects.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SimpleToAvroTransformer extends AbstractTransformer {

	private final EncoderFactory encoderFactory = new EncoderFactory();

	private Expression typeIdExpression =
			new FunctionExpression<Message<?>>((message) -> message.getPayload().getClass());

	private EvaluationContext evaluationContext;

	/**
	 * Set the expression to evaluate against the message to determine the value
	 * for the {@link AvroHeaders#TYPE} header.
	 * @param expression the expression.
	 * @return the transformer
	 */
	public SimpleToAvroTransformer typeExpression(Expression expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = expression;
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the value
	 * for the {@link AvroHeaders#TYPE} header.
	 * @param expression the expression.
	 * @return the transformer
	 */
	public SimpleToAvroTransformer typeExpression(String expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the value
	 * for the {@link AvroHeaders#TYPE} header.
	 * @param expression the expression.
	 */
	public void setTypeExpression(Expression expression) {
		assertExpressionNotNull(expression);
		this.typeIdExpression = expression;
	}

	/**
	 * Set the expression to evaluate against the message to determine the value
	 * for the {@link AvroHeaders#TYPE} header.
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

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.state(message.getPayload() instanceof SpecificRecord,
				"Payload must be an implementation of 'SpecificRecord'");
		SpecificRecord specific = (SpecificRecord) message.getPayload();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BinaryEncoder encoder = this.encoderFactory.directBinaryEncoder(out, null);
		DatumWriter<Object> writer = new SpecificDatumWriter<>(specific.getSchema());
		try {
			writer.write(specific, encoder);
			encoder.flush();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return getMessageBuilderFactory().withPayload(out.toByteArray())
				.copyHeaders(message.getHeaders())
				.setHeader(AvroHeaders.TYPE, this.typeIdExpression.getValue(this.evaluationContext, message))
				.build();
	}

}
