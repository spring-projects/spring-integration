/*
 * Copyright 2015-2021 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link AbstractPayloadTransformer} that delegates to a codec to decode the
 * payload from a byte[].
 *
 * @param <T> the payload type.
 *
 * @author Gary Russell
 *
 * @since 4.2
 */
public class DecodingTransformer<T> extends AbstractTransformer {

	private final Codec codec;

	private final Class<T> type;

	private final Expression typeExpression;

	private volatile StandardEvaluationContext evaluationContext;

	/**
	 * Construct an instance to use the supplied codec to decode to the supplied type.
	 * @param codec the codec.
	 * @param type the type.
	 */
	public DecodingTransformer(Codec codec, Class<T> type) {
		Assert.notNull(codec, "'codec' cannot be null");
		Assert.notNull(type, "'type' cannot be null");
		this.codec = codec;
		this.type = type;
		this.typeExpression = null;
	}

	/**
	 * Construct an instance to use the supplied codec to decode to the supplied type.
	 * @param codec the codec.
	 * @param typeExpression an expression that evaluates to a {@link Class}.
	 */
	public DecodingTransformer(Codec codec, Expression typeExpression) {
		Assert.notNull(codec, "'codec' cannot be null");
		Assert.notNull(typeExpression, "'typeExpression' cannot be null");
		this.codec = codec;
		this.type = null;
		this.typeExpression = typeExpression;
	}

	public void setEvaluationContext(StandardEvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	protected void onInit() {
		if (this.evaluationContext == null) {
			this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
		}
	}

	@Override
	protected T doTransform(Message<?> message) {
		Assert.isTrue(message.getPayload() instanceof byte[], "Message payload must be byte[]");
		byte[] bytes = (byte[]) message.getPayload();
		try {
			return this.codec.decode(bytes, this.type != null ? this.type : type(message));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private Class<T> type(Message<?> message) {
		Assert.state(this.evaluationContext != null, "EvaluationContext required");
		return this.typeExpression.getValue(this.evaluationContext, message, Class.class);
	}

}
