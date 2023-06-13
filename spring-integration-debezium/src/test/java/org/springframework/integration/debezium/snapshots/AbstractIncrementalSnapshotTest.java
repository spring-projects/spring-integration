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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.debezium.dsl.Debezium;
import org.springframework.integration.debezium.dsl.DebeziumMessageProducerSpec;
import org.springframework.integration.debezium.support.DebeziumHeaders;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The events with operation types: Create(c), Update(u) and Delete(d) are emitted from the WAL (e.g. streamed) while
 * those of type Reade(r) are produced from the Incremental Snapshot process. Later uses database query in chunks not
 * the WAL.
 * <p>
 * In case of conflicts between WAL and Query events, the Incremental Snapshot mechanism performs deduplication based on
 * the primary key. Therefore the primary keys are compulsory for the incremental snapshots.
 *
 * @author Christian Tzolov
 */
public abstract class AbstractIncrementalSnapshotTest {

	static final LogAccessor logger = new LogAccessor(AbstractIncrementalSnapshotTest.class);

	public static final String UPDATE_OPERATION = "u";
	public static final String CREATE_OPERATION = "c";
	public static final String DELETE_OPERATION = "d";
	public static final String READ_OPERATION = "r";

	@Autowired
	protected StreamTestConfiguration config;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeAll
	public static void beforeAll() {
		Awaitility.setDefaultTimeout(30, TimeUnit.SECONDS);
	}

	@Test
	public void incrementalSnapshotTest() throws InterruptedException {

		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

		// The 'snapshot.mode=never' (or 'schema_only' for some datasources) disables the
		// initial snapshot mechanism. It ensures that no messages were received after debezium (re)start.
		// For some data sources a 'schema_only' should be used as 'never' doesn't work or is not supported.
		// The DDL change events are filtered out and not used in the verifications.

		assertThat(config.messagesPerTableMap)
				.as("Initial snapshot should be disabled. Check the 'snapshot.mode'.")
				.isEmpty();
		assertThat(config.totalMessageCount.get()).isEqualTo(0);

		// Do customer insert to generate new change events.
		insertCustomer("Additional", "Customer", "additional.customer@acme.com");

		// Verify that one 'customers' change event of type "c" (e.g. Create) is received.
		await().until(() -> config.totalMessageCount.get() == 1);
		verifyThat(customers(), 1, CREATE_OPERATION);

		// Reset the test counts.
		resetCounts();

		// ---------------------------------------
		// --- Start the Incremental Snapshots ---
		// ---------------------------------------
		startIncrementalSnapshotFor("orders", "customers", "products");

		// Wait until all expected snapshot and stream events have arrive.
		await().until(() -> config.totalMessageCount.get() == 27);
		assertThat(config.messagesPerTableMap)
				.as("Expects 4 group of events: 'orders', 'customers', 'products' and 'dbz_signal'")
				.hasSize(4);

		// Number of 'orders' events of type 'r'.
		verifyThat(orders(), 4, READ_OPERATION);

		// Number of 'customers' events of type 'r'.
		verifyThat(customers(), 5, READ_OPERATION);

		// Verify that the 'customers' snapshot in addition to the original entries also contains
		// the value that we've just inserted.
		assertThat(config.messagesPerTableMap.get(customers()).stream()
				.filter(m -> new String((byte[]) m.getPayload()).contains("\"first_name\":\"Additional\"")).toList())
						.hasSize(1);

		// Number of 'products' events of type 'r'.
		verifyThat(products(), 9, READ_OPERATION);

		// Expects internal Debezium signal-table has expected number of 'c' (Create) signals.
		verifyThat(dbzSignal(), 9, CREATE_OPERATION);

		resetCounts();

		// Insert 5 new products with first names 'newProduct[0-4]'.
		for (int i = 0; i < 5; i++) {
			insertProduct("newProduct" + i, "newProduct" + i, 6.18f);
		}

		// Verify that 5 products create ('c') events were received.
		await().until(() -> config.totalMessageCount.get() == 5);
		verifyThat(products(), 5, CREATE_OPERATION);

		resetCounts();

		// Delete all 5 'newProductXXX' products.
		deleteProductByName("newProduct%");

		// Verify that 5 products delete ('d') events were received.
		await().until(() -> config.totalMessageCount.get() == 5);
		verifyThat(products(), 5, DELETE_OPERATION);

		resetCounts();

		// Update 1 existing product.
		updateProductName("scooter", "bicycle");

		// Verify that 1 products update ('u') event is received.
		await().until(() -> config.totalMessageCount.get() == 1);
		verifyThat(products(), 1, UPDATE_OPERATION);

		resetCounts();

		// Insert 1 new products with first names 'TEST'.
		insertProduct("TEST", "TEST", 6.18f);

		// Verify that 1 products create ('c') event is received.
		await().until(() -> config.totalMessageCount.get() == 1);
		verifyThat(products(), 1, CREATE_OPERATION);

		resetCounts();

		// -------------------------------------------------------------------
		// --- (Re)Start the Incremental Snapshots BUT ONLY for 'products' ---
		// -------------------------------------------------------------------
		startIncrementalSnapshotFor("products");

		// Wait until all expected snapshot and stream events have arrive.
		await().until(() -> config.totalMessageCount.get() == 15);

		// Number of 'dbzSignal' events of type 'c'(e.g. not for the snapshot but from the CDC).
		verifyThat(dbzSignal(), 5, CREATE_OPERATION);

		// Original product change events + the new insert.
		// Note that the 5 insert/delete operations are not present.
		verifyThat(products(), 10, READ_OPERATION);

		// The new inserted ('TEST') and updated ('bicycle') products should be among the products snapshot entries.
		assertThat(config.messagesPerTableMap.get(products()).stream()
				.filter(m -> new String((byte[]) m.getPayload()).contains("\"name\":\"TEST\"")).toList())
						.hasSize(1);
		assertThat(config.messagesPerTableMap.get(products()).stream()
				.filter(m -> new String((byte[]) m.getPayload()).contains("\"name\":\"bicycle\"")).toList())
						.hasSize(1);

		resetCounts();

		// --------------------------------------
		// --- Stop the Incremental Snapshots ---
		// --------------------------------------
		stopIncrementalSnapshotFor("orders", "customers", "products");

		// Verify that only the stop signal has been received.
		await().until(() -> config.totalMessageCount.get() == 1);
		verifyThat(dbzSignal(), 1, CREATE_OPERATION);

		resetCounts();
	}

	private void verifyThat(String destination, int expectedCount, String expectedOperationType) {
		assertThat(config.messagesPerTableMap.get(destination)).hasSize(expectedCount).as(
				"Expected messageMap size to be " + expectedCount + " but was: "
						+ config.messagesPerTableMap.get(destination).size());

		config.messagesPerTableMap.get(destination).stream()
				.forEach(m -> assertThat(operationType(m)).isEqualTo(expectedOperationType));

	}

	// abstract protected int customersCount();

	abstract protected String customers();

	abstract protected String products();

	abstract protected String orders();

	abstract protected String dbzSignal();

	abstract protected void insertCustomer(String firstName, String lastName, String email);

	abstract protected void insertProduct(String name, String description, Float weight);

	abstract protected void deleteProductByName(String name);

	abstract protected void updateProductName(String oldName, String newName);

	abstract protected void startIncrementalSnapshotFor(String... dataCollections);

	abstract protected void stopIncrementalSnapshotFor(String... dataCollections);

	abstract protected String getLsnHeaderName();

	protected void resetCounts() {
		config.messagesPerTableMap.keySet().stream().forEach(destination -> {
			String lsnJoin = config.messagesPerTableMap.get(destination).stream()
					.map(m -> "[" + operationType(m) + "]" + getLsn(m)).collect(Collectors.joining(","));
			logger.info("LSNs: " + destination + " -> " + lsnJoin);
			// System.out.println("LSNs: " + destination + " -> " + lsnJoin);
		});
		config.messagesPerTableMap.clear();
		config.totalMessageCount.set(0);
	}

	protected static ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	protected static Map<String, Object> toMap(Object value) {
		try {
			return mapper.readValue(new String((byte[]) value), Map.class);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	protected static String operationType(Message<?> message) {
		return "" + toMap(message.getHeaders().get("__op")).get("payload");
	}

	protected static Properties toDebeziumConfig(String... properties) {
		Properties config = new Properties();
		for (String property : properties) {
			String key = property.split("=", 2)[0];
			String value = property.split("=", 2)[1];
			config.put(key, value);
		}
		return config;
	}

	protected String getLsn(Message<?> message) {
		String headerName = this.getLsnHeaderName();
		try {
			return "" + mapper.readValue((byte[]) message.getHeaders().get(headerName), Map.class)
					.get("payload");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "null";
	}

	@Configuration
	@EnableIntegration
	@EnableAutoConfiguration(exclude = { MongoAutoConfiguration.class })
	public static class StreamTestConfiguration {

		private final AtomicInteger totalMessageCount = new AtomicInteger(0);

		private Map<String, List<Message<?>>> messagesPerTableMap = new ConcurrentHashMap<>();

		@Bean
		public IntegrationFlow streamFlowFromBuilder(DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> builder) {

			DebeziumMessageProducerSpec dsl = Debezium.inboundChannelAdapter(builder)
					.headerNames("*")
					.contentType("application/json")
					.enableBatch(false)
					.enableEmptyPayload(true);

			return IntegrationFlow.from(dsl)
					.handle(m -> {
						String destination = (String) m.getHeaders().get(DebeziumHeaders.DESTINATION);

						if ("my-topic".equals(destination)) {
							return; // Skip DDL change events.
						}

						totalMessageCount.incrementAndGet();
						messagesPerTableMap.putIfAbsent(destination, Collections.synchronizedList(new ArrayList<>()));
						messagesPerTableMap.get(destination).add(m);
					}).get();
		}

		@Bean
		public JdbcTemplate myJdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		@Primary
		@ConfigurationProperties("app.datasource")
		public DataSourceProperties dataSourceProperties() {
			return new DataSourceProperties();
		}

		@Bean
		public HikariDataSource dataSource(DataSourceProperties dataSourceProperties) {
			return dataSourceProperties.initializeDataSourceBuilder()
					.type(HikariDataSource.class)
					.build();
		}

	}

}
