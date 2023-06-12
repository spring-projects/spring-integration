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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.Builder;
import io.debezium.engine.DebeziumEngine.ChangeConsumer;
import io.debezium.engine.DebeziumEngine.RecordCommitter;
import io.debezium.engine.Header;
import io.debezium.engine.format.SerializationFormat;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.debezium.support.DebeziumHeaders;
import org.springframework.integration.debezium.support.DefaultDebeziumHeaderMapper;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.util.Assert;

/**
 * Debezium Change Event Channel Adapter.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class DebeziumMessageProducer extends MessageProducerSupport {

	private final DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder;

	private DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine;

	/**
	 * Debezium Engine is designed to be submitted to an {@link Executor}.
	 */
	private TaskExecutor taskExecutor;

	private String contentType = "application/json";

	private HeaderMapper<List<Header<Object>>> headerMapper = new DefaultDebeziumHeaderMapper();

	private boolean enableEmptyPayload = false;

	private boolean enableBatch = false;

	private volatile CountDownLatch lifecycleLatch = new CountDownLatch(0);

	/**
	 * Create new Debezium message producer inbound channel adapter.
	 * @param debeziumBuilder - pre-configured Debezium Engine Builder instance.
	 */
	public DebeziumMessageProducer(Builder<ChangeEvent<byte[], byte[]>> debeziumBuilder) {
		Assert.notNull(debeziumBuilder, "'debeziumBuilder' must not be null");
		this.debeziumEngineBuilder = debeziumBuilder;
	}

	/**
	 * Enable the {@link ChangeEvent} batch mode handling.
	 * When enabled the channel adapter will send a {@link List}
	 * of {@link ChangeEvent}s as a payload in a single downstream {@link Message}.
	 * Such a batch payload is not serializable.
	 * By default, the batch mode is disabled, e.g. every input {@link ChangeEvent} is converted into a
	 * single downstream {@link Message}.
	 * @param enable set to true to enable the batch mode. Disabled by default.
	 */
	public void setEnableBatch(boolean enable) {
		this.enableBatch = enable;
	}

	/**
	 * Enable support for tombstone (aka delete) messages.
	 * On a database row delete, Debezium can send a tombstone change event
	 * that has the same key as the deleted row and a value of {@link Optional#empty()}.
	 * This record is a marker for downstream processors.
	 * It indicates that log compaction can remove all records that have this key.
	 * When the tombstone functionality is enabled in the Debezium connector
	 * configuration you should enable the empty payload as well.
	 * @param enabled set true to enable the empty payload. Disabled by default.
	 */
	public void setEnableEmptyPayload(boolean enabled) {
		this.enableEmptyPayload = enabled;
	}

	/**
	 * Set a {@link TaskExecutor} for the Debezium engine task.
	 * @param taskExecutor the {@link TaskExecutor} to use.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "'taskExecutor' must not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the outbound message content type.
	 * Must be aligned with the {@link SerializationFormat} configuration used by
	 * the provided {@link DebeziumEngine}.
	 */
	public void setContentType(String contentType) {
		Assert.hasText(contentType, "'contentType' must not be empty");
		this.contentType = contentType;
	}

	/**
	 * Set a {@link HeaderMapper} to convert the {@link ChangeEvent}
	 * headers into {@link Message} headers.
	 * @param headerMapper {@link HeaderMapper} implementation to use.
	 * Defaults to {@link DefaultDebeziumHeaderMapper}.
	 */
	public void setHeaderMapper(HeaderMapper<List<Header<Object>>> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	public String getComponentType() {
		return "debezium:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();

		if (this.taskExecutor == null) {
			this.taskExecutor = new SimpleAsyncTaskExecutor(getComponentName() + "-thread-");
		}

		if (!this.enableBatch) {
			this.debeziumEngineBuilder.notifying(new StreamChangeEventConsumer<>());
		}
		else {
			this.debeziumEngineBuilder.notifying(new BatchChangeEventConsumer<>());
		}

		this.debeziumEngine = this.debeziumEngineBuilder.build();
	}

	@Override
	protected void doStart() {
		if (this.lifecycleLatch.getCount() > 0) {
			return;
		}
		this.lifecycleLatch = new CountDownLatch(1);
		this.taskExecutor.execute(() -> {
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
				this.lifecycleLatch.countDown();
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
			if (!this.lifecycleLatch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Failed to stop " + this);
			}
		}
		catch (InterruptedException ignored) {
		}
	}

	@Nullable
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
			logger.info(() -> "Dropped null payload message for Change Event key: " + key);
			return null;
		}

		return getMessageBuilderFactory()
				.withPayload(payload)
				.setHeader(DebeziumHeaders.KEY, key)
				.setHeader(DebeziumHeaders.DESTINATION, destination)
				.setHeader(MessageHeaders.CONTENT_TYPE, this.contentType)
				// Use the provided header mapper to convert Debezium headers into message headers.
				.copyHeaders(this.headerMapper.toHeaders(changeEvent.headers()))
				.build();
	}

	final class StreamChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		@Override
		public void accept(ChangeEvent<T, T> changeEvent) {
			Message<?> message = toMessage(changeEvent);
			if (message != null) {
				sendMessage(message);
			}
		}

	}

	final class BatchChangeEventConsumer<T> implements ChangeConsumer<ChangeEvent<T, T>> {

		@Override
		public void handleBatch(List<ChangeEvent<T, T>> changeEvents, RecordCommitter<ChangeEvent<T, T>> committer)
				throws InterruptedException {

			Message<List<ChangeEvent<T, T>>> message =
					getMessageBuilderFactory()
							.withPayload(changeEvents)
							.build();

			sendMessage(message);

			for (ChangeEvent<T, T> event : changeEvents) {
				committer.markProcessed(event);
			}
			committer.markBatchFinished();
		}

	}

}
