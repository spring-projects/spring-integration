/*
 * Copyright 2020-2021 the original author or authors.
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

import java.time.Duration;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.redis.support.RedisHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@link MessageProducerSupport} for reading messages from a Redis Stream and publishing them into the provided
 * output channel.
 * By default this adapter reads message as a standalone client {@code XREAD} (Redis command) but can be switched to a
 * Consumer Group feature {@code XREADGROUP} by setting {@link #consumerName} field.
 * By default the Consumer Group name is the id of this bean {@link #getBeanName()}.
 *
 * @author Attoumane Ahamadi
 * @author Artem Bilan
 * @author Rohan Mukesh
 *
 * @since 5.4
 */
public class ReactiveRedisStreamMessageProducer extends MessageProducerSupport {

	private final ReactiveRedisConnectionFactory reactiveConnectionFactory;

	private final String streamKey;

	private final StreamReceiver.StreamReceiverOptionsBuilder<String, ?> streamReceiverOptionsBuilder =
			StreamReceiver.StreamReceiverOptions.builder()
					.pollTimeout(Duration.ZERO)
					.onErrorResume(this::handleReceiverError);

	private ReactiveStreamOperations<String, ?, ?> reactiveStreamOperations;

	private StreamReceiver.StreamReceiverOptions<String, ?> streamReceiverOptions;

	private StreamReceiver<String, ?> streamReceiver;

	private ReadOffset readOffset = ReadOffset.latest();

	private boolean extractPayload = true;

	private boolean autoAck = true;

	@Nullable
	private String consumerGroup;

	@Nullable
	private String consumerName;

	private boolean createConsumerGroup;

	private boolean receiverBuilderOptionSet;

	public ReactiveRedisStreamMessageProducer(ReactiveRedisConnectionFactory reactiveConnectionFactory,
			String streamKey) {

		Assert.notNull(reactiveConnectionFactory, "'connectionFactory' must not be null");
		Assert.hasText(streamKey, "'streamKey' must be set");
		this.reactiveConnectionFactory = reactiveConnectionFactory;
		this.streamKey = streamKey;
	}

	/**
	 * Define the offset from which we want to read message. By default the {@link ReadOffset#latest()} is used.
	 * {@link ReadOffset#latest()} is equal to '$', which is the Id used with {@code XREAD} to get new data added to
	 * the stream. Note that when switching to the Consumer Group feature, we set it to
	 * {@link ReadOffset#lastConsumed()} if it is still equal to {@link ReadOffset#latest()}.
	 * @param readOffset the desired offset
	 */
	public void setReadOffset(ReadOffset readOffset) {
		this.readOffset = readOffset;
	}

	/**
	 * Configure this channel adapter to extract or not value from the {@link Record}.
	 * @param extractPayload default true
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	/**
	 * Set whether or not acknowledge message read in the Consumer Group. {@code true} by default.
	 * @param autoAck the acknowledge option.
	 */
	public void setAutoAck(boolean autoAck) {
		this.autoAck = autoAck;
	}

	/**
	 * Set the name of the Consumer Group. It is possible to create that Consumer Group if desired, see:
	 * {@link #createConsumerGroup}. If not set, the defined bean name {@link #getBeanName()} is used.
	 * @param consumerGroup the Consumer Group on which this adapter should register to listen messages.
	 */
	public void setConsumerGroup(@Nullable String consumerGroup) {
		this.consumerGroup = consumerGroup;
	}

	/**
	 * Set the name of the consumer. When a consumer name is provided, this adapter is switched to the Consumer Group
	 * feature. Note that this value should be unique in the group.
	 * @param consumerName the consumer name in the Consumer Group
	 */
	public void setConsumerName(@Nullable String consumerName) {
		this.consumerName = consumerName;
	}

	/**
	 * Create the Consumer Group if and only if it does not exist.
	 * During the creation we also create the stream, see {@code MKSTREAM}.
	 * @param createConsumerGroup specify if we should create the Consumer Group, {@code false} by default
	 */
	public void setCreateConsumerGroup(boolean createConsumerGroup) {
		this.createConsumerGroup = createConsumerGroup;
	}

	/**
	 * Set {@link ReactiveStreamOperations} used to customize the {@link StreamReceiver}.
	 * It provides a way to set the polling timeout and the serialization context.
	 * By default the polling timeout is set to infinite and
	 * {@link org.springframework.data.redis.serializer.StringRedisSerializer} is used.
	 * Mutually exclusive with 'pollTimeout', 'batchSize', 'onErrorResume', 'serializer', 'targetType', 'objectMapper'.
	 * @param streamReceiverOptions the desired receiver options
	 * */
	public void setStreamReceiverOptions(
			@Nullable StreamReceiver.StreamReceiverOptions<String, ?> streamReceiverOptions) {

		Assert.isTrue(!this.receiverBuilderOptionSet,
				"The 'streamReceiverOptions' is mutually exclusive with 'pollTimeout', 'batchSize', " +
						"'onErrorResume', 'serializer', 'targetType', 'objectMapper'");
		this.streamReceiverOptions = streamReceiverOptions;
	}

	private void assertStreamReceiverOptions(String property) {
		Assert.isNull(this.streamReceiverOptions,
				() -> "'" + property + "' cannot be set when 'StreamReceiver.StreamReceiverOptions' is provided.");
	}

	/**
	 * Configure a poll timeout for the BLOCK option during reading.
	 * Mutually exclusive with {@link #setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)}.
	 * @param pollTimeout the timeout for polling.
	 * @since 5.5
	 * @see org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptionsBuilder#pollTimeout(Duration)
	 */
	public void setPollTimeout(Duration pollTimeout) {
		assertStreamReceiverOptions("pollTimeout");
		this.streamReceiverOptionsBuilder.pollTimeout(pollTimeout);
		this.receiverBuilderOptionSet = true;
	}

	/**
	 * Configure a batch size for the COUNT option during reading.
	 * Mutually exclusive with {@link #setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)}.
	 * @param recordsPerPoll must be greater zero.
	 * @since 5.5
	 * @see org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptionsBuilder#batchSize(int)
	 */
	public void setBatchSize(int recordsPerPoll) {
		assertStreamReceiverOptions("batchSize");
		this.streamReceiverOptionsBuilder.batchSize(recordsPerPoll);
		this.receiverBuilderOptionSet = true;
	}

	/**
	 * Configure a resume Function to resume the main sequence when polling the stream fails.
	 * Mutually exclusive with {@link #setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)}.
	 * By default this function extract the failed {@link Record} and sends an
	 * {@link org.springframework.messaging.support.ErrorMessage} to the provided {@link #setErrorChannel}.
	 * The failed message for this record may have a {@link IntegrationMessageHeaderAccessor#ACKNOWLEDGMENT_CALLBACK}
	 * header when manual acknowledgment is configured for this message producer.
	 * @param resumeFunction must not be null.
	 * @since 5.5
	 * @see org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptionsBuilder#onErrorResume(Function)
	 */
	public void setOnErrorResume(Function<? super Throwable, ? extends Publisher<Void>> resumeFunction) {
		assertStreamReceiverOptions("onErrorResume");
		this.streamReceiverOptionsBuilder.onErrorResume(resumeFunction);
		this.receiverBuilderOptionSet = true;
	}

	/**
	 * Configure a key, hash key and hash value serializer.
	 * Mutually exclusive with {@link #setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)}.
	 * @param pair must not be null.
	 * @since 5.5
	 * @see StreamReceiver.StreamReceiverOptionsBuilder#serializer(RedisSerializationContext)
	 */
	public void setSerializer(RedisSerializationContext.SerializationPair<?> pair) {
		assertStreamReceiverOptions("serializer");
		this.streamReceiverOptionsBuilder.serializer(pair);
		this.receiverBuilderOptionSet = true;
	}

	/**
	 * Configure a hash target type. Changes the emitted Record type to ObjectRecord.
	 * Mutually exclusive with {@link #setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)}.
	 * @param targetType must not be null.
	 * @since 5.5
	 * @see StreamReceiver.StreamReceiverOptionsBuilder#targetType(Class)
	 */
	public void setTargetType(Class<?> targetType) {
		assertStreamReceiverOptions("targetType");
		this.streamReceiverOptionsBuilder.targetType(targetType);
		this.receiverBuilderOptionSet = true;
	}

	/**
	 * Configure a hash mapper.
	 * Mutually exclusive with {@link #setStreamReceiverOptions(StreamReceiver.StreamReceiverOptions)}.
	 * @param hashMapper must not be null.
	 * @since 5.5
	 * @see StreamReceiver.StreamReceiverOptionsBuilder#objectMapper(HashMapper)
	 */
	public void setObjectMapper(HashMapper<?, ?, ?> hashMapper) {
		assertStreamReceiverOptions("objectMapper");
		this.streamReceiverOptionsBuilder.objectMapper(hashMapper);
		this.receiverBuilderOptionSet = true;
	}

	@Override
	public String getComponentType() {
		return "redis:stream-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.streamReceiverOptions == null) {
			this.streamReceiverOptions = this.streamReceiverOptionsBuilder.build();
		}
		this.streamReceiver = StreamReceiver.create(this.reactiveConnectionFactory, this.streamReceiverOptions);
		if (StringUtils.hasText(this.consumerName) && !StringUtils.hasText(this.consumerGroup)) {
			this.consumerGroup = getBeanName();
		}
		ReactiveRedisTemplate<String, ?> reactiveRedisTemplate =
				new ReactiveRedisTemplate<>(this.reactiveConnectionFactory, RedisSerializationContext.string());
		this.reactiveStreamOperations = reactiveRedisTemplate.opsForStream();
	}

	@Override
	protected void doStart() {
		StreamOffset<String> offset = StreamOffset.create(this.streamKey, this.readOffset);

		Flux<? extends Record<String, ?>> events;

		if (!StringUtils.hasText(this.consumerName)) {
			events = this.streamReceiver.receive(offset);
		}
		else {
			Mono<?> consumerGroupMono = Mono.empty();
			if (this.createConsumerGroup) {
				consumerGroupMono =
						this.reactiveStreamOperations.createGroup(this.streamKey, this.consumerGroup) // NOSONAR
								.onErrorReturn(this.consumerGroup);
			}

			Consumer consumer = Consumer.from(this.consumerGroup, this.consumerName); // NOSONAR

			if (offset.getOffset().equals(ReadOffset.latest())) {
				// for consumer group offset id should be equal to '>'
				offset = StreamOffset.create(this.streamKey, ReadOffset.lastConsumed());
			}

			events =
					this.autoAck
							? this.streamReceiver.receiveAutoAck(consumer, offset)
							: this.streamReceiver.receive(consumer, offset);

			events = consumerGroupMono.thenMany(events);

		}

		Flux<? extends Message<?>> messageFlux =
				events.map((record) -> buildMessageFromRecord(record, this.extractPayload));
		subscribeToPublisher(messageFlux);
	}

	private Message<?> buildMessageFromRecord(Record<String, ?> record, boolean extractPayload) {
		AbstractIntegrationMessageBuilder<?> builder =
				getMessageBuilderFactory()
						.withPayload(extractPayload ? record.getValue() : record)
						.setHeader(RedisHeaders.STREAM_KEY, record.getStream())
						.setHeader(RedisHeaders.STREAM_MESSAGE_ID, record.getId())
						.setHeader(RedisHeaders.CONSUMER_GROUP, this.consumerGroup)
						.setHeader(RedisHeaders.CONSUMER, this.consumerName);

		if (!this.autoAck && this.consumerGroup != null) {
			builder.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
					(SimpleAcknowledgment) () ->
							this.reactiveStreamOperations
									.acknowledge(this.consumerGroup, record)
									.subscribe());
		}

		return builder.build();
	}

	private <T> Publisher<T> handleReceiverError(Throwable error) {
		Message<?> failedMessage = null;
		if (error instanceof ConversionFailedException) {
			@SuppressWarnings("unchecked")
			Record<String, ?> record = (Record<String, ?>) ((ConversionFailedException) error).getValue();
			if (record != null) {
				failedMessage = buildMessageFromRecord(record, false);
			}
		}
		MessagingException conversionException =
				new MessageConversionException(failedMessage, // NOSONAR
						"Cannot deserialize Redis Stream Record", error);
		if (!sendErrorMessageIfNecessary(null, conversionException)) {
			logger.getLog().error(conversionException);
		}
		return Mono.empty();
	}

}
