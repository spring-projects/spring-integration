/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jdbc.inbound;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Jonas Partner
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JdbcPollingChannelAdapterIntegrationTests implements TestApplicationContextAware {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeAll
	public static void setUp() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.DERBY)
				.addScript("classpath:org/springframework/integration/jdbc/inbound/pollingChannelAdapterIntegrationTest.sql");
		embeddedDatabase = builder.build();
		jdbcTemplate = new JdbcTemplate(embeddedDatabase);
	}

	@AfterAll
	public static void tearDown() {
		embeddedDatabase.shutdown();
	}

	@AfterEach
	public void cleanup() {
		jdbcTemplate.execute("DELETE FROM item");
		jdbcTemplate.execute("DELETE FROM copy");
	}

	@Test
	public void testSimplePollForListOfMapsNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase, "select * from item");
		jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Map.class))
				.hasSize(1)
				.first()
				.asInstanceOf(map(String.class, Object.class))
				.containsEntry("id", 1)
				.containsEntry("status", 2);
	}

	@Test
	public void testParameterizedPollForListOfMapsNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase,
				"select * from item where status=:status");
		adapter.setSelectSqlParameterSource(new SqlParameterSource() {

			@Override
			public boolean hasValue(String name) {
				return "status".equals(name);
			}

			@Override
			public Object getValue(String name) throws IllegalArgumentException {
				return 2;
			}

			@Override
			public String getTypeName(String name) {
				return null;
			}

			@Override
			public int getSqlType(String name) {
				return Types.INTEGER;
			}

		});
		jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Map.class))
				.hasSize(1)
				.first()
				.asInstanceOf(map(String.class, Object.class))
				.containsEntry("id", 1)
				.containsEntry("status", 2);
	}

	@Test
	public void testSimplePollForListWithRowMapperNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase, "select * from item");
		adapter.setRowMapper(new ItemRowMapper());
		jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Item.class))
				.hasSize(1)
				.first()
				.asInstanceOf(type(Item.class))
				.satisfies(item -> {
					assertThat(item.id()).as("Wrong id").isEqualTo(1);
					assertThat(item.status()).as("Wrong status").isEqualTo(2);
				});
	}

	@Test
	public void testSimplePollForListWithRowMapperAndOneUpdate() throws Exception {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase,
				"select * from item where status=2");
		adapter.setUpdateSql("update item set status = 10 where id in (:id)");
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Item.class))
				.hasSize(2)
				.first()
				.asInstanceOf(type(Item.class))
				.satisfies(item -> {
					assertThat(item.id()).as("Wrong id").isEqualTo(1);
					assertThat(item.status()).as("Wrong status").isEqualTo(2);
				});

		int countOfStatusTwo = jdbcTemplate.queryForObject("select count(*) from item where status = 2", Integer.class);
		assertThat(countOfStatusTwo).as("Status not updated incorrect number of rows with status 2").isEqualTo(0);

		int countOfStatusTen = jdbcTemplate.queryForObject("select count(*) from item where status = 10", Integer.class);
		assertThat(countOfStatusTen).as("Status not updated incorrect number of rows with status 10").isEqualTo(2);
	}

	@Test
	public void testSimplePollForListWithRowMapperAndUpdatePerRow() throws Exception {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase,
				"select * from item where status=2");
		adapter.setUpdateSql("update item set status = 10 where id = :id");
		adapter.setUpdatePerRow(true);
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Item.class))
				.hasSize(2)
				.first()
				.asInstanceOf(type(Item.class))
				.satisfies(item -> {
					assertThat(item.id()).as("Wrong id").isEqualTo(1);
					assertThat(item.status()).as("Wrong status").isEqualTo(2);
				});

		int countOfStatusTwo = jdbcTemplate.queryForObject("select count(*) from item where status = 2", Integer.class);
		assertThat(countOfStatusTwo).as("Status not updated incorrect number of rows with status 2").isEqualTo(0);

		int countOfStatusTen = jdbcTemplate.queryForObject("select count(*) from item where status = 10", Integer.class);
		assertThat(countOfStatusTen).as("Status not updated incorrect number of rows with status 10").isEqualTo(2);
	}

	@Test
	public void testSimplePollForListWithRowMapperAndInsertPerRowAndMaxRows() throws Exception {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase,
				"select * from item where id not in (select id from copy)");
		adapter.setUpdateSql("insert into copy values(:id,10)");
		adapter.setUpdatePerRow(true);
		adapter.setMaxRows(1);
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Item.class))
				.hasSize(1)
				.first()
				.asInstanceOf(type(Item.class))
				.satisfies(item -> {
					assertThat(item.id()).as("Wrong id").isEqualTo(1);
					assertThat(item.status()).as("Wrong status").isEqualTo(2);
				});

		int countOfStatusTwo = jdbcTemplate.queryForObject("select count(*) from item where status = 2", Integer.class);
		assertThat(countOfStatusTwo).as("Status not updated incorrect number of rows with status 2").isEqualTo(2);

		int countOfStatusTen = jdbcTemplate.queryForObject("select count(*) from copy where status = 10", Integer.class);
		assertThat(countOfStatusTen).as("Status not updated incorrect number of rows with status 10").isEqualTo(1);
	}

	@Test
	public void testSimplePollForListWithRowMapperAndUpdatePerRowWithMaxRows() throws Exception {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase,
				"select * from item where status=2");
		adapter.setUpdateSql("update item set status = 10 where id = :id");
		adapter.setUpdatePerRow(true);
		adapter.setMaxRows(1);
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		adapter.receive();
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(list(Item.class))
				.hasSize(1)
				.first()
				.asInstanceOf(type(Item.class))
				.satisfies(item -> {
					assertThat(item.id()).as("Wrong id").isEqualTo(2);
					assertThat(item.status()).as("Wrong status").isEqualTo(2);
				});

		int countOfStatusTwo = jdbcTemplate.queryForObject("select count(*) from item where status = 2", Integer.class);
		assertThat(countOfStatusTwo).as("Status not updated incorrect number of rows with status 2").isEqualTo(0);

		int countOfStatusTen = jdbcTemplate.queryForObject("select count(*) from item where status = 10", Integer.class);
		assertThat(countOfStatusTen).as("Status not updated incorrect number of rows with status 10").isEqualTo(2);
	}

	@Test
	public void testEmptyPoll() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase, "select * from item");
		Message<Object> message = adapter.receive();
		assertThat(message).as("Message received when no rows in table").isNull();
	}

	private record Item(int id, int status) {

	}

	private static class ItemRowMapper implements RowMapper<Item> {

		@Override
		public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Item(rs.getInt(1), rs.getInt(2));
		}

	}

}
