package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.StringMessage;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * @author Dave Syer
 */
public class JdbcMessageHandlerIntegrationTests {

	private EmbeddedDatabase embeddedDatabase;

	private SimpleJdbcTemplate jdbcTemplate;

	@Before
	public void setUp() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.HSQL).addScript(
				"classpath:org/springframework/integration/jdbc/messageHandlerIntegrationTest.sql");
		this.embeddedDatabase = builder.build();
		this.jdbcTemplate = new SimpleJdbcTemplate(this.embeddedDatabase);
	}

	@After
	public void tearDown() {
		this.embeddedDatabase.shutdown();
	}

	@Test
	public void testSimpleStaticInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate.getJdbcOperations(), "insert into foos (id, status, name) values (1, 0, 'foo')");
		Message<String> message = new StringMessage("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertEquals("Wrong id", "1", map.get("ID"));
		assertEquals("Wrong status", 0, map.get("STATUS"));
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

	@Test
	public void testSimpleDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate.getJdbcOperations(), "insert into foos (id, status, name) values (1, 0, :payload)");
		Message<String> message = new StringMessage("foo");
		handler.handleMessage(message);
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", 1);
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}
	
	@Test
	public void testIdHeaderDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate.getJdbcOperations(), "insert into foos (id, status, name) values (:headers[$id], 0, :payload)");
		Message<String> message = new StringMessage("foo");
		handler.handleMessage(message);
		String id = message.getHeaders().getId().toString();
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", id);
		assertEquals("Wrong id", id, map.get("ID"));
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

	@Test
	public void testDottedHeaderDynamicInsert() {
		JdbcMessageHandler handler = new JdbcMessageHandler(jdbcTemplate.getJdbcOperations(), "insert into foos (id, status, name) values (:headers[business.id], 0, :payload)");
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("business.id", "FOO").build();
		handler.handleMessage(message);
		String id = message.getHeaders().get("business.id").toString();
		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM FOOS WHERE ID=?", id);
		assertEquals("Wrong id", id, map.get("ID"));
		assertEquals("Wrong name", "foo", map.get("NAME"));
	}

}