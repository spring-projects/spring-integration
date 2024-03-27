/*
 * Copyright 2023-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.Header;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.debezium.DebeziumMySqlTestContainer;
import org.springframework.integration.debezium.support.DebeziumHeaders;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class DebeziumDslTests implements DebeziumMySqlTestContainer {

	static final LogAccessor logger = new LogAccessor(DebeziumDslTests.class);

	@Autowired
	private Config config;

	@Test
	void dslFromBuilder() throws InterruptedException {
		assertThat(config.latch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(config.payloads).hasSize(EXPECTED_DB_TX_COUNT);
		assertThat(config.headerKeys).hasSize(EXPECTED_DB_TX_COUNT);

		config.headerKeys.forEach(keys -> {
			assertThat(keys).contains("debezium_destination", "id", "contentType", "debezium_key", "timestamp");
			if (keys.size() > 5) {
				assertThat(keys).contains("__name", "__db", "__table");
			}
		});
	}

	@Test
	void dslBatch() throws InterruptedException {
		assertThat(config.batchLatch.await(30, TimeUnit.SECONDS)).isTrue();
		assertThat(config.bachPayloads)
				.as("Sum of the message payload counts should correspond to the number of DB transactions")
				.hasSize(EXPECTED_DB_TX_COUNT);
		assertThat(config.batchHeaderKeys).hasSize(EXPECTED_DB_TX_COUNT);
		assertThat(config.batchMessageCount)
				.as("Batch mode: message count should be less than the sum of the payloads counts")
				.isLessThan(EXPECTED_DB_TX_COUNT);

		config.batchHeaderKeys.stream()
				.filter(headerNames -> !CollectionUtils.isEmpty(headerNames))
				.forEach(headerNames -> {
					assertThat(headerNames).contains("__name", "__db", "__table");
				});
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(52);

		private final List<String> payloads = new ArrayList<>();

		private final List<Set<String>> headerKeys = new ArrayList<>();

		private final CountDownLatch batchLatch = new CountDownLatch(52);

		private final List<String> bachPayloads = new ArrayList<>();

		private final List<List<String>> batchHeaderKeys = new ArrayList<>();

		private int batchMessageCount = 0;

		@Bean
		public IntegrationFlow streamFlowFromBuilder(DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> builder) {

			DebeziumMessageProducerSpec dsl = Debezium.inboundChannelAdapter(builder)
					.headerNames("*")
					.contentType("application/json")
					.enableBatch(false)
					.enableEmptyPayload(true);

			return IntegrationFlow.from(dsl)
					.handle(m -> {
						Object key = new String((byte[]) m.getHeaders().get(DebeziumHeaders.KEY));
						Object destination = m.getHeaders().get(DebeziumHeaders.DESTINATION);
						logger.info("KEY: " + key + ", DESTINATION: " + destination);

						headerKeys.add(m.getHeaders().keySet());
						payloads.add(new String((byte[]) m.getPayload()));
						latch.countDown();
					})
					.get();
		}

		@Bean
		@SuppressWarnings("unchecked")
		public IntegrationFlow batchFlowDirect() {

			DebeziumMessageProducerSpec dsl = Debezium
					.inboundChannelAdapter(
							DebeziumMySqlTestContainer.connectorConfig(DebeziumMySqlTestContainer.mysqlPort()))
					.headerNames("*")
					.contentType("application/json")
					.enableBatch(true);

			return IntegrationFlow.from(dsl)
					.handle(m -> {
						batchMessageCount++;
						List<ChangeEvent<byte[], byte[]>> batch = (List<ChangeEvent<byte[], byte[]>>) m.getPayload();
						batch.forEach(ce -> {
							bachPayloads.add(new String(ce.value()));
							batchHeaderKeys
									.add(ce.headers().stream().map(Header::getKey).collect(Collectors.toList()));
							batchLatch.countDown();
						});
					}).get();
		}

		@Bean
		public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {
			return DebeziumEngine.create(KeyValueHeaderChangeEventFormat
							.of(io.debezium.engine.format.JsonByteArray.class,
									io.debezium.engine.format.JsonByteArray.class,
									io.debezium.engine.format.JsonByteArray.class))
					.using(DebeziumMySqlTestContainer.connectorConfig(DebeziumMySqlTestContainer.mysqlPort()));
		}

	}

}
