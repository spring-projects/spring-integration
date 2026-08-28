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

package org.springframework.integration.jdbc.outbound;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Pratap Chandra Deo
 */
public class StoredProcMessageHandlerH2IntegrationTests implements TestApplicationContextAware {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeAll
	public static void setUp() {
		embeddedDatabase =
				new EmbeddedDatabaseBuilder()
						.setType(EmbeddedDatabaseType.H2)
						.addScript("classpath:h2-stored-procedures.sql")
						.build();
		jdbcTemplate = new JdbcTemplate(embeddedDatabase);
	}

	@AfterAll
	public static void tearDown() {
		embeddedDatabase.shutdown();
	}

	@AfterEach
	public void cleanup() {
		jdbcTemplate.execute("DELETE FROM USERS");
	}

	@Test
	public void testStoredProcedureInsertWithDefaultSqlSource() {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");
		configureCreateUserCall(storedProcExecutor);
		storedProcExecutor.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
	}

	@Test
	public void testStoredProcInsertWithDefaultSqlSourceAndDynamicProcName() throws Exception {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		configureCreateUserCall(storedProcExecutor);
		storedProcExecutor.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		message.setHeader("stored_procedure_name", "CREATE_USER");
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
	}

	@Test
	public void testStoredProcInsertWithDefaultSqlSourceAndSpelProcName() throws Exception {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);
		ExpressionFactoryBean efb = new ExpressionFactoryBean("headers.headerWithProcedureName");
		efb.afterPropertiesSet();

		Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		configureCreateUserCall(storedProcExecutor);
		storedProcExecutor.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageHandler.afterPropertiesSet();

		MessageBuilder<User> message = MessageBuilder.withPayload(new User("username", "password", "email"));
		message.setHeader("headerWithProcedureName", "CREATE_USER");
		messageHandler.handleMessage(message.build());

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
	}

	@Test
	public void testStoredProcedureInsertWithExpression() {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<>();
		procedureParameters.add(new ProcedureParameter("username", null, "payload.username.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("password", null, "payload.password.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("email", null, "payload.email.toUpperCase()"));

		storedProcExecutor.setProcedureParameters(procedureParameters);
		configureCreateUserCall(storedProcExecutor);
		storedProcExecutor.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageHandler.afterPropertiesSet();

		Message<User> message = new GenericMessage<>(new User("Eric.Cartman", "c4rtm4n", "eric@cartman.com"));
		messageHandler.handleMessage(message);

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "ERIC.CARTMAN");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("ERIC.CARTMAN");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("C4RTM4N");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("ERIC@CARTMAN.COM");
	}

	@Test
	public void testStoredProcedureInsertWithHeaderExpression() {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<>();
		procedureParameters.add(new ProcedureParameter("username", null,
				"headers[business_id] + '_' + payload.username"));
		procedureParameters.add(new ProcedureParameter("password", "static_password", null));
		procedureParameters.add(new ProcedureParameter("email", "static_email", null));

		storedProcExecutor.setProcedureParameters(procedureParameters);
		configureCreateUserCall(storedProcExecutor);
		storedProcExecutor.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageHandler.afterPropertiesSet();

		Message<User> message =
				MessageBuilder.withPayload(new User("Eric.Cartman", "c4rtm4n", "eric@cartman.com"))
						.setHeader("business_id", "1234")
						.build();
		messageHandler.handleMessage(message);

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "1234_Eric.Cartman");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("1234_Eric.Cartman");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("static_password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("static_email");
	}

	private static void configureCreateUserCall(StoredProcExecutor storedProcExecutor) {
		storedProcExecutor.setIgnoreColumnMetaData(true);
		storedProcExecutor.setSqlParameters(List.of(
				new SqlParameter("username", Types.VARCHAR),
				new SqlParameter("password", Types.VARCHAR),
				new SqlParameter("email", Types.VARCHAR)));
	}

}
