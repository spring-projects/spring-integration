/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.debezium.inbound;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.Builder;
import io.debezium.engine.DebeziumEngine.ChangeConsumer;
import io.debezium.engine.DebeziumEngine.RecordCommitter;
import io.debezium.engine.Header;
import io.debezium.engine.format.SerializationFormat;

import org.springframework.integration.debezium.inbound.support.DebeziumHeaders;
import org.springframework.integration.debezium.inbound.support.DefaultDebeziumHeaderMapper;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * Debezium Change Event Channel Adapter.
 *
 * @author Christian Tzolov
 * @since 6.2
 */
public class DebeziumMessageProducer extends MessageProducerSupport {

	private final DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder;

	private DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine;

	private Executor executor = Executors
			.newSingleThreadExecutor(new CustomizableThreadFactory("debezium-"));

	/**
	 * Flag to denote whether the {@link Executor} was provided via the setter and thus should not be shutdown when
	 * {@link #destroy()} is called.
	 */
	private boolean executorExplicitlySet = false;

	private CountDownLatch latch = new CountDownLatch(0);

	private String contentType = "application/json";

	private HeaderMapper<List<Header<Object>>> headerMapper = new DefaultDebeziumHeaderMapper();

	private boolean enableEmptyPayload = false;

	private boolean enableBatch = false;

	/**
	 * Create new Debezium message producer inbound channel adapter.
	 * @param debeziumBuilder - pre-configured Debezium Engine Builder instance.
	 */
	public DebeziumMessageProducer(Builder<ChangeEvent<byte[], byte[]>> debeziumBuilder) {
		Assert.notNull(debeziumBuilder, "The Debezium Engine Builder is null!");
		this.debeziumEngineBuilder = debeziumBuilder;
	}

	/**
	 * Defines if a single or a batch of messages are send downstream.
	 * @param enableBatch False - sends downstream {@link Message} for every {@link ChangeEvent data change event} read
	 * from the source database. True - sends the received {@link List} of {@link ChangeEvent}s as a raw {@link Message}
	 * payload in a single (batch) downstream message. Such payload is not serializable and would required custom
	 * serialization/deserialization implementation.
	 */
	public void setEnableBatch(boolean enableBatch) {
		this.enableBatch = enableBatch;
	}

	/**
	 * Enables support for tombstone (aka delete) messages. On a database row delete, Debezium can send a tombstone
	 * change event that has the same key as the deleted row and a value of {@link Optional.empty}. This record is a
	 * marker for downstream processors. It indicates that log compaction can remove all records that have this key.
	 * When the tombstone functionality is enabled in the Debezium connector configuration you should enable the empty
	 * payload as well.
	 * @param enableEmptyPayload True enables the empty payload handling mechanism. False by default.
	 */
	public void setEnableEmptyPayload(boolean enableEmptyPayload) {
		this.enableEmptyPayload = enableEmptyPayload;
	}

	/**
	 * Debezium Engine is designed to be submitted to an {@link Executor} or {@link ExecutorService} for execution by a
	 * single thread. By default a single-threaded Executor instance is provided configured with a
	 * {@link CustomizableThreadFactory} and a `debezium-` thread prefix.
	 * @param executor custom Executor instance used to run the Debezium Engine.
	 */
	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "Executor can not be null!");
		this.executor = executor;
		this.executorExplicitlySet = true;
	}

	/**
	 * Outbound message content type. Should be aligned with the {@link SerializationFormat} configured for the
	 * {@link DebeziumEngine}.
	 */
	public void setContentType(String contentType) {
		Assert.hasText(contentType, "Invalid content type: " + contentType);
		this.contentType = contentType;
	}

	/**
	 * Specifies how to convert Debezium change event headers into Message headers.
	 * @param headerMapper concrete HeaderMapping implementation.
	 */
	public void setHeaderMapper(HeaderMapper<List<Header<Object>>> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	/**
	 * @return Returns current header mapper.
	 */
	public HeaderMapper<List<Header<Object>>> getHeaderMapper() {
		return this.headerMapper;
	}

	@Override
	public String getComponentType() {
		return "debezium:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();

		Assert.notNull(this.executor, "Invalid Executor Service. ");
		Assert.notNull(this.headerMapper, "Header mapper can not be null!");

		if (!this.enableBatch) {
			this.debeziumEngineBuilder.notifying(new StreamChangeEventConsumer<byte[]>());
		}
		else {
			this.debeziumEngineBuilder.notifying(new BatchChangeEventConsumer<byte[]>());
		}

		this.debeziumEngine = this.debeziumEngineBuilder.build();
	}

	@Override
	protected void doStart() {
		if (this.latch.getCount() > 0) {
			return;
		}
		this.latch = new CountDownLatch(1);
		this.executor.execute(() -> {
			try {
				// Runs the debezium connector and deliver database changes to the registered consumer. This method
				// blocks until the connector is stopped.
				// If this instance is already running, then the run immediately returns.
				// When run the connector and starts polling the configured connector for change events.
				// All messages are delivered in batches to the consumer registered with this debezium engine.
				// The batch size, polling frequency, and other parameters are controlled via connector's configuration
				// settings. This continues until this connector is stopped.
				// This method can be called repeatedly as needed.
				this.debeziumEngine.run();
			}
			finally {
				this.latch.countDown();
			}
		});
	}

	@Override
	protected void doStop() {
		try {
			this.debeziumEngine.close();
		}
		catch (IOException e) {
			logger.warn(e, "Debezium failed to close!");
		}
		try {
			if (!this.latch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Failed to stop " + this);
			}
		}
		catch (InterruptedException ignored) {
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (!this.executorExplicitlySet) {
			((ExecutorService) this.executor).shutdown();
			try {
				((ExecutorService) this.executor).awaitTermination(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				throw new IllegalStateException("Debezium failed to close!", e);
			}
		}
	}

	private <T> Message<?> toMessage(ChangeEvent<T, T> changeEvent) {

		Object key = changeEvent.key();
		Object payload = changeEvent.value();
		String destination = changeEvent.destination();

		// When the tombstone event is enabled, Debezium serializes the payload to null (e.g. empty payload)
		// while the metadata information is carried through the headers (debezium_key).
		// Note: Event for none flattened responses, when the debezium.properties.tombstones.on.delete=true
		// (default), tombstones are generate by Debezium and handled by the code below.
		if (payload == null && DebeziumMessageProducer.this.enableEmptyPayload) {
			payload = Optional.empty();
		}

		// If payload is still null ignore the message.
		if (payload == null) {
			logger.info("Dropped null payload message");
			return null;
		}

		AbstractIntegrationMessageBuilder<Object> messageBuilder = getMessageBuilderFactory().withPayload(payload)
				.setHeader(DebeziumHeaders.KEY, key)
				.setHeader(DebeziumHeaders.DESTINATION, destination)
				.setHeader(MessageHeaders.CONTENT_TYPE, this.contentType)
				// Use the provided header mapper to convert Debezium headers into message headers.
				.copyHeaders(this.headerMapper.toHeaders(changeEvent.headers()));

		return messageBuilder.build();
	}

	final class StreamChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		@Override
		public void accept(ChangeEvent<T, T> changeEvent) {
			DebeziumMessageProducer.this.sendMessage(toMessage(changeEvent));
		}

	}

	final class BatchChangeEventConsumer<T> implements ChangeConsumer<ChangeEvent<T, T>> {

		@Override
		public void handleBatch(List<ChangeEvent<T, T>> changeEvents, RecordCommitter<ChangeEvent<T, T>> committer)
				throws InterruptedException {

			AbstractIntegrationMessageBuilder<Object> messageBuilder = getMessageBuilderFactory()
					.withPayload(changeEvents);

			for (ChangeEvent<T, T> event : changeEvents) {
				committer.markProcessed(event);
			}

			DebeziumMessageProducer.this.sendMessage(messageBuilder.build());
			committer.markBatchFinished();
		}

	}
}
