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
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.debezium.DebeziumMySqlTestContainer;
import org.springframework.messaging.MessageChannel;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@Configuration
@EnableIntegration
public class DebeziumTestConfiguration {

	@Bean
	public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {

		return DebeziumEngine.create(KeyValueHeaderChangeEventFormat
						.of(
								io.debezium.engine.format.JsonByteArray.class,
								io.debezium.engine.format.JsonByteArray.class,
								io.debezium.engine.format.JsonByteArray.class))
				.using(DebeziumMySqlTestContainer.connectorConfig(DebeziumMySqlTestContainer.mysqlPort()));
	}

	@Bean
	public MessageChannel debeziumInputChannel() {
		return new DirectChannel();
	}

	@Bean
	@BridgeFrom("debeziumInputChannel")
	public MessageChannel queueChannel() {
		return new QueueChannel();
	}

}
