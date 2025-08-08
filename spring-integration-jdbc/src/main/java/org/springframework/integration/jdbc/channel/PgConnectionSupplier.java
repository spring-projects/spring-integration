/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.jdbc.channel;

import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.jdbc.PgConnection;

/**
 * A connection supplier for a {@link PgConnection} to a Postgres database that is
 * to be used for a {@link PostgresChannelMessageTableSubscriber}.
 * <p/>
 * The supplied connection must <b>not</b> be read from a shared connection pool, typically
 * represented by a {@link javax.sql.DataSource}. If a shared connection pool is used, this
 * pool might reclaim a connection that was not closed within a given time frame. This
 * becomes a problem as a {@link PostgresChannelMessageTableSubscriber} requires a dedicated
 * {@link java.sql.Connection} to receive notifications from the Postgres database. This
 * connection needs to remain open over a longer period of time. Typically, a
 * {@link PgConnection} should be created directly via
 * {@link java.sql.Driver#connect(String, Properties)} and a subsequent call
 * to {@link java.sql.Connection#unwrap(Class)}.
 *
 * @author Rafael Winterhalter
 *
 * @since 6.0
 *
 * @see PostgresChannelMessageTableSubscriber
 */
@FunctionalInterface
public interface PgConnectionSupplier {

	/**
	 * Supply an open, un-pooled connection to a Postgres database.
	 * @return A dedicated connection to a Postgres database for listening.
	 * @throws SQLException If the connection could not be established.
	 */
	PgConnection get() throws SQLException;

}
