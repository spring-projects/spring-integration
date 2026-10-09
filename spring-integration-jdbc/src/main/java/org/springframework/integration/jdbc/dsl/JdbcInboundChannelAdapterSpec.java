/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jdbc.dsl;

import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.jdbc.SqlParameterSourceFactory;
import org.springframework.integration.jdbc.inbound.JdbcPollingChannelAdapter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * A {@link MessageSourceSpec} for a {@link JdbcInboundChannelAdapterSpec}.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class JdbcInboundChannelAdapterSpec
		extends MessageSourceSpec<JdbcInboundChannelAdapterSpec, JdbcPollingChannelAdapter> {

	protected JdbcInboundChannelAdapterSpec(JdbcOperations jdbcOperations, String selectQuery) {
		this.target = new JdbcPollingChannelAdapter(jdbcOperations, selectQuery);
	}

	/**
	 * @param rowMapper the rowMapper
	 * @return the spec
	 * @see JdbcPollingChannelAdapter#setRowMapper(RowMapper)
	 */
	public JdbcInboundChannelAdapterSpec rowMapper(RowMapper<?> rowMapper) {
		this.target.setRowMapper(rowMapper);
		return this;
	}

	/**
	 * @param updateSql the updateSql
	 * @return the spec
	 * @see JdbcPollingChannelAdapter#setUpdateSql(String)
	 */
	public JdbcInboundChannelAdapterSpec updateSql(String updateSql) {
		this.target.setUpdateSql(updateSql);
		return this;
	}

	/**
	 * @param updatePerRow the updatePerRow
	 * @return the spec
	 * @see JdbcPollingChannelAdapter#setUpdatePerRow(boolean)
	 */
	public JdbcInboundChannelAdapterSpec updatePerRow(boolean updatePerRow) {
		this.target.setUpdatePerRow(updatePerRow);
		return this;
	}

	/**
	 * @param sqlParameterSourceFactory the sqlParameterSourceFactory
	 * @return the spec
	 * @see JdbcPollingChannelAdapter#setUpdateSqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	public JdbcInboundChannelAdapterSpec updateSqlParameterSourceFactory(
			SqlParameterSourceFactory sqlParameterSourceFactory) {

		this.target.setUpdateSqlParameterSourceFactory(sqlParameterSourceFactory);
		return this;
	}

	/**
	 * @param sqlQueryParameterSource the sqlQueryParameterSource
	 * @return the spec
	 * @see JdbcPollingChannelAdapter#setSelectSqlParameterSource(SqlParameterSource)
	 */
	public JdbcInboundChannelAdapterSpec selectSqlParameterSource(SqlParameterSource sqlQueryParameterSource) {
		this.target.setSelectSqlParameterSource(sqlQueryParameterSource);
		return this;
	}

	/**
	 * @param maxRows the maxRows
	 * @return the spec
	 * @see JdbcPollingChannelAdapter#setMaxRows(int)
	 */
	public JdbcInboundChannelAdapterSpec maxRows(int maxRows) {
		this.target.setMaxRows(maxRows);
		return this;
	}

}
