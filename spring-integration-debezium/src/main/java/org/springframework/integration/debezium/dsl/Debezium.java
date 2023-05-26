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

package org.springframework.integration.debezium.dsl;

import java.time.Clock;
import java.util.Properties;

import javax.annotation.Nullable;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.Builder;
import io.debezium.engine.DebeziumEngine.CompletionCallback;
import io.debezium.engine.DebeziumEngine.ConnectorCallback;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import io.debezium.engine.format.SerializationFormat;
import io.debezium.engine.spi.OffsetCommitPolicy;

import org.springframework.util.Assert;

/**
 * Factory class for Debezium components DSL.
 *
 * @author Christian Tzolov
 *
 * @since 6.2
 */
public final class Debezium {

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} for the provided {@link DebeziumEngine.Builder}.
	 * @param debeziumEngineBuilder the {@link DebeziumEngine.Builder} to use.
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(
			Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {
		return new DebeziumMessageProducerSpec(debeziumEngineBuilder);
	}

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} for the provided native debezium {@link Properties} and
	 * JSON serialization formats.
	 * @param debeziumConfig {@link Properties} with required debezium engine and connector properties.
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(Properties debeziumConfig) {

		return new DebeziumMessageProducerSpec(builder(
				debeziumConfig, io.debezium.engine.format.JsonByteArray.class,
				io.debezium.engine.format.JsonByteArray.class, null, null,
				null, null));
	}

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} for the provided native debezium {@link Properties} and
	 * serialization formats.
	 * @param debeziumConfig {@link Properties} with required debezium engine and connector properties.
	 * @param messageFormat {@link SerializationFormat} format for the {@link ChangeEvent} key and payload.
	 * @param headerFormat {@link SerializationFormat} format for the {@link ChangeEvent} headers.
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(Properties debeziumConfig,
			Class<? extends SerializationFormat<byte[]>> messageFormat,
			Class<? extends SerializationFormat<byte[]>> headerFormat) {

		return new DebeziumMessageProducerSpec(builder(
				debeziumConfig, messageFormat, headerFormat, null, null, null, null));
	}

	/**
	 * Create an instance of {@link DebeziumMessageProducerSpec} from provided debezium configuration.
	 * @param debeziumConfig {@link Properties} with required debezium engine and connector properties.
	 * @param messageFormat {@link SerializationFormat} format for the {@link ChangeEvent} key and payload.
	 * @param headerFormat {@link SerializationFormat} format for the {@link ChangeEvent} headers.
	 * @param offsetCommitPolicy Set the Debezium commit policy. The default is a periodic commit policy based on time *
	 * intervals.
	 * @param completionCallback Set the completion callback that logs the completion status.
	 * @param connectorCallback Set a callback to provide feedback about the different stages according to the
	 * completion state of each component running within the engine.
	 * @param debeziumClock Set a specific clock when needing to determine the current time. Defaults to
	 * {@link Clock#systemDefaultZone() system clock}
	 * @return the spec.
	 */
	public static DebeziumMessageProducerSpec inboundChannelAdapter(Properties debeziumConfig,
			Class<? extends SerializationFormat<byte[]>> messageFormat,
			Class<? extends SerializationFormat<byte[]>> headerFormat,
			@Nullable OffsetCommitPolicy offsetCommitPolicy, @Nullable CompletionCallback completionCallback,
			@Nullable ConnectorCallback connectorCallback, @Nullable Clock debeziumClock) {

		return new DebeziumMessageProducerSpec(builder(
				debeziumConfig, messageFormat, headerFormat, offsetCommitPolicy, completionCallback, connectorCallback,
				debeziumClock));
	}

	private static Builder<ChangeEvent<byte[], byte[]>> builder(Properties debeziumConfig,
			Class<? extends SerializationFormat<byte[]>> messageFormat,
			Class<? extends SerializationFormat<byte[]>> headerFormat,
			OffsetCommitPolicy offsetCommitPolicy, CompletionCallback completionCallback,
			ConnectorCallback connectorCallback, Clock debeziumClock) {

		Assert.notNull(messageFormat, "Null message format!");
		Assert.notNull(headerFormat, "Null header format!");
		Assert.notNull(debeziumConfig, "Null properties!");
		Assert.isTrue(debeziumConfig.contains("connector.class"), "The connector.class property must be set!");

		Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder = DebeziumEngine
				.create(KeyValueHeaderChangeEventFormat
						.of(messageFormat, messageFormat, headerFormat))
				.using(debeziumConfig);

		if (offsetCommitPolicy != null) {
			debeziumEngineBuilder.using(offsetCommitPolicy);
		}
		if (completionCallback != null) {
			debeziumEngineBuilder.using(completionCallback);
		}
		if (connectorCallback != null) {
			debeziumEngineBuilder.using(connectorCallback);
		}
		if (debeziumClock != null) {
			debeziumEngineBuilder.using(debeziumClock);
		}

		return debeziumEngineBuilder;
	}

	private Debezium() {
	}

}
