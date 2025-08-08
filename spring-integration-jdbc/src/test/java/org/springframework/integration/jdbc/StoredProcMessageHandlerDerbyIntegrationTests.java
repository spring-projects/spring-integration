/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
public class StoredProcMessageHandlerDerbyIntegrationTests {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeAll
	public static void setUp() {
		embeddedDatabase =
				new EmbeddedDatabaseBuilder()
						.setType(EmbeddedDatabaseType.DERBY)
						.addScript("classpath:derby-stored-procedures.sql")
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

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
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

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
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

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
	}

	@Test
	public void testDerbyStoredProcedureInsertWithExpression() {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<>();
		procedureParameters.add(new ProcedureParameter("username", null, "payload.username.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("password", null, "payload.password.toUpperCase()"));
		procedureParameters.add(new ProcedureParameter("email", null, "payload.email.toUpperCase()"));

		storedProcExecutor.setProcedureParameters(procedureParameters);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
		messageHandler.afterPropertiesSet();

		Message<User> message = new GenericMessage<>(new User("Eric.Cartman", "c4rtm4n", "eric@cartman.com"));
		messageHandler.handleMessage(message);

		Map<String, Object> map = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "ERIC.CARTMAN");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("ERIC.CARTMAN");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("C4RTM4N");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("ERIC@CARTMAN.COM");
	}

	@Test
	public void testDerbyStoredProcedureInsertWithHeaderExpression() {
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(embeddedDatabase);
		StoredProcMessageHandler messageHandler = new StoredProcMessageHandler(storedProcExecutor);

		storedProcExecutor.setStoredProcedureName("CREATE_USER");

		final List<ProcedureParameter> procedureParameters = new ArrayList<>();
		procedureParameters.add(new ProcedureParameter("USERNAME", null,
				"headers[business_id] + '_' + payload.username"));
		procedureParameters.add(new ProcedureParameter("password", "static_password", null));
		procedureParameters.add(new ProcedureParameter("email", "static_email", null));

		storedProcExecutor.setProcedureParameters(procedureParameters);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();
		messageHandler.setBeanFactory(mock(BeanFactory.class));
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

}
