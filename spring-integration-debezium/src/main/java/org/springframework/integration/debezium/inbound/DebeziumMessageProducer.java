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
import java.lang.reflect.Field;
import java.util.List;
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
import io.debezium.engine.Header;
import io.debezium.engine.format.SerializationFormat;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;

/**
 * Debezium Change Event Channel Adapter.
 *
 * @author Christian Tzolov
 * @since 6.2
 */
public class DebeziumMessageProducer extends MessageProducerSupport implements BeanClassLoaderAware {
	/**
	 * ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL.
	 */
	public static final String ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL = "org.springframework.kafka.support.KafkaNull";

	private Object kafkaNull = null;

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
	private HeaderMapper<List<Header<byte[]>>> headerMapper = new DefaultDebeziumHeaderMapper<byte[]>();

	public DebeziumMessageProducer(Builder<ChangeEvent<byte[], byte[]>> debeziumBuilder) {
		this.debeziumEngineBuilder = debeziumBuilder;
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

	public void setHeaderMapper(HeaderMapper<List<Header<byte[]>>> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		try {
			Class<?> clazz = ClassUtils.forName(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL, classLoader);
			Field field = clazz.getDeclaredField("INSTANCE");
			this.kafkaNull = field.get(null);
		}
		catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
		}
	}

	@Override
	public String getComponentType() {
		return "debezium:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {

		super.onInit();

		Assert.notNull(this.debeziumEngineBuilder, "Failed to resolve Debezium Engine Builder. " +
				"Debezium Engine Builder must either be set explicitly via constructor argument.");
		Assert.notNull(this.executorService, "Invalid Executor Service. ");
		Assert.notNull(this.headerMapper, "Header mapper can not be null!");

		this.debeziumEngine = this.debeziumEngineBuilder
				.notifying(new ChangeEventConsumer<byte[]>(this.headerMapper, this.contentType))
				.build();
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

	void nextMessage(Message<?> message) {
		this.sendMessage(message);
	}

	private final class ChangeEventConsumer<T> implements Consumer<ChangeEvent<T, T>> {

		/**
		 * Outbound message content type. Should be aligned with the {@link SerializationFormat} configured for the
		 * {@link DebeziumEngine}.
		 */
		private final String contentType;

		/**
		 * Specifies how to convert Debezium change event headers into Message headers.
		 */
		private HeaderMapper<List<Header<T>>> headerMapper;

		ChangeEventConsumer(HeaderMapper<List<Header<T>>> headerMapper, String contentType) {
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
			if (payload == null) {
				payload = DebeziumMessageProducer.this.kafkaNull;
			}

			// If payload is still null ignore the message.
			if (payload == null) {
				logger.info("Dropped null payload message");
				return;
			}

			MessageBuilder<?> messageBuilder = MessageBuilder
					.withPayload(payload)
					.setHeader("debezium_key", key)
					.setHeader("debezium_destination", destination)
					.setHeader(MessageHeaders.CONTENT_TYPE,
							(payload.equals(DebeziumMessageProducer.this.kafkaNull))
									? MimeTypeUtils.TEXT_PLAIN_VALUE
									: this.contentType);

			// Use the provided header mapper to convert Debezium headers into message headers.
			messageBuilder.copyHeaders(this.headerMapper.toHeaders(changeEvent.headers()));

			DebeziumMessageProducer.this.sendMessage(messageBuilder.build());
		}
	}
}
