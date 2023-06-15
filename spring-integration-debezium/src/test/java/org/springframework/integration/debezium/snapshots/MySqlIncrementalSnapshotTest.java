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

import javax.sql.DataSource;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.awaitility.Awaitility.await;

/**
 * @author Christian Tzolov
 */
@SpringJUnitConfig
@DirtiesContext
public class MySqlIncrementalSnapshotTest extends AbstractIncrementalSnapshotTest implements MySqlTestContainer {

	protected void debeziumReadyCheck() {
		await().until(() -> config.ddlMessages.size() > 1);
	}

	protected void insertCustomer(String firstName, String lastName, String email) {
		jdbcTemplate.update("INSERT INTO customers VALUES (default,?,?,?)", firstName, lastName, email);
	}

	protected void insertProduct(String name, String description, Float weight) {
		jdbcTemplate.update("INSERT INTO products VALUES (default,?,?,?)", name, description, weight);
	}

	protected void deleteProductByName(String name) {
		jdbcTemplate.update("DELETE FROM products WHERE name like ?", name);
	}

	protected void updateProductName(String oldName, String newName) {
		jdbcTemplate.update("UPDATE products set name=? WHERE name like ?", newName, oldName);
	}

	protected void startIncrementalSnapshotFor(String... dataCollections) {
		String names = Stream.of(dataCollections).map(name -> "\"inventory." + name + "\"")
				.collect(Collectors.joining(","));
		jdbcTemplate.update(
				"INSERT INTO dbz_signal (id, type, data) VALUES ('ad-hoc-1', 'execute-snapshot',"
						+ "'{\"data-collections\": [" + names + "],\"type\":\"incremental\"}')");
	}

	protected void stopIncrementalSnapshotFor(String... dataCollections) {
		String names = Stream.of(dataCollections).map(name -> "\"inventory." + name + "\"")
				.collect(Collectors.joining(","));
		jdbcTemplate.update(
				"INSERT INTO dbz_signal (id, type, data) VALUES ('ad-hoc-1', 'stop-snapshot',"
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
		return "__pos";
	}

	@Configuration
	@EnableIntegration
	@Import(AbstractIncrementalSnapshotTest.StreamTestConfiguration.class)
	public static class Config2 {

		@Bean
		public DataSource dataSource() {

			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName("com.mysql.jdbc.Driver");
			dataSource.setUrl(String.format("jdbc:mysql://localhost:%s/inventory?enabledTLSProtocols=TLSv1.2",
					MySqlTestContainer.mappedPort()));
			dataSource.setUsername("root");
			dataSource.setPassword("debezium");
			return dataSource;
		}

		@Bean
		public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {

			return DebeziumEngine.create(KeyValueHeaderChangeEventFormat
					.of(io.debezium.engine.format.JsonByteArray.class,
							io.debezium.engine.format.JsonByteArray.class,
							io.debezium.engine.format.JsonByteArray.class))
					.using(toDebeziumConfig(
							"transforms=flatten",
							"transforms.flatten.type=io.debezium.transforms.ExtractNewRecordState",
							"transforms.flatten.drop.tombstones=true",
							"transforms.flatten.delete.handling.mode=rewrite",
							// Note: 'pos' is Postgres specific metadata
							"transforms.flatten.add.headers=op,pos",
							// "transforms.flatten.add.fields=name,db,op,table",
							// "transforms.flatten.add.headers=name,db,op,table",

							"schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
							"offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

							"key.converter.schemas.enable=false",
							"value.converter.schemas.enable=false",

							"topic.prefix=my-topic",
							"name=my-connector",
							"database.server.id=85744",

							"connector.class=io.debezium.connector.mysql.MySqlConnector",
							"database.user=root",
							"database.password=debezium",
							"database.hostname=localhost",
							"database.port=" + MySqlTestContainer.mappedPort(),

							"snapshot.mode=schema_only",

							"signal.data.collection=inventory.dbz_signal",
							"table.include.list=inventory.orders,inventory.customers,inventory.products,inventory.dbz_signal"));

		}
	}
}
