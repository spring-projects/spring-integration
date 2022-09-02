/*
 * Copyright 2022 the original author or authors.
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
