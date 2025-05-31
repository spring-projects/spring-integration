/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Jiandong Ma
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JdbcOutboundGatewayTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void testSetMaxRowsPerPollWithoutSelectQuery() {
		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "update something");

		try {
			jdbcOutboundGateway.setMaxRows(10);
			jdbcOutboundGateway.setBeanFactory(mock(BeanFactory.class));
			jdbcOutboundGateway.afterPropertiesSet();

			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
					.isEqualTo("If you want to set 'maxRows', then you must provide a 'selectQuery'.");
		}
	}

	@Test
	public void testConstructorWithNullJdbcOperations() {
		JdbcOperations jdbcOperations = null;

		try {
			new JdbcOutboundGateway(jdbcOperations, "select * from DOES_NOT_EXIST");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'jdbcOperations' must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testConstructorWithEmptyAndNullQueries() {
		final String selectQuery = "   ";
		final String updateQuery = null;

		try {
			new JdbcOutboundGateway(dataSource, updateQuery, selectQuery);

			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
					.isEqualTo("The 'updateQuery' and the 'selectQuery' must not both be null or empty.");
		}
	}

	@Test
	public void testSetMaxRowsPerPoll() {
		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "select * from DOES_NOT_EXIST");

		try {
			jdbcOutboundGateway.setMaxRows(null);

			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'maxRows' must not be null.");
		}
	}

	@Test
	public void testQueryForStream() {
		// GIVEN
		int rowCnt = 30_000;
		for (int i = 0; i < rowCnt; i++) {
			jdbcTemplate.update("insert into item values(%s,0)".formatted(i + 1));
		}
		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, null, "select * from item");
		jdbcOutboundGateway.setRowMapper((RowMapper<Item>) (rs, rowNum) -> new Item(rs.getInt(1), rs.getInt(2)));
		jdbcOutboundGateway.setQueryForStream(true);
		List<Item> resultList = new ArrayList<>();
		jdbcOutboundGateway.setStreamConsumer(obj -> {
			Item item = (Item) obj;
			resultList.add(item);
		});
		QueueChannel replyChannel = new QueueChannel();
		jdbcOutboundGateway.setOutputChannel(replyChannel);
		jdbcOutboundGateway.setBeanFactory(mock(BeanFactory.class));
		jdbcOutboundGateway.afterPropertiesSet();
		// WHEN
		jdbcOutboundGateway.handleMessage(MessageBuilder.withPayload("foo").build());
		// THEN
		assertThat(resultList).hasSize(rowCnt);
		for (int i = 0; i < rowCnt; i++) {
			assertThat(resultList.get(i).id).isEqualTo(i + 1);
			assertThat(resultList.get(i).status).isEqualTo(0);
		}
		Message<?> replyMessage = replyChannel.receive();
		List<?> payload = (List<?>) replyMessage.getPayload();
		assertThat(payload).isEmpty();
	}

	record Item(int id, int status) { }

	@Configuration
	public static class Config {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.HSQL)
					.addScript("classpath:org/springframework/integration/jdbc/jdbcOutboundGatewayTest.sql")
					.build();
		}

		@Bean
		public JdbcTemplate jdbcTemplate() {
			return new JdbcTemplate(dataSource());
		}
	}
}
