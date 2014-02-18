/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Dave Syer
 */
public class JdbcMessageHandlerIntegrationTests {

	private EmbeddedDatabase embeddedDatabase;

	private JdbcTemplate jdbcTemplate;

	@Before
	public void setUp() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.HSQL).addScript(
				"classpath:org/springframework/integration/jdbc/messageHandlerIntegrationTest.sql");
		this.embeddedDatabase = builder.build();
		this.jdbcTemplate = new JdbcTemplate(this.embeddedDatabase);
	}

	@After
	public void tearDown() {
		this.embeddedDatabase.shutdown();
	}

	@Test
	public void testSimpleStaticInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate, "insert into foos (id, status, name) values (1, 0, 'foo')");
		Message<String> message = new GenericMessage<String>("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertEquals("Wrong id", "1", map.get("ID"));
		assertEquals("Wrong status", 0, map.get("STATUS"));
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

	@Test
	public void testSimpleDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate, "insert into foos (id, status, name) values (1, 0, :payload)");
		Message<String> message = new GenericMessage<String>("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

	@Test
	public void testIdHeaderDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate, "insert into foos (id, status, name) values (:headers[idAsString], 0, :payload)");
		Message<String> message = new GenericMessage<String>("foo");
		String id = message.getHeaders().getId().toString();
		message = MessageBuilder.fromMessage(message)
				.setHeader("idAsString", message.getHeaders().getId().toString())
				.build();
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", id);
		assertEquals("Wrong id", id, map.get("ID"));
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

	@Test
	public void testDottedHeaderDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate, "insert into foos (id, status, name) values (:headers[business.id], 0, :payload)");
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("business.id", "FOO").build();
		handler.handleMessage(message);
		String id = message.getHeaders().get("business.id").toString();
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", id);
		assertEquals("Wrong id", id, map.get("ID"));
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

}
