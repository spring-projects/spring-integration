/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.jdbc.dsl;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.jdbc.BeanPropertySqlParameterSourceFactory;
import org.springframework.integration.jdbc.ExpressionEvaluatingSqlParameterSourceFactory;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.storedproc.PrimeMapper;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonJsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
@SpringJUnitConfig
@DirtiesContext
class JdbcTests {

	@Autowired
	private JdbcTemplate h2JdbcTemplate;

	@Autowired
	private QueueChannel inboundFlowPollerChannel;

	@Autowired
	@Qualifier("outboundFlow.input")
	private MessageChannel outboundFlowInputChannel;

	@Autowired
	@Qualifier("outboundGateway.input")
	private MessageChannel outboundGatewayInputChannel;

	@Autowired
	private QueueChannel outboundGatewayReplyChannel;

	@Autowired
	@Qualifier("outboundGatewayNoSelectQuery.input")
	private MessageChannel outboundGatewayNoSelectQueryInputChannel;

	@Autowired
	private QueueChannel outboundGatewayNoSelectQueryReplyChannel;

	@Autowired
	private QueueChannel storedProcInboundPollerChannel;

	@Autowired
	@Qualifier("storedProcOutboundAdapter.input")
	private MessageChannel storedProcOutboundFlowInputChannel;

	@Autowired
	@Qualifier("storedProcOutboundGateway.input")
	private MessageChannel storedProcOutboundGatewayInputChannel;

	@Autowired
	private QueueChannel storedProcOutboundGatewayReplyChannel;

	@Test
	void testInboundFlow() {
		Message<?> message = this.inboundFlowPollerChannel.receive(10_000);
		List<?> rows = (List<?>) message.getPayload();
		assertThat(rows).hasSize(2);
		assertThat(rows.get(0))
				.asInstanceOf(InstanceOfAssertFactories.type(Inbound.class))
				.hasFieldOrPropertyWithValue("id", 1)
				.hasFieldOrPropertyWithValue("status", 2);

		Integer countOfStatusTwo =
				h2JdbcTemplate.queryForObject("select count(*) from inbound where status = 2", Integer.class);
		assertThat(countOfStatusTwo).isEqualTo(0);

		Integer countOfStatusTen =
				h2JdbcTemplate.queryForObject("select count(*) from inbound where status = 10", Integer.class);
		assertThat(countOfStatusTen).isEqualTo(2);
	}

	@Test
	void testOutboundFlow() {
		outboundFlowInputChannel.send(new GenericMessage<>("foo"));
		Map<String, Object> map = h2JdbcTemplate.queryForMap("select * from outbound where id=?", 1);
		assertThat(map).containsEntry("name", "foo");
	}

	@Test
	void testOutboundGateway() {
		outboundGatewayInputChannel.send(new GenericMessage<>(10));
		Message<?> message = outboundGatewayReplyChannel.receive(10_000);
		assertThat(message).isNotNull();
		List<?> payload = (List<?>) message.getPayload();
		assertThat(payload).hasSize(1);
		Object item = payload.get(0);
		assertThat(item)
				.asInstanceOf(InstanceOfAssertFactories.map(String.class, Integer.class))
				.containsEntry("status", 10);
	}

	@Test
	void testOutboundGatewayNoSelectQuery() {
		outboundGatewayNoSelectQueryInputChannel.send(new GenericMessage<>(10));
		Message<?> message = outboundGatewayNoSelectQueryReplyChannel.receive(10_000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload)
				.asInstanceOf(InstanceOfAssertFactories.map(String.class, Integer.class))
				.containsEntry("UPDATED", 1);
	}

	@Test
	void testStoredProcInboundFlow() {
		Message<?> message = this.storedProcInboundPollerChannel.receive(10_000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload).asInstanceOf(InstanceOfAssertFactories.list(Integer.class)).hasSize(4);
	}

	@Test
	void testStoredProcOutboundFlow() {
		storedProcOutboundFlowInputChannel.send(new GenericMessage<>(new User("username", "password", "email")));
		Map<String, Object> map = this.h2JdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");
		assertThat(map)
				.containsEntry("USERNAME", "username")
				.containsEntry("PASSWORD", "password")
				.containsEntry("EMAIL", "email");
	}

	@Test
	@Transactional(transactionManager = "h2TransactionManager")
	void testStoredProcOutboundGateway() throws SQLException {
		Message<String> testMessage = MessageBuilder.withPayload("TEST").setHeader("FOO", "BAR").build();
		String messageId = testMessage.getHeaders().getId().toString();
		String jsonMessage = new JsonOutboundMessageMapper().fromMessage(testMessage);
		this.h2JdbcTemplate.update("INSERT INTO json_message VALUES (?,?)", messageId, jsonMessage);

		this.storedProcOutboundGatewayInputChannel.send(new GenericMessage<>(messageId));
		Message<?> resultMessage = this.storedProcOutboundGatewayReplyChannel.receive(10_000);

		assertThat(resultMessage).isNotNull();
		Object resultPayload = resultMessage.getPayload();
		if (resultPayload instanceof List<?> resultList) {
			assertThat(resultList).hasSize(1);
			resultPayload = resultList.get(0);
		}
		assertThat(resultPayload).isInstanceOf(String.class);
		Message<?> message = new JsonInboundMessageMapper(String.class, new JacksonJsonMessageParser())
				.toMessage((String) resultPayload);
		assertThat(message.getPayload()).isEqualTo(testMessage.getPayload());
		assertThat(message.getHeaders().get("FOO")).isEqualTo(testMessage.getHeaders().get("FOO"));
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public IntegrationFlow inboundFlow(DataSource h2DataSource, BeanFactory beanFactory) {
			var sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();
			sqlParameterSourceFactory.setBeanFactory(beanFactory);
			return IntegrationFlow.from(Jdbc.inboundAdapter(h2DataSource, "select * from inbound")
									.maxRows(2)
									.rowMapper((rs, rowNum) -> new Inbound(rs.getInt(1), rs.getInt(2)))
									.updateSql("update inbound set status = 10 where id in (:id)")
									.updatePerRow(false)
									.updateSqlParameterSourceFactory(sqlParameterSourceFactory)
									.selectSqlParameterSource(null),
							e -> e.poller(p -> p.trigger(new OnlyOnceTrigger())))
					.channel(c -> c.queue("inboundFlowPollerChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow outboundFlow(DataSource h2DataSource) {
			return flow -> flow
					.handle(Jdbc.outboundAdapter(h2DataSource,
									"insert into outbound (id, status, name) values (1, 0, ?)")
							.preparedStatementSetter((ps, requestMessage) ->
									ps.setObject(1, requestMessage.getPayload()))
							.usePayloadAsParameterSource(false)
							.keysGenerated(false));
		}

		@Bean
		public IntegrationFlow outboundGateway(DataSource h2DataSource) {
			return flow -> flow
					.handle(Jdbc.outboundGateway(h2DataSource,
									"update outbound_gateway set status = :payload where id = 1",
									"select * from outbound_gateway where id = 1")
							.keysGenerated(false)
							.requestSqlParameterSourceFactory(new BeanPropertySqlParameterSourceFactory())
							.requestPreparedStatementSetter(null)
							.replySqlParameterSourceFactory(new ExpressionEvaluatingSqlParameterSourceFactory())
							.rowMapper(null)
							.maxRows(0)
					)
					.channel(c -> c.queue("outboundGatewayReplyChannel"));
		}

		@Bean
		public IntegrationFlow outboundGatewayNoSelectQuery(DataSource h2DataSource) {
			return flow -> flow
					.handle(Jdbc.outboundGateway(h2DataSource,
							"update outbound_gateway set status = :payload where id = 2")
					)
					.channel(c -> c.queue("outboundGatewayNoSelectQueryReplyChannel"));
		}

		@Bean
		public IntegrationFlow storedProcInboundFlow(DataSource h2DataSource) {
			return IntegrationFlow.from(Jdbc.storedProcInboundAdapter(h2DataSource)
									.expectSingleResult(true)
									.configurerStoredProcExecutor(configurer -> configurer
											.ignoreColumnMetaData(true)
											.isFunction(false)
											.storedProcedureName("GET_PRIME_NUMBERS")
											.procedureParameter(new ProcedureParameter("beginRange", 1, null))
											.procedureParameter(new ProcedureParameter("endRange", 10, null))
											.sqlParameter(new SqlParameter("beginRange", Types.INTEGER))
											.sqlParameter(new SqlParameter("endRange", Types.INTEGER))
											.returningResultSetRowMapper("out", new PrimeMapper())
									),
							e -> e.poller(p -> p.trigger(new OnlyOnceTrigger())))
					.channel(c -> c.queue("storedProcInboundPollerChannel"))
					.get();
		}

		@Bean
		StoredProcExecutorSpec storedProcExecutor(DataSource h2DataSource) {
			return Jdbc.storedProcExecutorSpec(h2DataSource)
					.ignoreColumnMetaData(true)
					.storedProcedureName("CREATE_USER")
					.sqlParameter(new SqlParameter("username", Types.VARCHAR))
					.sqlParameter(new SqlParameter("password", Types.VARCHAR))
					.sqlParameter(new SqlParameter("email", Types.VARCHAR))
					.sqlParameterSourceFactory(new BeanPropertySqlParameterSourceFactory())
					.usePayloadAsParameterSource(true);
		}

		@Bean
		public IntegrationFlow storedProcOutboundAdapter(StoredProcExecutor storedProcExecutor) {
			return flow -> flow
					.handle(Jdbc.storedProcOutboundAdapter(storedProcExecutor));
		}

		@Bean
		public IntegrationFlow storedProcOutboundGateway(DataSource h2DataSource) {

			return flow -> flow
					.handle(Jdbc.storedProcOutboundGateway(h2DataSource)
							.requiresReply(true)
							.expectSingleResult(true)
							.configurerStoredProcExecutor(configurer -> configurer
									.storedProcedureNameExpression(new ValueExpression<>("GET_MESSAGE"))
									.ignoreColumnMetaData(true)
									.isFunction(false)
									.procedureParameters(List.of(
											new ProcedureParameter("message_id", null, "payload")
									))
									.sqlParameters(List.of(
											new SqlParameter("message_id", Types.VARCHAR)
									))
									.returningResultSetRowMapper("out", new SingleColumnRowMapper<>(String.class))
									.returnValueRequired(false)
									.skipUndeclaredResults(true)
									.jdbcCallOperationsCacheSize(10)
							))
					.channel(c -> c.queue("storedProcOutboundGatewayReplyChannel"));
		}

		@Bean
		public DataSource h2DataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.addScripts("classpath:dsl-h2.sql", "classpath:h2-stored-procedures.sql")
					.build();
		}

		@Bean
		public JdbcTemplate h2JdbcTemplate(DataSource h2DataSource) {
			return new JdbcTemplate(h2DataSource);
		}

		@Bean
		public PlatformTransactionManager h2TransactionManager() {
			return new DataSourceTransactionManager(h2DataSource());
		}

	}

	record Inbound(int id, int status) {

	}

}
