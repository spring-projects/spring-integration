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
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.transformer.support.AvroHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An Apache Avro transformer to create generated {@link SpecificRecord} objects
 * from {@code byte[]}.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SimpleFromAvroTransformer extends AbstractTransformer {

	private final Class<? extends SpecificRecord> defaultType;

	private final Map<String, Class<? extends SpecificRecord>> typeMappings = new HashMap<>();

	private final DecoderFactory decoderFactory = new DecoderFactory();

	private Expression typeIdExpression =
			EXPRESSION_PARSER.parseExpression("headers['" + AvroHeaders.TYPE_ID + "']");

	private EvaluationContext evaluationContext;

	/**
	 * Construct an instance with the supplied default type to create.
	 * @param defaultType the type.
	 */
	public SimpleFromAvroTransformer(Class<? extends SpecificRecord> defaultType) {
		Assert.notNull(defaultType, "'defaultType' must not be null");
		this.defaultType = defaultType;
	}

	/**
	 * Set type mappings.
	 * @param typesToMap the types to map.
	 * @return the transformer.
	 * @see #typeIdExpression
	 */
	public SimpleFromAvroTransformer typeMappings(Map<String, Class<? extends SpecificRecord>> typesToMap) {
		Assert.notNull(typesToMap, "'typeMappings' must not be null");
		this.typeMappings.putAll(typesToMap);
		return this;
	}

	/**
	 * Add an individual type mapping.
	 * @param typeId the type id.
	 * @param clazz the type.
	 * @return the transformer.
	 */
	public SimpleFromAvroTransformer typeMapping(String typeId, Class<? extends SpecificRecord> clazz) {
		Assert.notNull(typeId, "'typeId' must not be null");
		Assert.notNull(clazz, "'clazz' must not be null");
		this.typeMappings.put(typeId, clazz);
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id.
	 * Default {@code headers['avro_typeId']}.
	 * @param expression the expression.
	 * @return the transformer
	 * @see #typeMappings
	 */
	public SimpleFromAvroTransformer typeIdExpression(Expression expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.typeIdExpression = expression;
		return this;
	}

	/**
	 * Set the expression to evaluate against the message to determine the type id.
	 * Default {@code headers['avro_typeId']}.
	 * @param expression the expression.
	 * @return the transformer
	 * @see #typeMappings
	 */
	public SimpleFromAvroTransformer typeIdExpression(String expression) {
		Assert.notNull(expression, "'expression' must not be null");
		this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
		return this;
	}

	@Override
	protected void onInit() {
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Assert.state(message.getPayload() instanceof byte[], "Payload must be a byte[]");
		Class<? extends SpecificRecord> type = null;
		String typeId = this.typeIdExpression.getValue(this.evaluationContext, message, String.class);
		if (typeId != null) {
			type = this.typeMappings.get(typeId);
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
