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
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.Builder;
import io.debezium.engine.DebeziumEngine.ChangeConsumer;
import io.debezium.engine.DebeziumEngine.RecordCommitter;
import io.debezium.engine.Header;
import io.debezium.engine.format.SerializationFormat;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Debezium Change Event Channel Adapter.
 *
 * @author Christian Tzolov
 * @since 6.2
 */
public class DebeziumMessageProducer extends MessageProducerSupport {

	private DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder;

	private DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngine;

	/**
	 * Executor service for running engine daemon.
	 */
	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	/**
	 * Flag to denote whether the {@link ExecutorService} was provided via the setter and thus should not be shutdown
	 * when {@link #destroy()} is called.
	 */
	private boolean executorServiceExplicitlySet;

	private CountDownLatch latch = new CountDownLatch(0);

	private Future<?> future = CompletableFuture.completedFuture(null);

	/**
	 * Outbound message content type. Should be aligned with the {@link SerializationFormat} configured for the
	 * {@link DebeziumEngine}.
	 */
	private String contentType = "application/json";

	/**
	 * Specifies how to convert Debezium change event headers into Message headers.
	 */
	private HeaderMapper<List<Header<Object>>> headerMapper = new DefaultDebeziumHeaderMapper();

	/**
	 * Enable support for tombstone (aka delete) messages.
	 */
	private boolean enableEmptyPayload = false;

	/**
	 * Alow sending batch of Change Event messages down stream.
	 */
	private boolean enableBatch = false;

	public DebeziumMessageProducer(Builder<ChangeEvent<byte[], byte[]>> debeziumBuilder) {
		Assert.notNull(debeziumBuilder, "Failed to resolve Debezium Engine Builder. " +
				"Debezium Engine Builder must either be set explicitly via constructor argument.");
		this.debeziumEngineBuilder = debeziumBuilder;
	}

	public void setEnableBatch(boolean batch) {
		this.enableBatch = batch;
	}

	public void setEnableEmptyPayload(boolean enableEmptyPayload) {
		this.enableEmptyPayload = enableEmptyPayload;
	}

	/**
	 * Set the {@link ExecutorService}, where is not provided then a default of single thread Executor will be used.
	 * @param executorService the executor service.
	 */
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
		this.executorServiceExplicitlySet = true;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

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

		Assert.notNull(this.executorService, "Invalid Executor Service. ");
		Assert.notNull(this.headerMapper, "Header mapper can not be null!");

		if (this.enableBatch) {
			this.debeziumEngineBuilder.notifying(new BatchChangeEventConsumer<byte[]>());
		}
		else {
			this.debeziumEngineBuilder.notifying(new ChangeEventConsumer<byte[]>(this.headerMapper, this.contentType));
		}

		this.debeziumEngine = this.debeziumEngineBuilder.build();
	}

	@Override
	protected void doStart() {
		if (this.latch.getCount() > 0) {
			return;
		}
		this.latch = new CountDownLatch(1);
		this.future = this.executorService.submit(() -> {
			try {
				this.debeziumEngine.run();
			}
			finally {
				this.latch.countDown();
			}
		});
	}

	@Override
	protected void doStop() {
		if (this.future.isDone()) {
			return;
		}
		this.future.cancel(true);
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
		if (!this.executorServiceExplicitlySet) {
			this.executorService.shutdown();
		}
		if (this.debeziumEngine != null) {
			try {
				this.debeziumEngine.close();
			}
			catch (IOException e) {
				throw new UncheckedIOException("Debezium failed to close!", e);
			}
		}
	}

	final class ChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		/**
		 * Outbound message content type. Should be aligned with the {@link SerializationFormat} configured for the
		 * {@link DebeziumEngine}.
		 */
		private final String contentType;

		/**
		 * Specifies how to convert Debezium change event headers into Message headers.
		 */
		private HeaderMapper<List<Header<Object>>> headerMapper;

		ChangeEventConsumer(HeaderMapper<List<Header<Object>>> headerMapper, String contentType) {
			this.headerMapper = headerMapper;
			this.contentType = contentType;
		}

		@Override
		public void accept(ChangeEvent<T, T> changeEvent) {
			if (logger.isDebugEnabled()) {
				logger.debug("[Debezium Event]: " + changeEvent.key());
			}

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
				return;
			}

			MessageBuilder<?> messageBuilder = MessageBuilder
					.withPayload(payload)
					.setHeader(DebeziumHeaders.KEY, key)
					.setHeader(DebeziumHeaders.DESTINATION, destination)
					.setHeader(MessageHeaders.CONTENT_TYPE, this.contentType);

			// Use the provided header mapper to convert Debezium headers into message headers.
			messageBuilder.copyHeaders(this.headerMapper.toHeaders(changeEvent.headers()));

			sendMessage(messageBuilder.build());
		}
	}

	final class BatchChangeEventConsumer<T> implements ChangeConsumer<ChangeEvent<T, T>> {

		@Override
		public void handleBatch(List<ChangeEvent<T, T>> records, RecordCommitter<ChangeEvent<T, T>> committer)
				throws InterruptedException {
			throw new UnsupportedOperationException("Unimplemented method 'handleBatch'");
		}

	}
}
