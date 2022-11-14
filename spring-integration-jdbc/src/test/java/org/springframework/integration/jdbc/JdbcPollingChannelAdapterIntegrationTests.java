/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Jonas Partner
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JdbcPollingChannelAdapterIntegrationTests {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeClass
	public static void setUp() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.DERBY)
				.addScript("classpath:org/springframework/integration/jdbc/pollingChannelAdapterIntegrationTest.sql");
		embeddedDatabase = builder.build();
		jdbcTemplate = new JdbcTemplate(embeddedDatabase);
	}

	@AfterClass
	public static void tearDown() {
		embeddedDatabase.shutdown();
	}

	@After
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
		assertThat(payload instanceof List<?>).as("Wrong payload type").isTrue();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(1);
		assertThat(rows.get(0) instanceof Map<?, ?>).as("Returned row not a map").isTrue();
		Map<?, ?> row = (Map<?, ?>) rows.get(0);
		assertThat(row.get("id")).as("Wrong id").isEqualTo(1);
		assertThat(row.get("status")).as("Wrong status").isEqualTo(2);
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
		assertThat(payload instanceof List<?>).as("Wrong payload type").isTrue();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(1);
		assertThat(rows.get(0) instanceof Map<?, ?>).as("Returned row not a map").isTrue();
		Map<?, ?> row = (Map<?, ?>) rows.get(0);
		assertThat(row.get("id")).as("Wrong id").isEqualTo(1);
		assertThat(row.get("status")).as("Wrong status").isEqualTo(2);
	}

	@Test
	public void testSimplePollForListWithRowMapperNoUpdate() {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase, "select * from item");
		adapter.setRowMapper(new ItemRowMapper());
		jdbcTemplate.update("insert into item values(1,2)");
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(1);
		assertThat(rows.get(0) instanceof Item).as("Wrong payload type").isTrue();
		Item item = (Item) rows.get(0);
		assertThat(item.getId()).as("Wrong id").isEqualTo(1);
		assertThat(item.getStatus()).as("Wrong status").isEqualTo(2);
	}

	@Test
	public void testSimplePollForListWithRowMapperAndOneUpdate() throws Exception {
		JdbcPollingChannelAdapter adapter = new JdbcPollingChannelAdapter(embeddedDatabase,
				"select * from item where status=2");
		adapter.setUpdateSql("update item set status = 10 where id in (:id)");
		adapter.setRowMapper(new ItemRowMapper());
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(2);
		assertThat(rows.get(0) instanceof Item).as("Wrong payload type").isTrue();
		Item item = (Item) rows.get(0);
		assertThat(item.getId()).as("Wrong id").isEqualTo(1);
		assertThat(item.getStatus()).as("Wrong status").isEqualTo(2);

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
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(2);
		assertThat(rows.get(0) instanceof Item).as("Wrong payload type").isTrue();
		Item item = (Item) rows.get(0);
		assertThat(item.getId()).as("Wrong id").isEqualTo(1);
		assertThat(item.getStatus()).as("Wrong status").isEqualTo(2);

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
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(1);
		assertThat(rows.get(0) instanceof Item).as("Wrong payload type").isTrue();
		Item item = (Item) rows.get(0);
		assertThat(item.getId()).as("Wrong id").isEqualTo(1);
		assertThat(item.getStatus()).as("Wrong status").isEqualTo(2);

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
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		jdbcTemplate.update("insert into item values(1,2)");
		jdbcTemplate.update("insert into item values(2,2)");

		adapter.receive();
		Message<Object> message = adapter.receive();
		Object payload = message.getPayload();
		List<?> rows = (List<?>) payload;
		assertThat(rows.size()).as("Wrong number of elements").isEqualTo(1);
		assertThat(rows.get(0) instanceof Item).as("Wrong payload type").isTrue();
		Item item = (Item) rows.get(0);
		assertThat(item.getId()).as("Wrong id").isEqualTo(2);
		assertThat(item.getStatus()).as("Wrong status").isEqualTo(2);

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

	private static class Item {

		private int id;

		private int status;

		public int getId() {
			return this.id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getStatus() {
			return this.status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		@Override
		public String toString() {
			return "Item [id=" + this.id + ", status=" + this.status + "]";
		}

	}

	private static class ItemRowMapper implements RowMapper<Item> {

		@Override
		public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
			Item item = new Item();
			item.setId(rs.getInt(1));
			item.setStatus(rs.getInt(2));
			return item;
		}

	}

}
