/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jdbc.dsl;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Factory class for JDBC components.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public final class Jdbc {

	/**
	 * The factory to produce a {@link JdbcInboundChannelAdapterSpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @param selectQuery the select query to build on
	 * @return the {@link JdbcInboundChannelAdapterSpec} instance
	 */
	public static JdbcInboundChannelAdapterSpec inboundAdapter(DataSource dataSource, String selectQuery) {
		return inboundAdapter(new JdbcTemplate(dataSource), selectQuery);
	}

	/**
	 * The factory to produce a {@link JdbcInboundChannelAdapterSpec}.
	 * @param jdbcOperations the {@link JdbcOperations} to build on
	 * @param selectQuery the select query to build on
	 * @return the {@link JdbcInboundChannelAdapterSpec} instance
	 */
	public static JdbcInboundChannelAdapterSpec inboundAdapter(JdbcOperations jdbcOperations, String selectQuery) {
		return new JdbcInboundChannelAdapterSpec(jdbcOperations, selectQuery);
	}

	/**
	 * The factory to produce a {@link JdbcOutboundChannelAdapterSpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @param updateQuery the update query to build on
	 * @return the {@link JdbcOutboundChannelAdapterSpec} instance
	 */
	public static JdbcOutboundChannelAdapterSpec outboundAdapter(DataSource dataSource, String updateQuery) {
		return outboundAdapter(new JdbcTemplate(dataSource), updateQuery);
	}

	/**
	 * The factory to produce a {@link JdbcOutboundChannelAdapterSpec}.
	 * @param jdbcOperations the {@link JdbcOperations} to build on
	 * @param updateQuery the update query to build on
	 * @return the {@link JdbcOutboundChannelAdapterSpec} instance
	 */
	public static JdbcOutboundChannelAdapterSpec outboundAdapter(JdbcOperations jdbcOperations, String updateQuery) {
		return new JdbcOutboundChannelAdapterSpec(jdbcOperations, updateQuery);
	}

	/**
	 * The factory to produce a {@link JdbcOutboundGatewaySpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @param updateQuery the update query to build on
	 * @return the {@link JdbcOutboundGatewaySpec} instance
	 */
	public static JdbcOutboundGatewaySpec outboundGateway(DataSource dataSource, String updateQuery) {
		return outboundGateway(new JdbcTemplate(dataSource), updateQuery);
	}

	/**
	 * The factory to produce a {@link JdbcOutboundGatewaySpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @param updateQuery the update query to build on
	 * @param selectQuery the select query to build on
	 * @return the {@link JdbcOutboundGatewaySpec} instance
	 */
	public static JdbcOutboundGatewaySpec outboundGateway(DataSource dataSource, String updateQuery, String selectQuery) {
		return outboundGateway(new JdbcTemplate(dataSource), updateQuery, selectQuery);
	}

	/**
	 * The factory to produce a {@link JdbcOutboundGatewaySpec}.
	 * @param jdbcOperations the {@link JdbcOperations} to build on
	 * @param updateQuery the update query to build on
	 * @return the {@link JdbcOutboundGatewaySpec} instance
	 */
	public static JdbcOutboundGatewaySpec outboundGateway(JdbcOperations jdbcOperations, String updateQuery) {
		return outboundGateway(jdbcOperations, updateQuery, null);
	}

	/**
	 * The factory to produce a {@link JdbcOutboundGatewaySpec}.
	 * @param jdbcOperations the {@link JdbcOperations} to build on
	 * @param updateQuery the update query to build on
	 * @param selectQuery the select query to build on
	 * @return the {@link JdbcOutboundGatewaySpec} instance
	 */
	public static JdbcOutboundGatewaySpec outboundGateway(JdbcOperations jdbcOperations, String updateQuery,
			@Nullable String selectQuery) {

		return new JdbcOutboundGatewaySpec(jdbcOperations, updateQuery, selectQuery);
	}

	/**
	 * The factory to produce a {@link JdbcStoredProcInboundChannelAdapterSpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @return the {@link JdbcStoredProcInboundChannelAdapterSpec} instance
	 */
	public static JdbcStoredProcInboundChannelAdapterSpec storedProcInboundAdapter(DataSource dataSource) {
		return new JdbcStoredProcInboundChannelAdapterSpec(storedProcExecutorSpec(dataSource));
	}

	/**
	 * The factory to produce a {@link JdbcStoredProcInboundChannelAdapterSpec}.
	 * @param storedProcExecutor the {@link StoredProcExecutor} to use
	 * @return the {@link JdbcStoredProcInboundChannelAdapterSpec} instance
	 */
	public static JdbcStoredProcInboundChannelAdapterSpec storedProcInboundAdapter(
			StoredProcExecutor storedProcExecutor) {

		return new JdbcStoredProcInboundChannelAdapterSpec(storedProcExecutor);
	}

	/**
	 * The factory to produce a {@link JdbcStoredProcOutboundChannelAdapterSpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @return the {@link JdbcStoredProcOutboundChannelAdapterSpec} instance
	 */
	public static JdbcStoredProcOutboundChannelAdapterSpec storedProcOutboundAdapter(DataSource dataSource) {
		return new JdbcStoredProcOutboundChannelAdapterSpec(storedProcExecutorSpec(dataSource));
	}

	/**
	 * The factory to produce a {@link JdbcStoredProcOutboundChannelAdapterSpec}.
	 * @param storedProcExecutor the {@link StoredProcExecutor} to use
	 * @return the {@link JdbcStoredProcOutboundChannelAdapterSpec} instance
	 */
	public static JdbcStoredProcOutboundChannelAdapterSpec storedProcOutboundAdapter(
			StoredProcExecutor storedProcExecutor) {

		return new JdbcStoredProcOutboundChannelAdapterSpec(storedProcExecutor);
	}

	/**
	 * The factory to produce a {@link JdbcStoredProcOutboundGatewaySpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @return the {@link JdbcStoredProcOutboundGatewaySpec} instance
	 */
	public static JdbcStoredProcOutboundGatewaySpec storedProcOutboundGateway(DataSource dataSource) {
		return new JdbcStoredProcOutboundGatewaySpec(storedProcExecutorSpec(dataSource));
	}

	/**
	 * The factory to produce a {@link JdbcStoredProcOutboundGatewaySpec}.
	 * @param storedProcExecutor the {@link StoredProcExecutor} to use
	 * @return the {@link JdbcStoredProcOutboundGatewaySpec} instance
	 */
	public static JdbcStoredProcOutboundGatewaySpec storedProcOutboundGateway(StoredProcExecutor storedProcExecutor) {
		return new JdbcStoredProcOutboundGatewaySpec(storedProcExecutor);
	}

	/**
	 * The factory to produce a {@link StoredProcExecutorSpec}.
	 * @param dataSource the {@link DataSource} to build on
	 * @return the {@link StoredProcExecutorSpec} instance
	 */
	public static StoredProcExecutorSpec storedProcExecutorSpec(DataSource dataSource) {
		return new StoredProcExecutorSpec(dataSource);
	}

	private Jdbc() {
	}

}
