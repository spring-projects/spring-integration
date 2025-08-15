/*
 * Copyright 2020-present the original author or authors.
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

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.Record;
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
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.messaging.ReactiveMessageHandler} which writes
 * Message payload or Message itself (see {@link #extractPayload}) into a Redis stream using Reactive Stream operations.
 *
 * @author Attoumane Ahamadi
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ReactiveRedisStreamMessageHandler extends AbstractReactiveMessageHandler {

	private final Expression streamKeyExpression;

	private final ReactiveRedisConnectionFactory connectionFactory;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	private boolean extractPayload = true;

	@SuppressWarnings("NullAway.Init")
	private ReactiveStreamOperations<String, ?, ?> reactiveStreamOperations;

	private RedisSerializationContext<String, ?> serializationContext = RedisSerializationContext.string();

	@Nullable
	private HashMapper<String, ?, ?> hashMapper;

	@Nullable
	private Function<Message<?>, RedisStreamCommands.XAddOptions> addOptionsFunction;

	/**
	 * Create an instance based on provided {@link ReactiveRedisConnectionFactory} and key for stream.
	 * @param connectionFactory the {@link ReactiveRedisConnectionFactory} to use
	 * @param streamKey the key for stream
	 */
	public ReactiveRedisStreamMessageHandler(ReactiveRedisConnectionFactory connectionFactory, String streamKey) {
		this(connectionFactory, new LiteralExpression(streamKey));
	}

	/**
	 * Create an instance based on provided {@link ReactiveRedisConnectionFactory} and expression for stream key.
	 * @param connectionFactory the {@link ReactiveRedisConnectionFactory} to use
	 * @param streamKeyExpression the SpEL expression to evaluate a key for stream
	 */
	public ReactiveRedisStreamMessageHandler(ReactiveRedisConnectionFactory connectionFactory,
			Expression streamKeyExpression) {

		Assert.notNull(streamKeyExpression, "'streamKeyExpression' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.streamKeyExpression = streamKeyExpression;
		this.connectionFactory = connectionFactory;
	}

	public void setSerializationContext(RedisSerializationContext<String, ?> serializationContext) {
		Assert.notNull(serializationContext, "'serializationContext' must not be null");
		this.serializationContext = serializationContext;
	}

	/**
	 * (Optional) Set the {@link HashMapper} used to create {@link #reactiveStreamOperations}.
	 * The default {@link HashMapper} is defined from the provided {@link RedisSerializationContext}
	 * @param hashMapper the wanted hashMapper
	 * */
	public void setHashMapper(@Nullable HashMapper<String, ?, ?> hashMapper) {
		this.hashMapper = hashMapper;
	}

	/**
	 * Set to {@code true} to extract the payload; otherwise
	 * the entire message is sent. Default {@code true}.
	 * @param extractPayload false to not extract.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	/**
	 * Set a function to create a {@link RedisStreamCommands.XAddOptions} based on the request message.
	 * Cannot be null and cannot return null.
	 * @param addOptionsFunction the function to provide a {@link RedisStreamCommands.XAddOptions}.
	 * @since 6.5
	 */
	public void setAddOptionsFunction(Function<Message<?>, RedisStreamCommands.XAddOptions> addOptionsFunction) {
		this.addOptionsFunction = addOptionsFunction;
	}

	@Override
	public String getComponentType() {
		return "redis:stream-outbound-channel-adapter";
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void onInit() {
		super.onInit();

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());

		ReactiveRedisTemplate<String, ?> template =
				new ReactiveRedisTemplate<>(this.connectionFactory, this.serializationContext);
		this.reactiveStreamOperations =
				this.hashMapper == null
						? template.opsForStream()
						: template.opsForStream(
						(HashMapper<? super String, ? super Object, ? super Object>) this.hashMapper);
	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {
		return Mono
				.fromSupplier(() -> {
					String streamKey = this.streamKeyExpression.getValue(this.evaluationContext, message, String.class);
					Assert.notNull(streamKey, "'streamKey' must not be null");
					return streamKey;
				})
				.flatMap((streamKey) -> {
					Object value = message;
					if (this.extractPayload) {
						value = message.getPayload();
					}

					Record<String, ?> record =
							StreamRecords.objectBacked(value)
									.withStreamKey(streamKey);

					if (this.addOptionsFunction == null) {
						return this.reactiveStreamOperations.add(record);
					}
					else {
						return this.reactiveStreamOperations.add(record, this.addOptionsFunction.apply(message));
					}
				})
				.then();
	}

}
