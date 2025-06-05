/*
 * Copyright 2016-2025 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
import org.springframework.integration.jdbc.config.JdbcTypesEnum;
import org.springframework.integration.jdbc.storedproc.ClobSqlReturnType;
import org.springframework.integration.jdbc.storedproc.PrimeMapper;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnType;
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
import static org.mockito.Mockito.mock;

/**
 * @author Jiandong Ma
 *
 * @since 7.0
 */
@SpringJUnitConfig
@DirtiesContext
class JdbcTest {

	@Autowired
	private JdbcTemplate h2JdbcTemplate;

	@Autowired
	private JdbcTemplate derbyJdbcTemplate;

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

	@Autowired
	private SqlReturnType clobSqlReturnType;

	@Test
	void testInboundFlow() {
		Message<?> message = this.inboundFlowPollerChannel.receive(10_000);
		List<?> rows = (List<?>) message.getPayload();
		assertThat(rows.size()).isEqualTo(2);
		assertThat(rows.get(0) instanceof Inbound).isTrue();
		Inbound item = (Inbound) rows.get(0);
		assertThat(item.id()).isEqualTo(1);
		assertThat(item.status()).isEqualTo(2);

		Integer countOfStatusTwo = h2JdbcTemplate.queryForObject("select count(*) from inbound where status = 2", Integer.class);
		assertThat(countOfStatusTwo).isEqualTo(0);

		Integer countOfStatusTen = h2JdbcTemplate.queryForObject("select count(*) from inbound where status = 10", Integer.class);
		assertThat(countOfStatusTen).isEqualTo(2);
	}

	record Inbound(int id, int status) {
	}

	@Test
	void testOutboundFlow() {
		outboundFlowInputChannel.send(new GenericMessage<>("foo"));
		Map<String, Object> map = h2JdbcTemplate.queryForMap("select * from outbound where id=?", 1);
		assertThat(map.get("name")).isEqualTo("foo");
	}

	@Test
	void testOutboundGateway() {
		outboundGatewayInputChannel.send(new GenericMessage<>(10));
		Message<?> message = outboundGatewayReplyChannel.receive(10_000);
		assertThat(message).isNotNull();
		List<?> payload = (List<?>) message.getPayload();
		assertThat(payload).hasSize(1);
		Object item = payload.get(0);
		assertThat(item).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) item).get("status")).isEqualTo(10);
	}

	@Test
	void testOutboundGatewayNoSelectQuery() {
		outboundGatewayNoSelectQueryInputChannel.send(new GenericMessage<>(10));
		Message<?> message = outboundGatewayNoSelectQueryReplyChannel.receive(10_000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) payload).get("UPDATED")).isEqualTo(1);
	}

	@Test
	void testStoredProcInboundFlow() {
		Message<?> message = this.storedProcInboundPollerChannel.receive(10_000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload).isInstanceOf(List.class);

		List<Integer> primeNumbers = (List<Integer>) payload;

		assertThat(primeNumbers.size() == 4).isTrue();
	}

	@Test
	void testStoredProcOutboundFlow() {
		storedProcOutboundFlowInputChannel.send(MessageBuilder.withPayload(new User("username", "password", "email")).build());
		Map<String, Object> map = this.derbyJdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");
		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
	}

	@Test
	@Transactional(transactionManager = "derbyTransactionManager")
	void testStoredProcOutboundGateway() throws SQLException {
		Mockito.reset(this.clobSqlReturnType);
		Message<String> testMessage = MessageBuilder.withPayload("TEST").setHeader("FOO", "BAR").build();
		String messageId = testMessage.getHeaders().getId().toString();
		String jsonMessage = new JsonOutboundMessageMapper().fromMessage(testMessage);
		this.derbyJdbcTemplate.update("INSERT INTO json_message VALUES (?,?)", messageId, jsonMessage);

		this.storedProcOutboundGatewayInputChannel.send(new GenericMessage<>(messageId));
		Message<?> resultMessage = this.storedProcOutboundGatewayReplyChannel.receive(10_000);

		assertThat(resultMessage).isNotNull();
		Object resultPayload = resultMessage.getPayload();
		assertThat(resultPayload instanceof String).isTrue();
		Message<?> message = new JsonInboundMessageMapper(String.class, new Jackson2JsonMessageParser())
				.toMessage((String) resultPayload);
		assertThat(message.getPayload()).isEqualTo(testMessage.getPayload());
		assertThat(message.getHeaders().get("FOO")).isEqualTo(testMessage.getHeaders().get("FOO"));
		Mockito.verify(clobSqlReturnType).getTypeValue(Mockito.any(CallableStatement.class),
				Mockito.eq(2), Mockito.eq(JdbcTypesEnum.CLOB.getCode()), Mockito.eq(null));

	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public IntegrationFlow inboundFlow(DataSource h2DataSource) {
			var sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();
			sqlParameterSourceFactory.setBeanFactory(mock());
			return IntegrationFlow.from(Jdbc.inboundAdapter(h2DataSource, "select * from inbound")
									.maxRows(2)
									.rowMapper((RowMapper<Inbound>) (rs, rowNum) -> new Inbound(rs.getInt(1), rs.getInt(2)))
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
					.handle(Jdbc.outboundAdapter(h2DataSource, "insert into outbound (id, status, name) values (1, 0, ?)")
							.preparedStatementSetter((ps, requestMessage) -> {
								ps.setObject(1, requestMessage.getPayload());
							})
							.usePayloadAsParameterSource(false)
							.sqlParameterSourceFactory(null)
							.keysGenerated(false)
					);
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
											.procedureParameters(List.of(
													new ProcedureParameter("beginRange", 1, null),
													new ProcedureParameter("endRange", 10, null)
											))
											.sqlParameters(List.of(
													new SqlParameter("beginRange", Types.INTEGER),
													new SqlParameter("endRange", Types.INTEGER)
											))
											.returningResultSetRowMappers(Map.of("out", new PrimeMapper()))
									),
							e -> e.poller(p -> p.trigger(new OnlyOnceTrigger())))
					.channel(c -> c.queue("storedProcInboundPollerChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow storedProcOutboundAdapter(DataSource derbyDataSource) {
			return flow -> flow
					.handle(Jdbc.storedProcOutboundAdapter(derbyDataSource)
							.configurerStoredProcExecutor(configurer -> configurer
									.storedProcedureName("CREATE_USER")
									.sqlParameterSourceFactory(new BeanPropertySqlParameterSourceFactory())
									.usePayloadAsParameterSource(true)
							)
					);
		}

		@Bean
		public IntegrationFlow storedProcOutboundGateway(DataSource derbyDataSource) throws Exception {

			return flow -> flow
					.handle(Jdbc.storedProcOutboundGateway(derbyDataSource)
							.requiresReply(true)
							.expectSingleResult(true)
							.configurerStoredProcExecutor(configurer -> configurer
									.storedProcedureNameExpression(new ValueExpression<>("GET_MESSAGE"))
									.ignoreColumnMetaData(false)
									.isFunction(false)
									.procedureParameters(List.of(
											new ProcedureParameter("message_id", null, "payload")
									))
									.sqlParameters(List.of(
											new SqlParameter("message_id", Types.VARCHAR),
											new SqlOutParameter("message_json", Types.CLOB, null, clobSqlReturnType())
									))
									.returnValueRequired(false)
									.skipUndeclaredResults(true)
									.jdbcCallOperationsCacheSize(10)
							))
					.channel(c -> c.queue("storedProcOutboundGatewayReplyChannel"));
		}

		@Bean
		public ClobSqlReturnType clobSqlReturnType() {
			return Mockito.spy(new ClobSqlReturnType());
		}

		@Bean
		public DataSource h2DataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.addScripts("classpath:dsl-h2.sql", "classpath:h2-stored-procedures.sql")
					.build();
		}

		@Bean
		public DataSource derbyDataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.DERBY)
					.addScripts("classpath:derby-stored-procedures.sql")
					.build();
		}

		@Bean
		public JdbcTemplate h2JdbcTemplate(DataSource h2DataSource) {
			return new JdbcTemplate(h2DataSource);
		}

		@Bean
		public JdbcTemplate derbyJdbcTemplate(DataSource derbyDataSource) {
			return new JdbcTemplate(derbyDataSource);
		}

		@Bean
		public PlatformTransactionManager h2TransactionManager() {
			return new DataSourceTransactionManager(h2DataSource());
		}

		@Bean
		public PlatformTransactionManager derbyTransactionManager() {
			return new DataSourceTransactionManager(derbyDataSource());
		}
	}

}
