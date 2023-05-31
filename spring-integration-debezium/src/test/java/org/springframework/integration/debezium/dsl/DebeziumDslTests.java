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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.debezium.engine.ChangeEvent;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.debezium.DebeziumMySqlTestContainer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Christian Tzolov
 *
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class DebeziumDslTests implements DebeziumMySqlTestContainer {

	@Autowired
	private Config config;

	@Test
	void dslBatch() throws InterruptedException {
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(config.payloads).hasSize(52);
		assertThat(config.headerKeys).hasSize(52);

		config.headerKeys.stream()
				.filter(headerNames -> !CollectionUtils.isEmpty(headerNames))
				.forEach(headerNames -> {
					assertThat(headerNames).contains("__name", "__db", "__table", "__op");
				});
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(52);

		private final List<String> payloads = new ArrayList<>();

		private final List<List<String>> headerKeys = new ArrayList<>();

		@Bean
		public IntegrationFlow listener() {

			DebeziumMessageProducerSpec dsl = Debezium
					.inboundChannelAdapter(
						DebeziumMySqlTestContainer.connectorConfig(DebeziumMySqlTestContainer.mysqlPort()))
					.headerNames("*")
					.contentType("application/json")
					.enableBatch(true);

			return IntegrationFlow.from(dsl)
					.handle(m -> {
						List<ChangeEvent<byte[], byte[]>> batch = (List<ChangeEvent<byte[], byte[]>>) m.getPayload();
						batch.stream().forEach(ce -> {
							payloads.add(new String(ce.value()));
							headerKeys.add(ce.headers().stream().map(h -> h.getKey()).collect(Collectors.toList()));
							this.latch.countDown();
						});
					}).get();
		}
	}

}
