/*
 * Copyright 2002-2017 the original author or authors.
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
import static org.mockito.Mockito.mock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
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
 * @author Gary Russell
 * @author Artem Bilan
 */
public class StoredProcMessageHandlerDerbyIntegrationTests {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeClass
	public static void setUp() throws SQLException {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		builder.setType(EmbeddedDatabaseType.DERBY);
		builder.addScript("classpath:derby-stored-procedures.sql");
		embeddedDatabase = builder.build();
		jdbcTemplate = new JdbcTemplate(embeddedDatabase);
	}

	@AfterClass
	public static void tearDown() {
		embeddedDatabase.shutdown();
	}

	@After
	public void cleanup() {
		jdbcTemplate.execute("DELETE FROM USERS");
	}
	@Test
	public void testDerbyStoredProcedureInsertWithDefaultSqlSource() {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertEquals("Wrong username", "username", map.get("USERNAME"));
		assertEquals("Wrong password", "password", map.get("PASSWORD"));
		assertEquals("Wrong email", "email", map.get("EMAIL"));
	}

	@Test
	public void testDerbyStoredProcInsertWithDefaultSqlSourceAndDynamicProcName() throws Exception {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		message.setHeader("stored_procedure_name", "CREATE_USER");
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertEquals("Wrong username", "username", map.get("USERNAME"));
		assertEquals("Wrong password", "password", map.get("PASSWORD"));
		assertEquals("Wrong email", "email", map.get("EMAIL"));
	}

	@Test
	public void testDerbyStoredProcInsertWithDefaultSqlSourceAndSpelProcName() throws Exception {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);
		ExpressionFactoryBean efb = new ExpressionFactoryBean("headers.headerWithProcedureName");
		efb.afterPropertiesSet();

		Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<ProcedureParameter>();
		procedureParameters.add(new ProcedureParameter("username", null, "payload.username.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("password", null, "payload.password.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("email", null, "payload.email.toUpperCase()"));

		storedProcExecutor.setProcedureParameters(procedureParameters);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<ProcedureParameter>();
		procedureParameters.add(new ProcedureParameter("USERNAME", null, "headers[business_id] + '_' + payload.username"));
		procedureParameters.add(new ProcedureParameter("password", "static_password", null));
		procedureParameters.add(new ProcedureParameter("email", "static_email", null));

		storedProcExecutor.setProcedureParameters(procedureParameters);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
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
