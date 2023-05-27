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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.debezium.DebeziumMySqlTestContainer;
import org.springframework.integration.debezium.DebeziumTestUtils;
import org.springframework.integration.debezium.support.DebeziumHeaders;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 *
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class DebeziumDslWithBuilderTests implements DebeziumMySqlTestContainer {

	static final LogAccessor logger = new LogAccessor(DebeziumDslWithBuilderTests.class);

	@Autowired
	private Config config;

	@Test
	void dslFromBuilder() throws InterruptedException {
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(config.payloads).hasSize(52);
		assertThat(config.headerKeys).hasSize(52);

		config.headerKeys.stream().forEach(keys -> {
			assertThat(keys).contains("debezium_destination", "id", "contentType", "debezium_key", "timestamp");
			if (keys.size() > 5) {
				assertThat(keys).contains("__name", "__db", "__table", "__op");
			}
		});
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(52);

		private final List<String> payloads = new ArrayList<>();

		private final List<Set<String>> headerKeys = new ArrayList<>();

		@Bean
		public IntegrationFlow listener(DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {

			DebeziumMessageProducerSpec dsl = Debezium.inboundChannelAdapter(debeziumEngineBuilder)
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
						this.latch.countDown();
					})
					.get();
		}

		@Bean
		public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {
			return DebeziumEngine.create(KeyValueHeaderChangeEventFormat
					.of(io.debezium.engine.format.JsonByteArray.class,
							io.debezium.engine.format.JsonByteArray.class,
							io.debezium.engine.format.JsonByteArray.class))
					.using(DebeziumTestUtils.connectorConfig(DebeziumMySqlTestContainer.mysqlPort()));
		}
	}

}
