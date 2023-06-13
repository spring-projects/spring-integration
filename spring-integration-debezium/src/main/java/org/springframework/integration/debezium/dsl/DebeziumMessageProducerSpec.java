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

import java.util.List;
import java.util.Optional;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.Header;
import io.debezium.engine.format.SerializationFormat;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.debezium.inbound.DebeziumMessageProducer;
import org.springframework.integration.debezium.support.DefaultDebeziumHeaderMapper;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.messaging.support.HeaderMapper;

/**
 * A {@link org.springframework.integration.dsl.MessageProducerSpec} for {@link DebeziumMessageProducer}.
 *
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class DebeziumMessageProducerSpec
		extends MessageProducerSpec<DebeziumMessageProducerSpec, DebeziumMessageProducer> {

	protected DebeziumMessageProducerSpec(DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {
		super(new DebeziumMessageProducer(debeziumEngineBuilder));
	}

	/**
	 * Enable the {@link ChangeEvent} batch mode handling. When enabled the channel adapter will send a {@link List} of
	 * {@link ChangeEvent}s as a payload in a single downstream {@link org.springframework.messaging.Message}.
	 * Such a batch payload is not serializable.
	 * By default, the batch mode is disabled, e.g. every input {@link ChangeEvent} is converted into a
	 * single downstream {@link org.springframework.messaging.Message}.
	 * @param enable set to true to enable the batch mode. Disabled by default.
	 * @return the spec.
	 */
	public DebeziumMessageProducerSpec enableBatch(boolean enable) {
		this.target.setEnableBatch(enable);
		return this;
	}

	/**
	 * Enable support for tombstone (aka delete) messages. On a database row delete, Debezium can send a tombstone
	 * change event that has the same key as the deleted row and a value of {@link Optional#empty()}. This record is a
	 * marker for downstream processors. It indicates that log compaction can remove all records that have this key.
	 * When the tombstone functionality is enabled in the Debezium connector configuration you should enable the empty
	 * payload as well.
	 * @param enabled set true to enable the empty payload. Disabled by default.
	 * @return the spec.
	 */
	public DebeziumMessageProducerSpec enableEmptyPayload(boolean enabled) {
		this.target.setEnableEmptyPayload(enabled);
		return this;
	}

	/**
	 * Set a {@link TaskExecutor} for the Debezium engine.
	 * @param taskExecutor the {@link TaskExecutor} to use.
	 * @return the spec.
	 */
	public DebeziumMessageProducerSpec taskExecutor(TaskExecutor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

	/**
	 * Set the outbound message content type.
	 * Must be aligned with the {@link SerializationFormat} configuration used by the provided {@link DebeziumEngine}.
	 * @param contentType payload content type.
	 * @return the spec.
	 */
	public DebeziumMessageProducerSpec contentType(String contentType) {
		this.target.setContentType(contentType);
		return this;
	}

	/**
	 * Comma-separated list of names of {@link ChangeEvent} headers to be mapped into outbound Message headers.
	 * Debezium's NewRecordStateExtraction 'add.headers' property configures the metadata to be used as
	 * {@link ChangeEvent} headers.
	 * <p>
	 * You should prefix the names passed to the 'headerNames' with the prefix configured by the Debezium
	 * 'add.headers.prefix' property. Later defaults to '__'. For example for 'add.headers=op,name' and
	 * 'add.headers.prefix=__' you should use header hames like: '__op', '__name'.
	 * @param headerNames The values in this list can be a simple patterns to be matched against the header names.
	 * @return the spec.
	 */
	public DebeziumMessageProducerSpec headerNames(String... headerNames) {
		DefaultDebeziumHeaderMapper headerMapper = new DefaultDebeziumHeaderMapper();
		headerMapper.setHeaderNamesToMap(headerNames);

		return headerMapper(headerMapper);
	}

	/**
	 * Set a {@link HeaderMapper} to convert the {@link ChangeEvent} headers
	 * into {@link org.springframework.messaging.Message} headers.
	 * @param headerMapper {@link HeaderMapper} implementation to use. Defaults to {@link DefaultDebeziumHeaderMapper}.
	 * @return the spec.
	 */
	public DebeziumMessageProducerSpec headerMapper(HeaderMapper<List<Header<Object>>> headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

}
