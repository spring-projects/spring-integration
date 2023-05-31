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

package org.springframework.integration.debezium;

import java.util.Properties;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@Testcontainers(disabledWithoutDocker = true)
public interface DebeziumMySqlTestContainer {

	@Container
	GenericContainer<?> DEBEZIUM_MYSQL =
			new GenericContainer<>("debezium/example-mysql:2.2.0.Final")
					.withExposedPorts(3306)
					.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
					.withEnv("MYSQL_USER", "mysqluser")
					.withEnv("MYSQL_PASSWORD", "mysqlpw");

	static int mysqlPort() {
		return DEBEZIUM_MYSQL.getMappedPort(3306);
	}

	static Properties connectorConfig(int port) {

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
		// config.put("database.server.id", "" + Math.abs(new Random().nextInt()));
		config.put("database.server.name", "my-app-connector");

		config.put("key.converter.schemas.enable", "false");
		config.put("value.converter.schemas.enable", "false");

		config.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");
		config.put("database.user", "debezium");
		config.put("database.password", "dbz");
		config.put("database.hostname", "localhost");
		config.put("database.port", String.valueOf(port));

		return config;
	}

}
