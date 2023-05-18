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

package org.springframework.integration.debezium.it;

import java.util.Properties;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.JsonByteArray;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@Configuration
@EnableIntegration
public class DebeziumTestConfiguration {

	static final Log logger = LogFactory.getLog(DebeziumTestConfiguration.class);

	@Bean
	public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {

		Properties config = new Properties();

		config.put("transforms", "unwrap");
		config.put("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
		config.put("transforms.unwrap.drop.tombstones", "false");
		config.put("transforms.unwrap.delete.handling.mode", "rewrite");
		config.put("transforms.unwrap.add.fields", "name,db,op,table");
		config.put("transforms.unwrap.add.headers", "name,db,op,table");

		config.put("schema.history.internal", "io.debezium.relational.history.MemorySchemaHistory");
		config.put("offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");

		config.put("topic.prefix", "my-topic");
		config.put("name", "my-connector");
		config.put("database.server.id", "85744");
		config.put("database.server.name", "my-app-connector");

		config.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");
		config.put("database.user", "debezium");
		config.put("database.password", "dbz");
		config.put("database.hostname", "localhost");
		config.put("database.port", String.valueOf(DebeziumMySqlTestContainer.DEBEZIUM_MYSQL.getMappedPort(3306)));
		// config.put("database.port", String.valueOf(DebeziumMySqlTestContainer.mysqlPort()));

		KeyValueHeaderChangeEventFormat<JsonByteArray, JsonByteArray, JsonByteArray> format = KeyValueHeaderChangeEventFormat
				.of(io.debezium.engine.format.JsonByteArray.class, io.debezium.engine.format.JsonByteArray.class,
						io.debezium.engine.format.JsonByteArray.class);

		// Class<JsonByteArray> format = io.debezium.engine.format.JsonByteArray.class;

		return DebeziumEngine.create(format).using(config);
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

	@Bean
	// @ServiceActivator(inputChannel = "debeziumInputChannel")
	public MessageHandler handler() {
		return new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				logger.info(new String((byte[]) message.getPayload()));
			}
		};
	}
}
