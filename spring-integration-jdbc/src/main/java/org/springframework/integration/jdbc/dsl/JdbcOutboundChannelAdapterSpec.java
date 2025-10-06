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

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.jdbc.MessagePreparedStatementSetter;
import org.springframework.integration.jdbc.SqlParameterSourceFactory;
import org.springframework.integration.jdbc.outbound.JdbcMessageHandler;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * A {@link MessageHandlerSpec} for a {@link JdbcOutboundChannelAdapterSpec}.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class JdbcOutboundChannelAdapterSpec
		extends MessageHandlerSpec<JdbcOutboundChannelAdapterSpec, JdbcMessageHandler> {

	protected JdbcOutboundChannelAdapterSpec(JdbcOperations jdbcOperations, String updateQuery) {
		this.target = new JdbcMessageHandler(jdbcOperations, updateQuery);
	}

	/**
	 * @param keysGenerated the keysGenerated
	 * @return the spec
	 * @see JdbcMessageHandler#setKeysGenerated(boolean)
	 */
	public JdbcOutboundChannelAdapterSpec keysGenerated(boolean keysGenerated) {
		this.target.setKeysGenerated(keysGenerated);
		return this;
	}

	/**
	 * @param sqlParameterSourceFactory the sqlParameterSourceFactory
	 * @return the spec
	 * @see JdbcMessageHandler#setSqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	public JdbcOutboundChannelAdapterSpec sqlParameterSourceFactory(
			SqlParameterSourceFactory sqlParameterSourceFactory) {

		this.target.setSqlParameterSourceFactory(sqlParameterSourceFactory);
		return this;
	}

	/**
	 * @param usePayloadAsParameterSource the usePayloadAsParameterSource
	 * @return the spec
	 * @see JdbcMessageHandler#setUsePayloadAsParameterSource(boolean)
	 */
	public JdbcOutboundChannelAdapterSpec usePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
		this.target.setUsePayloadAsParameterSource(usePayloadAsParameterSource);
		return this;
	}

	/**
	 * @param preparedStatementSetter the preparedStatementSetter
	 * @return the spec
	 * @see JdbcMessageHandler#setPreparedStatementSetter(MessagePreparedStatementSetter)
	 */
	public JdbcOutboundChannelAdapterSpec preparedStatementSetter(
			MessagePreparedStatementSetter preparedStatementSetter) {

		this.target.setPreparedStatementSetter(preparedStatementSetter);
		return this;
	}

}
