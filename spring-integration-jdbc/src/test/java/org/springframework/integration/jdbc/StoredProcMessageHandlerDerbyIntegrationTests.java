/*
 * Copyright 2002-2012 the original author or authors.
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * @author Gunnar Hillert
 */
public class StoredProcMessageHandlerDerbyIntegrationTests {

	private EmbeddedDatabase embeddedDatabase;

	private JdbcTemplate jdbcTemplate;

	@Before
	public void setUp() throws SQLException {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.DERBY);
		builder.addScript("classpath:derby-stored-procedures.sql");
		this.embeddedDatabase = builder.build();
		this.jdbcTemplate = new JdbcTemplate(this.embeddedDatabase);
	}

	@After
	public void tearDown() {
		this.embeddedDatabase.shutdown();
	}

	@Test
	public void testDerbyStoredProcedureInsertWithDefaultSqlSource() {

		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(this.embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		storedProcExecutor.afterPropertiesSet();
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertEquals("Wrong username", "username", map.get("USERNAME"));
		assertEquals("Wrong password", "password", map.get("PASSWORD"));
		assertEquals("Wrong email", "email", map.get("EMAIL"));

	}

	@Test
	public void testDerbyStoredProcInsertWithDefaultSqlSourceAndDynamicProcName() {

		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(this.embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);
		storedProcExecutor.setAllowDynamicStoredProcedureNames(true);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		message.setHeader(JdbcHeaders.STORED_PROCEDURE_NAME, "CREATE_USER");
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertEquals("Wrong username", "username", map.get("USERNAME"));
		assertEquals("Wrong password", "password", map.get("PASSWORD"));
		assertEquals("Wrong email", "email", map.get("EMAIL"));

	}

	@Test
	public void testDerbyStoredProcInsertWithDefaultSqlSourceAndSpelProcName() throws Exception {

		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(this.embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);
		ExpressionFactoryBean efb = new ExpressionFactoryBean("headers.headerWithProcedureName");
		efb.afterPropertiesSet();

		Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		message.setHeader("headerWithProcedureName", "CREATE_USER");
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertEquals("Wrong username", "username", map.get("USERNAME"));
		assertEquals("Wrong password", "password", map.get("PASSWORD"));
		assertEquals("Wrong email", "email", map.get("EMAIL"));

	}

	@Test
	public void testDerbyStoredProcedureInsertWithExpression() {

		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(this.embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<ProcedureParameter>();
		procedureParameters.add(new ProcedureParameter("username", null, "payload.username.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("password", null, "payload.password.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("email",    null, "payload.email.toUpperCase()"));

		storedProcExecutor.setProcedureParameters(procedureParameters);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("Eric.Cartman", "c4rtm4n", "eric@cartman.com"));
		messageHandler.handleMessage(message.build());



		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "ERIC.CARTMAN");

		assertEquals("Wrong username", "ERIC.CARTMAN", map.get("USERNAME"));
		assertEquals("Wrong password", "C4RTM4N", map.get("PASSWORD"));
		assertEquals("Wrong email", "ERIC@CARTMAN.COM", map.get("EMAIL"));

	}

	@Test
	public void testDerbyStoredProcedureInsertWithHeaderExpression() {

		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(this.embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<ProcedureParameter>();
		procedureParameters.add(new ProcedureParameter("USERNAME", null, "headers[business_id] + '_' + payload.username"));
		procedureParameters.add(new ProcedureParameter("password", "static_password", null));
		procedureParameters.add(new ProcedureParameter("email",    "static_email"   , null));

		storedProcExecutor.setProcedureParameters(procedureParameters);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("Eric.Cartman", "c4rtm4n", "eric@cartman.com"));
		message.setHeader("business_id", "1234");
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "1234_Eric.Cartman");

		assertEquals("Wrong username", "1234_Eric.Cartman", map.get("USERNAME"));
		assertEquals("Wrong password", "static_password", map.get("PASSWORD"));
		assertEquals("Wrong email", "static_email", map.get("EMAIL"));
	}

}
