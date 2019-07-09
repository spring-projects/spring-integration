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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * @since 5.2
 *
 */
public class SimpleFromAvroTransformer extends AbstractTransformer implements BeanClassLoaderAware {

	private final Class<? extends SpecificRecord> defaultType;

	private final DecoderFactory decoderFactory = new DecoderFactory();

	private final Map<String, Class<? extends SpecificRecord>> typeCache = new ConcurrentHashMap<>();

	private Expression typeIdExpression = new FunctionExpression<Message<?>>(
			msg -> msg.getHeaders().get(AvroHeaders.TYPE));

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
		Assert.notNull(expression, "'expression' must not be null");
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
		Assert.notNull(expression, "'expression' must not be null");
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type.
	 * Default {@code headers['avro_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpression(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.typeIdExpression = expression;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id.
	 * Default {@code headers['avro_type']}.
	 * @param expression the expression.
	 */
	public void setTypeExpression(String expression) {
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
		Assert.state(message.getPayload() instanceof byte[], "Payload must be a byte[]");
		Class<? extends SpecificRecord> type = null;
		Object value = this.typeIdExpression.getValue(this.evaluationContext, message);
		if (value instanceof Class) {
			type = (Class<? extends SpecificRecord>) value;
		}
		else if (value instanceof String) {
			if (this.typeCache.containsKey(value)) {
				type = this.typeCache.get(value);
			}
			else {
				try {
					type = (Class<? extends SpecificRecord>) ClassUtils.forName((String) value, this.beanClassLoader);
					this.typeCache.put((String) value, type);
				}
				catch (ClassNotFoundException | LinkageError e) {
					throw new IllegalStateException(e);
				}
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
