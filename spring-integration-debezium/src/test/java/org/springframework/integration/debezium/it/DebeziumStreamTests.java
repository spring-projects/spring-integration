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

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
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
import org.springframework.integration.debezium.support.DefaultDebeziumHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
public class DebeziumStreamTests implements DebeziumMySqlTestContainer {

	@Autowired
	@Qualifier("queueChannel")
	private QueueChannel queueChannel;

	@Test
	void streamMode() {
		boolean foundDebeziumHeaders = false;
		for (int i = 0; i < EXPECTED_DB_TX_COUNT; i++) {
			Message<?> message = this.queueChannel.receive(30_000);
			assertThat(message).isNotNull();

			if (message.getHeaders().size() > 5) {
				assertThat(message.getHeaders()).containsKeys("__name", "__db", "__table");
				foundDebeziumHeaders = true;
			}
		}
		assertThat(foundDebeziumHeaders).isTrue();
	}

	@Configuration
	@EnableIntegration
	@Import(DebeziumTestConfiguration.class)
	public static class StreamTestConfiguration {

		@Bean
		public MessageProducer debeziumMessageProducer(
				@Qualifier("debeziumInputChannel") MessageChannel debeziumInputChannel,
				DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {

			DebeziumMessageProducer debeziumMessageProducer = new DebeziumMessageProducer(debeziumEngineBuilder);

			// This corresponds to the 'transforms.unwrap.add.headers=name,db,op,table' debezium configuration in
			// the DebeziumTestConfiguration#debeziumEngineBuilder!
			DefaultDebeziumHeaderMapper debeziumHeaderMapper = new DefaultDebeziumHeaderMapper();
			debeziumHeaderMapper.setHeaderNamesToMap("__name", "__db", "__op", "__table");
			debeziumMessageProducer.setHeaderMapper(debeziumHeaderMapper);

			debeziumMessageProducer.setOutputChannel(debeziumInputChannel);
			return debeziumMessageProducer;
		}

	}

}
