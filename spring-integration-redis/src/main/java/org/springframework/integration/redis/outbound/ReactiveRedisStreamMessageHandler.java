/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.redis.outbound;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReactiveMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

/**
 * Implementation of {@link org.springframework.messaging.ReactiveMessageHandler} which writes
 * Message payload into a Redis stream , using reactive stream operation.
 *
 * @author Attoumane Ahamadi
 *
 * @since 5.4
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReactiveRedisStreamMessageHandler extends AbstractReactiveMessageHandler {

	private final Expression streamKeyExpression;

	private volatile EvaluationContext evaluationContext;

	private boolean extractPayload = true;

	private ReactiveStreamOperations reactiveStreamOperations;

	private RedisSerializationContext serializationContext = RedisSerializationContext.string();

	private final ReactiveRedisConnectionFactory connectionFactory;

	@Nullable
	private HashMapper hashMapper;

	public ReactiveRedisStreamMessageHandler(ReactiveRedisConnectionFactory connectionFactory, Expression streamKeyExpression) {
		Assert.notNull(streamKeyExpression, "'streamKeyExpression' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.streamKeyExpression = streamKeyExpression;
		this.connectionFactory = connectionFactory;
	}

	public ReactiveRedisStreamMessageHandler(ReactiveRedisConnectionFactory connectionFactory, String streamKey) {
		this(connectionFactory, new LiteralExpression(streamKey));
	}

	public void setSerializationContext(RedisSerializationContext serializationContext) {
		Assert.notNull(serializationContext, "'serializationContext' must not be null");
		this.serializationContext = serializationContext;
	}

	public void setHashMapper(@Nullable HashMapper hashMapper) {
		this.hashMapper = hashMapper;
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "redis:stream-outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.evaluationContext == null && getBeanFactory() != null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}
		ReactiveRedisTemplate template = new ReactiveRedisTemplate<>(this.connectionFactory,
				this.serializationContext);
		this.reactiveStreamOperations = this.hashMapper == null ? template.opsForStream() :
				template.opsForStream(this.hashMapper);
	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {

		String streamKey = this.streamKeyExpression.getValue(this.evaluationContext, message, String.class);

		Object value = message;
		if (this.extractPayload) {
			value = message.getPayload();
		}

		ObjectRecord record = StreamRecords
				.objectBacked(value)
				.withStreamKey(streamKey);

		return this.reactiveStreamOperations.add(record);
	}

}
