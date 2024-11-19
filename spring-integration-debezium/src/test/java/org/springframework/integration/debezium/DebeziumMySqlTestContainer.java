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

package org.springframework.integration.debezium;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@Testcontainers(disabledWithoutDocker = true)
public interface DebeziumMySqlTestContainer {

	int EXPECTED_DB_TX_COUNT = 52;

	MySQLContainer<?> DEBEZIUM_MYSQL =
			new MySQLContainer<>(
					DockerImageName.parse("debezium/example-mysql:3.0.0.Final")
							.asCompatibleSubstituteFor("mysql"))
					.withUsername("mysqluser")
					.withPassword("mysqlpw");

	@BeforeAll
	static void startContainer() {
		DEBEZIUM_MYSQL.start();
	}

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

		UUID uuid = UUID.randomUUID();
		config.put("name", "my-connector-" + uuid);

		// Topic prefix for the database server or cluster.
		config.put("topic.prefix", "my-topic-" + uuid);
		// Unique ID of the connector.
		config.put("database.server.id", "" + Math.abs(ThreadLocalRandom.current().nextInt()));

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
