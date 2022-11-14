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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
public class JdbcMessageHandlerIntegrationTests {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeAll
	public static void setUp() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.HSQL).addScript(
				"classpath:org/springframework/integration/jdbc/messageHandlerIntegrationTest.sql");
		embeddedDatabase = builder.build();
		jdbcTemplate = new JdbcTemplate(embeddedDatabase);
	}

	@AfterAll
	public static void tearDown() {
		embeddedDatabase.shutdown();
	}

	@AfterEach
	public void cleanup() {
		jdbcTemplate.execute("DELETE FROM FOOS");
	}

	@Test
	public void testSimpleStaticInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (1, 0, 'foo')");
		handler.afterPropertiesSet();
		Message<String> message = new GenericMessage<>("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertThat(map.get("ID")).as("Wrong id").isEqualTo("1");
		assertThat(map.get("STATUS")).as("Wrong status").isEqualTo(0);
		assertThat(map.get("NAME")).as("Wrong name").isEqualTo("foo");
	}

	@Test
	public void testSimpleDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (1, 0, :payload)");
		handler.afterPropertiesSet();
		Message<String> message = new GenericMessage<>("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertThat(map.get("NAME")).as("Wrong name").isEqualTo("foo");
	}

	@Test
	public void testInsertBatch() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (:payload, 0, :payload)");
		handler.afterPropertiesSet();

		Message<List<String>> message = new GenericMessage<>(Arrays.asList("foo1", "foo2", "foo3"));
		handler.handleMessage(message);

		List<Map<String, Object>> foos = jdbcTemplate.queryForList("SELECT * FROM FOOS ORDER BY id");

		assertThat(foos.size()).isEqualTo(3);

		assertThat(foos.get(0).get("NAME")).isEqualTo("foo1");
		assertThat(foos.get(1).get("NAME")).isEqualTo("foo2");
		assertThat(foos.get(2).get("NAME")).isEqualTo("foo3");
	}

	@Test
	public void testInsertBatchOfMessages() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (:id, 0, :payload)");
		ExpressionEvaluatingSqlParameterSourceFactory sqlParameterSourceFactory =
				new ExpressionEvaluatingSqlParameterSourceFactory();
		sqlParameterSourceFactory.setParameterExpressions(Map.of("id", "headers.id", "payload", "payload"));
		sqlParameterSourceFactory.setBeanFactory(mock(BeanFactory.class));
		handler.setSqlParameterSourceFactory(sqlParameterSourceFactory);
		handler.afterPropertiesSet();

		List<GenericMessage<String>> payload =
				IntStream.range(1, 4)
						.mapToObj(i -> "Item" + i)
						.map(GenericMessage::new)
						.toList();

		handler.handleMessage(new GenericMessage<>(payload));

		List<Map<String, Object>> foos = jdbcTemplate.queryForList("SELECT * FROM FOOS ORDER BY NAME");

		assertThat(foos.size()).isEqualTo(3);

		assertThat(foos.get(0).get("NAME")).isEqualTo("Item1");
		assertThat(foos.get(1).get("NAME")).isEqualTo("Item2");
		assertThat(foos.get(2).get("NAME")).isEqualTo("Item3");

		assertThat(foos.get(0).get("ID"))
				.isNotEqualTo(foos.get(0).get("NAME"))
				.isEqualTo(payload.get(0).getHeaders().getId().toString());
	}

	@Test
	public void testInsertWithMessagePreparedStatementSetter() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (1, 0, ?)");
		final AtomicBoolean setterInvoked = new AtomicBoolean();
		handler.setPreparedStatementSetter((ps, requestMessage) -> {
			ps.setObject(1, requestMessage.getPayload());
			setterInvoked.set(true);
		});
		handler.afterPropertiesSet();
		Message<String> message = new GenericMessage<>("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertThat(map.get("NAME")).as("Wrong name").isEqualTo("foo");
		assertThat(setterInvoked.get()).isTrue();
	}

	@Test
	public void testInsertBatchWithMessagePreparedStatementSetter() {
		JdbcMessageHandler handler =
				new JdbcMessageHandler(jdbcTemplate, "insert into foos (id, status, name) values (?, 0, ?)");
		handler.setPreparedStatementSetter((ps, requestMessage) -> {
			ps.setObject(1, requestMessage.getPayload());
			ps.setObject(2, requestMessage.getPayload());
		});
		handler.afterPropertiesSet();

		Message<List<String>> message = new GenericMessage<>(Arrays.asList("foo1", "foo2", "foo3"));
		handler.handleMessage(message);

		List<Map<String, Object>> foos = jdbcTemplate.queryForList("SELECT * FROM FOOS ORDER BY id");

		assertThat(foos.size()).isEqualTo(3);

		assertThat(foos.get(0).get("NAME")).isEqualTo("foo1");
		assertThat(foos.get(1).get("NAME")).isEqualTo("foo2");
		assertThat(foos.get(2).get("NAME")).isEqualTo("foo3");
	}

	@Test
	public void testIdHeaderDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (:headers[idAsString], 0, :payload)");
		handler.afterPropertiesSet();
		Message<String> message = new GenericMessage<>("foo");
		String id = message.getHeaders().getId().toString();
		message = MessageBuilder.fromMessage(message)
				.setHeader("idAsString", message.getHeaders().getId().toString())
				.build();
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", id);
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(id);
		assertThat(map.get("NAME")).as("Wrong name").isEqualTo("foo");
	}

	@Test
	public void testDottedHeaderDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate,
				"insert into foos (id, status, name) values (:headers[business.id], 0, :payload)");
		handler.afterPropertiesSet();
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("business.id", "FOO").build();
		handler.handleMessage(message);
		String id = message.getHeaders().get("business.id").toString();
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", id);
		assertThat(map.get("ID")).as("Wrong id").isEqualTo(id);
		assertThat(map.get("NAME")).as("Wrong name").isEqualTo("foo");
	}

}
