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

package org.springframework.integration.debezium.it;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.Header;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.debezium.DebeziumMySqlTestContainer;
import org.springframework.integration.debezium.inbound.DebeziumMessageProducer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class DebeziumBatchTests implements DebeziumMySqlTestContainer {

	private final List<ChangeEvent<Object, Object>> allPayload = new ArrayList<>();

	@Autowired
	@Qualifier("queueChannel")
	private QueueChannel queueChannel;

	private int batchCount = 0;

	@Test
	void batchMode() {
		await().atMost(Duration.ofMinutes(1)).until(this::receivePayloads, (count) -> count == EXPECTED_DB_TX_COUNT);

		assertThat(allPayload).hasSize(EXPECTED_DB_TX_COUNT);
		assertThat(batchCount).isLessThan(EXPECTED_DB_TX_COUNT);

		for (int i = 0; i < 52; i++) {
			ChangeEvent<Object, Object> changeEvent = allPayload.get(i);

			List<String> headerKeys = changeEvent.headers()
					.stream()
					.map(Header::getKey)
					.toList();

			assertThat(changeEvent.destination()).startsWith("my-topic");
			if (i > 15) {
				assertThat(changeEvent.destination()).contains(".inventory");
				assertThat(headerKeys).hasSize(4).contains("__name", "__db", "__op", "__table");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private int receivePayloads() {
		Message<?> message = this.queueChannel.receive(500);
		if (message != null) {
			allPayload.addAll((List<ChangeEvent<Object, Object>>) message.getPayload());
			batchCount++;
		}
		return allPayload.size();
	}

	@Configuration
	@EnableIntegration
	@Import(DebeziumTestConfiguration.class)
	public static class BatchTestConfiguration {

		@Bean
		public MessageProducer debeziumMessageProducer(
				@Qualifier("debeziumInputChannel") MessageChannel debeziumInputChannel,
				DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {

			DebeziumMessageProducer debeziumMessageProducer = new DebeziumMessageProducer(debeziumEngineBuilder);
			debeziumMessageProducer.setEnableBatch(true);
			debeziumMessageProducer.setOutputChannel(debeziumInputChannel);
			return debeziumMessageProducer;
		}

	}

}
