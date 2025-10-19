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

import org.jspecify.annotations.Nullable;

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.jdbc.MessagePreparedStatementSetter;
import org.springframework.integration.jdbc.SqlParameterSourceFactory;
import org.springframework.integration.jdbc.outbound.JdbcOutboundGateway;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

/**
 * A {@link MessageHandlerSpec} for a {@link JdbcOutboundGatewaySpec}.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class JdbcOutboundGatewaySpec extends MessageHandlerSpec<JdbcOutboundGatewaySpec, JdbcOutboundGateway> {

	protected JdbcOutboundGatewaySpec(JdbcOperations jdbcOperations, String updateQuery, @Nullable String selectQuery) {
		this.target = new JdbcOutboundGateway(jdbcOperations, updateQuery, selectQuery);
	}

	/**
	 * @param maxRows the maxRows
	 * @return the spec
	 * @see JdbcOutboundGateway#setMaxRows(Integer)
	 */
	public JdbcOutboundGatewaySpec maxRows(Integer maxRows) {
		this.target.setMaxRows(maxRows);
		return this;
	}

	/**
	 * @param keysGenerated the keysGenerated
	 * @return the spec
	 * @see JdbcOutboundGateway#setKeysGenerated(boolean)
	 */
	public JdbcOutboundGatewaySpec keysGenerated(boolean keysGenerated) {
		this.target.setKeysGenerated(keysGenerated);
		return this;
	}

	/**
	 * @param sqlParameterSourceFactory the sqlParameterSourceFactory
	 * @return the spec
	 * @see JdbcOutboundGateway#setRequestSqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	public JdbcOutboundGatewaySpec requestSqlParameterSourceFactory(
			SqlParameterSourceFactory sqlParameterSourceFactory) {

		this.target.setRequestSqlParameterSourceFactory(sqlParameterSourceFactory);
		return this;
	}

	/**
	 * @param preparedStatementSetter the preparedStatementSetter
	 * @return the spec
	 * @see JdbcOutboundGateway#setRequestPreparedStatementSetter(MessagePreparedStatementSetter)
	 */
	public JdbcOutboundGatewaySpec requestPreparedStatementSetter(
			MessagePreparedStatementSetter preparedStatementSetter) {

		this.target.setRequestPreparedStatementSetter(preparedStatementSetter);
		return this;
	}

	/**
	 * @param sqlParameterSourceFactory the sqlQueryParameterSource
	 * @return the spec
	 * @see JdbcOutboundGateway#setReplySqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	public JdbcOutboundGatewaySpec replySqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.target.setReplySqlParameterSourceFactory(sqlParameterSourceFactory);
		return this;
	}

	/**
	 * @param rowMapper the rowMapper
	 * @return the spec
	 * @see JdbcOutboundGateway#setRowMapper(RowMapper)
	 */
	public JdbcOutboundGatewaySpec rowMapper(RowMapper<?> rowMapper) {
		this.target.setRowMapper(rowMapper);
		return this;
	}

}
