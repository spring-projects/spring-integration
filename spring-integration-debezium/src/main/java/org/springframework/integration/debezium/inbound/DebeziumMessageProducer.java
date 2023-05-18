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
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
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
	private boolean executorServiceExplicitlySet = false;

	private CountDownLatch latch = new CountDownLatch(0);

	private Future<?> future = CompletableFuture.completedFuture(null);

	private String contentType = "application/json";

	private HeaderMapper<List<Header<Object>>> headerMapper = new DefaultDebeziumHeaderMapper();

	private boolean enableEmptyPayload = false;

	private boolean enableBatch = false;

	public DebeziumMessageProducer(Builder<ChangeEvent<byte[], byte[]>> debeziumBuilder) {
		Assert.notNull(debeziumBuilder, "Failed to resolve Debezium Engine Builder. " +
				"Debezium Engine Builder must either be set explicitly via constructor argument.");
		this.debeziumEngineBuilder = debeziumBuilder;
	}

	/**
	 * Comma-separated list of names of Debezium's Change Event headers to be mapped to the outbound Message headers.
	 *
	 * The Debezium' New Record State Extraction 'add.headers' property is used to configure the metadata to be set in
	 * the produced ChangeEvent headers.
	 *
	 * Note that you must prefix the 'headerNames' used the 'setAllowedHeaderNames' with the prefix configured by the
	 * 'add.headers.prefix' debezium property. Later defaults to '__'. For example for 'add.headers=op,name' and
	 * 'add.headers.prefix=__' you should use headerNames == "__op", "__name".
	 *
	 * @param headerNames The values in this list can be a simple patterns to be matched against the header names (e.g.
	 * "foo*" or "*foo").
	 *
	 * @see <a href=
	 * "https://debezium.io/documentation/reference/2.2/transformations/event-flattening.html#extract-new-record-state-add-headers">add.headers</a>
	 * @see <a href=
	 * "https://debezium.io/documentation/reference/2.2/transformations/event-flattening.html#extract-new-record-state-add-headers-prefix">add.headers.prefix</a>
	 *
	 */
	public void setAllowedHeaderNames(String... headerNames) {
		Assert.isInstanceOf(DefaultDebeziumHeaderMapper.class, this.headerMapper,
				"Only applicable with 'DefaultDebeziumHeaderMapper' header mappers!");
		((DefaultDebeziumHeaderMapper) this.headerMapper).setAllowedHeaderNames(headerNames);
	}

	/**
	 * Defines if a single or a batch of messages are send downstream.
	 *
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
	 *
	 * When the tombstone functionality is enabled in the Debezium connector configuration you should enable the empty
	 * payload as well.
	 */
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

	/**
	 * Outbound message content type. Should be aligned with the {@link SerializationFormat} configured for the
	 * {@link DebeziumEngine}.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Specifies how to convert Debezium change event headers into Message headers.
	 *
	 * @param headerMapper concrete HeaderMapping implementation.
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

		Assert.notNull(this.executorService, "Invalid Executor Service. ");
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

	private <T> Message<?> toMessage(ChangeEvent<T, T> changeEvent) {

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
			sendMessage(toMessage(changeEvent));
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

			sendMessage(messageBuilder.build());
			committer.markBatchFinished();
		}
	}
}
