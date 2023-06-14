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

package org.springframework.integration.debezium.snapshots;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;
import org.junit.Ignore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Christian Tzolov
 */
@Ignore
@TestPropertySource(properties = {
		// JdbcTemplate configuration
		"app.datasource.username=postgres",
		"app.datasource.password=postgres",
		"app.datasource.type=com.zaxxer.hikari.HikariDataSource"
})
@SpringJUnitConfig
@DirtiesContext
public class PostgresIncrementalSnapshotTest extends AbstractIncrementalSnapshotTest implements PostgresTestContainer {

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("app.datasource.url",
				() -> String.format("jdbc:postgresql://localhost:%s/postgres", PostgresTestContainer.mappedPort()));
	}

	protected void debeziumReadyCheck() {
	}

	protected void insertCustomer(String firstName, String lastName, String email) {
		jdbcTemplate.update("INSERT INTO inventory.customers VALUES (default,?,?,?)", firstName, lastName, email);
	}

	protected void insertProduct(String name, String description, Float weight) {
		jdbcTemplate.update("INSERT INTO inventory.products VALUES (default,?,?,?)", name, description, weight);
	}

	protected void deleteProductByName(String name) {
		jdbcTemplate.update("DELETE FROM inventory.products WHERE name like ?", name);
	}

	protected void updateProductName(String oldName, String newName) {
		jdbcTemplate.update("UPDATE inventory.products set name=? WHERE name like ?", newName, oldName);
	}

	protected void startIncrementalSnapshotFor(String... dataCollections) {
		String names = Stream.of(dataCollections).map(name -> "\"inventory." + name + "\"")
				.collect(Collectors.joining(","));
		jdbcTemplate.update(
				"INSERT INTO inventory.dbz_signal (id, type, data) VALUES ('ad-hoc-1', 'execute-snapshot',"
						+ "'{\"data-collections\": [" + names + "],\"type\":\"incremental\"}')");
	}

	protected void stopIncrementalSnapshotFor(String... dataCollections) {
		String names = Stream.of(dataCollections).map(name -> "\"inventory." + name + "\"")
				.collect(Collectors.joining(","));
		jdbcTemplate.update(
				"INSERT INTO inventory.dbz_signal (id, type, data) VALUES ('ad-hoc-1', 'stop-snapshot',"
						+ "'{\"data-collections\": [" + names + "],\"type\":\"incremental\"}')");
	}

	protected String customers() {
		return "my-topic.inventory.customers";
	}

	protected String products() {
		return "my-topic.inventory.products";
	}

	protected String orders() {
		return "my-topic.inventory.orders";
	}

	protected String dbzSignal() {
		return "my-topic.inventory.dbz_signal";
	}

	protected String getLsnHeaderName() {
		return "__lsn";
	}

	@Configuration
	@EnableIntegration
	@Import(AbstractIncrementalSnapshotTest.StreamTestConfiguration.class)
	public static class Config2 {
		@Bean
		public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {

			return DebeziumEngine.create(KeyValueHeaderChangeEventFormat
					.of(io.debezium.engine.format.JsonByteArray.class,
							io.debezium.engine.format.JsonByteArray.class,
							io.debezium.engine.format.JsonByteArray.class))
					.using(toDebeziumConfig(
							// Common configuration. Common for all DBs.
							"transforms=flatten",
							"transforms.flatten.type=io.debezium.transforms.ExtractNewRecordState",
							"transforms.flatten.drop.tombstones=true",
							"transforms.flatten.delete.handling.mode=rewrite",
							// Note: 'lsn' is Postgres specific metadata
							"transforms.flatten.add.headers=op,lsn",
							// "transforms.flatten.add.fields=name,db,op,table",
							// "transforms.flatten.add.headers=name,db,op,table",

							"schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
							"offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

							"key.converter.schemas.enable=false",
							"value.converter.schemas.enable=false",

							"topic.prefix=my-topic",
							"name=my-connector",
							"database.server.id=85744",

							// Postgres specific configuration.
							"connector.class=io.debezium.connector.postgresql.PostgresConnector",
							"database.user=postgres",
							"database.password=postgres",
							"slot.name=debezium",
							"database.dbname=postgres",
							"database.hostname=localhost",

							// "snapshot.mode=initial_only",
							"snapshot.mode=never",

							"signal.data.collection=inventory.dbz_signal",
							"table.include.list=inventory.orders,inventory.customers,inventory.products,inventory.dbz_signal",

							"database.port=" + PostgresTestContainer.mappedPort()));
		}
	}

}
