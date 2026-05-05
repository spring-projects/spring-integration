/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.redis.dsl;

import java.time.Duration;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.redis.inbound.ReactiveRedisStreamMessageProducer;

/**
 * A {@link MessageProducerSpec} for a {@link ReactiveRedisStreamMessageProducer}.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public class ReactiveRedisInboundChannelAdapterSpec extends
		MessageProducerSpec<ReactiveRedisInboundChannelAdapterSpec, ReactiveRedisStreamMessageProducer> {

	protected ReactiveRedisInboundChannelAdapterSpec(ReactiveRedisConnectionFactory connectionFactory,
			String streamKey) {

		this.target = new ReactiveRedisStreamMessageProducer(connectionFactory, streamKey);
	}

	/**
	 * Specify the offset from which to read message.
	 * @param readOffset the readOffset
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setReadOffset(ReadOffset)
	 */
	public ReactiveRedisInboundChannelAdapterSpec readOffset(ReadOffset readOffset) {
		this.target.setReadOffset(readOffset);
		return this;
	}

	/**
	 * Specify whether extract payload.
	 * @param extractPayload the extractPayload
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setExtractPayload(boolean)
	 */
	public ReactiveRedisInboundChannelAdapterSpec extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return this;
	}

	/**
	 * Specify whether acknowledge message read in the Consumer Group.
	 * @param autoAck the acknowledge option
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setAutoAck(boolean)
	 */
	public ReactiveRedisInboundChannelAdapterSpec autoAck(boolean autoAck) {
		this.target.setAutoAck(autoAck);
		return this;
	}

	/**
	 * Specify the name of the consumer group.
	 * @param consumerGroup the consumerGroup
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setConsumerGroup(String)
	 */
	public ReactiveRedisInboundChannelAdapterSpec consumerGroup(String consumerGroup) {
		this.target.setConsumerGroup(consumerGroup);
		return this;
	}

	/**
	 * Specify the name of the consumer.
	 * @param consumerName the consumerName
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setConsumerName(String)
	 */
	public ReactiveRedisInboundChannelAdapterSpec consumerName(@Nullable String consumerName) {
		this.target.setConsumerName(consumerName);
		return this;
	}

	/**
	 * Specify whether create the consumer group.
	 * @param createConsumerGroup the createConsumerGroup
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setCreateConsumerGroup(boolean)
	 */
	public ReactiveRedisInboundChannelAdapterSpec createConsumerGroup(boolean createConsumerGroup) {
		this.target.setCreateConsumerGroup(createConsumerGroup);
		return this;
	}

	/**
	 * Specify the streamReceiverOptions to customize the {@link StreamReceiver}.
	 * @param streamReceiverOptions the streamReceiverOptions
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)
	 */
	public ReactiveRedisInboundChannelAdapterSpec streamReceiverOptions(
			StreamReceiver.@Nullable StreamReceiverOptions<String, ?> streamReceiverOptions) {

		this.target.setStreamReceiverOptions(streamReceiverOptions);
		return this;
	}

	/**
	 * Specify the poll timeout for the BLOCK option.
	 * @param pollTimeout the pollTimeout
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setPollTimeout(Duration)
	 */
	public ReactiveRedisInboundChannelAdapterSpec pollTimeout(Duration pollTimeout) {
		this.target.setPollTimeout(pollTimeout);
		return this;
	}

	/**
	 * Specify the batch size for the COUNT option.
	 * @param recordsPerPoll the recordsPerPoll
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setBatchSize(int)
	 */
	public ReactiveRedisInboundChannelAdapterSpec batchSize(int recordsPerPoll) {
		this.target.setBatchSize(recordsPerPoll);
		return this;
	}

	/**
	 * Specify the resume Function when polling the stream fails.
	 * @param resumeFunction the resumeFunction
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setOnErrorResume(Function)
	 */
	public ReactiveRedisInboundChannelAdapterSpec errorResumeFunction(
			Function<? super Throwable, ? extends Publisher<Void>> resumeFunction) {

		this.target.setOnErrorResume(resumeFunction);
		return this;
	}

	/**
	 * Specify the key, hash key and hash value serializer.
	 * @param pair the pair
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setSerializer(RedisSerializationContext.SerializationPair)
	 */
	public ReactiveRedisInboundChannelAdapterSpec serializer(RedisSerializationContext.SerializationPair<?> pair) {
		this.target.setSerializer(pair);
		return this;
	}

	/**
	 * Specify the hash target type.
	 * @param targetType the targetType
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setTargetType(Class)
	 */
	public ReactiveRedisInboundChannelAdapterSpec targetType(Class<?> targetType) {
		this.target.setTargetType(targetType);
		return this;
	}

	/**
	 * Specify the hashMapper.
	 * @param hashMapper the hashMapper
	 * @return the spec
	 * @see ReactiveRedisStreamMessageProducer#setObjectMapper(HashMapper)
	 */
	public ReactiveRedisInboundChannelAdapterSpec objectMapper(HashMapper<?, ?, ?> hashMapper) {
		this.target.setObjectMapper(hashMapper);
		return this;
	}

}
