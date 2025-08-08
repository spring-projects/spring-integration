/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.debezium.dsl;

import java.util.Properties;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.JsonByteArray;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import io.debezium.engine.format.SerializationFormat;

import org.springframework.util.Assert;

/**
 * Factory class for Debezium DSL components.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
public final class Debezium {

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} for the provided native debezium {@link Properties} and
	 * JSON serialization formats.
	 * @param debeziumConfig {@link Properties} with required debezium engine and connector properties.
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(Properties debeziumConfig) {
		return inboundChannelAdapter(debeziumConfig, JsonByteArray.class, JsonByteArray.class);
	}

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} for the provided native debezium {@link Properties} and
	 * serialization formats.
	 * @param debeziumConfig {@link Properties} with required debezium engine and connector properties.
	 * @param messageFormat  {@link SerializationFormat} format for the {@link ChangeEvent} key and payload.
	 * @param headerFormat   {@link SerializationFormat} format for the {@link ChangeEvent} headers.
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(Properties debeziumConfig,
			Class<? extends SerializationFormat<byte[]>> messageFormat,
			Class<? extends SerializationFormat<byte[]>> headerFormat) {

		return inboundChannelAdapter(builder(debeziumConfig, messageFormat, headerFormat));
	}

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} for the provided {@link DebeziumEngine.Builder}.
	 * @param debeziumEngineBuilder the {@link DebeziumEngine.Builder} to use.
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(
			DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {

		return new DebeziumMessageProducerSpec(debeziumEngineBuilder);
	}

	private static DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> builder(Properties debeziumConfig,
			Class<? extends SerializationFormat<byte[]>> messageFormat,
			Class<? extends SerializationFormat<byte[]>> headerFormat) {

		Assert.notNull(messageFormat, "'messageFormat' must not be null");
		Assert.notNull(headerFormat, "'headerFormat' must not be null");
		Assert.notNull(debeziumConfig, "'debeziumConfig' must not be null");
		Assert.isTrue(debeziumConfig.containsKey("connector.class"), "The 'connector.class' property must be set");

		return DebeziumEngine
				.create(KeyValueHeaderChangeEventFormat.of(messageFormat, messageFormat, headerFormat))
				.using(debeziumConfig);
	}

	private Debezium() {
	}

}
