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

package org.springframework.integration.redis.inbound;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * A {@link MessageProducerSupport} for Redis that reads message from a Redis Stream and publishes it to the provided
 * output channel.
 * By default this adapter reads message as a standalone client {@code XREAD} Redis command but we can switch to a
 * Consumer Group feature {@code XREADGROUP} by setting {@link #consumerGroupName} and {@link #consumerName} fields.
 *
 * @author Attoumane Ahamadi
 *
 * @since 5.4
 */
public class ReactiveRedisStreamMessageProducer extends MessageProducerSupport {

	private final ReactiveRedisConnectionFactory reactiveConnectionFactory;

	private final Expression streamKeyExpression;

	private EvaluationContext evaluationContext;

	private StreamReceiver.StreamReceiverOptions<String, ?> streamReceiverOptions = StreamReceiver
			.StreamReceiverOptions.builder().pollTimeout(Duration.ZERO).build();

	private StreamReceiver<String, ?> streamReceiver;

	private ReadOffset readOffset = ReadOffset.lastConsumed();

	private boolean extractPayload = true;

	private String streamKey;

	@Nullable
	private String consumerGroupName;

	@Nullable
	private String consumerName;

	private boolean createConsumerGroupIfNotExist;

	public ReactiveRedisStreamMessageProducer(ReactiveRedisConnectionFactory reactiveConnectionFactory,
			String streamKey) {
		this(reactiveConnectionFactory, new LiteralExpression(streamKey));
	}

	public ReactiveRedisStreamMessageProducer(ReactiveRedisConnectionFactory connectionFactory,
			Expression streamKeyExpression) {
		this.reactiveConnectionFactory = connectionFactory;
		this.streamKeyExpression = streamKeyExpression;
	}

	/**
	 * Define the offset from which we want to read message. By defaut the {@link ReadOffset#lastConsumed()} is used.
	 * @param readOffset the desired offset
	 */
	public void setReadOffset(ReadOffset readOffset) {
		this.readOffset = readOffset;
	}

	/**
	 * Configure this channel adapter to extract or not the message payload.
	 * @param extractPayload default true
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	/**
	 * Set the name of the Consumer Group. It is possible to create that Consumer Group if desired, see:
	 * {@link #createConsumerGroupIfNotExist}. Note that if this value is set, {@link #consumerName} should also be set
	 * @param consumerGroupName the Consumer Group which this adapter should register to listen messages.
	 */
	public void setConsumerGroupName(@Nullable String consumerGroupName) {
		this.consumerGroupName = consumerGroupName;
	}

	/**
	 * If the Consumer Group is used, this value should be provided. Note that this value should be unique in the group
	 * @param consumerName the conusmer name in the Consumer Group
	 */
	public void setConsumerName(@Nullable String consumerName) {
		this.consumerName = consumerName;
	}

	/**
	 * Create the Consumer Group if and only if it does not exist. During the
	 * creation we also create the stream {@see MKSTREAM}. If the stream already exists {@code MKSTREAM}
	 * has no effect.
	 * @param createConsumerGroupIfNotExist specify if we should create the Consumer Group, {@code false} by default
	 */
	public void setCreateConsumerGroupIfNotExist(boolean createConsumerGroupIfNotExist) {
		this.createConsumerGroupIfNotExist = createConsumerGroupIfNotExist;
	}

	/**
	 * Set {@link StreamOperations} used to customize the {@link StreamReceiver}.
	 * It provides a way to set the polling timeout and the serialization context.
	 * By default the polling timeout is set to infinite and {@link StringRedisSerializer} is used.
	 * @param streamReceiverOptions the desired receiver options
	 * */
	public void setStreamReceiverOptions(@Nullable StreamReceiver.StreamReceiverOptions<String, ?> streamReceiverOptions) {
		this.streamReceiverOptions = streamReceiverOptions;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.streamReceiverOptions != null) {
			this.streamReceiver = StreamReceiver.create(this.reactiveConnectionFactory, this.streamReceiverOptions);
		}
		else {
			this.streamReceiver = StreamReceiver.create(this.reactiveConnectionFactory);
		}
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected void doStart() {
		super.doStart();

		this.streamKey = this.streamKeyExpression.getValue(this.evaluationContext, String.class);

		Assert.notNull(this.streamKey, "'streamKey' must not be null");

		StreamOffset<String> offset = StreamOffset.create(this.streamKey, this.readOffset);

		Flux<Message<?>> messageFlux;

		if (StringUtils.isEmpty(this.consumerGroupName)) {
			messageFlux = this.streamReceiver
					.receive(offset)
					.map(event -> MessageBuilder.withPayload(this.extractPayload ? event.getValue() : event)
							.setHeader(RedisHeaders.STREAM_KEY, event.getStream())
							.setHeader(RedisHeaders.STREAM_MESSAGE_ID, event.getId())
							.build());
		}
		else {
			createConsumerGroup();
			Consumer consumer = Consumer.from(this.consumerGroupName, this.consumerName);
			messageFlux = this.streamReceiver
					.receiveAutoAck(consumer, offset)
					.map(event -> MessageBuilder.withPayload(this.extractPayload ? event.getValue() : event)
							.setHeader(RedisHeaders.STREAM_KEY, event.getStream())
							.setHeader(RedisHeaders.STREAM_MESSAGE_ID, event.getId())
							.build());
		}

		subscribeToPublisher(messageFlux);
	}

	@Override
	public String getComponentType() {
		return "redis:stream-inbound-channel-adapter";
	}

	private void createConsumerGroup() {
		if (this.createConsumerGroupIfNotExist) {
			Assert.hasText(this.consumerGroupName, "'consumerGroupName' must be set");
			Assert.hasText(this.consumerName, "'consumerName' must be set");

			this.reactiveConnectionFactory.getReactiveConnection()
					.streamCommands()
					.xGroupCreate(ByteBuffer.wrap(this.streamKey.getBytes(StandardCharsets.UTF_8)),
							this.consumerGroupName, this.readOffset, true);
		}
	}
}
