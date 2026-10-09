/*
 * Copyright 2025-present the original author or authors.
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
