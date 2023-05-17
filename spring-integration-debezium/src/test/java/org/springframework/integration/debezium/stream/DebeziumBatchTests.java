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

package org.springframework.integration.debezium.stream;

import java.util.List;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.debezium.inbound.DebeziumMessageProducer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@SpringJUnitConfig(DebeziumBatchTests.BatchTestConfiguration.class)
@DirtiesContext
public class DebeziumBatchTests implements DebeziumMySqlTestContainer {

	static final Log logger = LogFactory.getLog(DebeziumBatchTests.class);

	@Autowired
	@Qualifier("queueChannel")
	private QueueChannel queueChannel;

	@Test
	@SuppressWarnings("unchecked")
	void mysqlInventoryDB() throws InterruptedException {
		Message<?> message = this.queueChannel.receive(10_000);
		assertThat(message).isNotNull();
		List<ChangeEvent<Object, Object>> payload = (List<ChangeEvent<Object, Object>>) message.getPayload();
		assertThat(payload).hasSize(52);

		// No more messages.
		assertThat(this.queueChannel.receive(5_000)).isNull();
	}

	@Configuration
	@EnableIntegration
	@Import(DebeziumTestConfiguration.class)
	public static class BatchTestConfiguration {
		@Bean
		public MessageProducer debeziumMessageProducer(@Qualifier("debeziumInputChannel") MessageChannel debeziumInputChannel,
				DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {
			DebeziumMessageProducer debeziumMessageProducer = new DebeziumMessageProducer(debeziumEngineBuilder);
			debeziumMessageProducer.setEnableBatch(true);
			debeziumMessageProducer.setOutputChannel(debeziumInputChannel);
			return debeziumMessageProducer;
		}

	}

}
