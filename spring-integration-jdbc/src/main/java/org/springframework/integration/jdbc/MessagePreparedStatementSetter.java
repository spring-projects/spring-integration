/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.messaging.Message;

/**
 * The callback to be used with the {@link JdbcMessageHandler}
 * as an alternative to the {@link SqlParameterSourceFactory}.
 * <p>
 * Plays the same role as standard
 * {@link org.springframework.jdbc.core.PreparedStatementSetter},
 * but with {@code Message<?> requestMessage} context during {@code handleMessage}
 * process in the {@link JdbcMessageHandler}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 *
 * @see org.springframework.jdbc.core.PreparedStatementSetter
 */
@FunctionalInterface
public interface MessagePreparedStatementSetter {

	/**
	 * Set parameter values on the given {@link PreparedStatement} and message context.
	 * @param ps the {@link PreparedStatement} to set value.
	 * @param requestMessage the message as a context for values.
	 * @throws SQLException if an SQLException is encountered
	 */
	void setValues(PreparedStatement ps, Message<?> requestMessage) throws SQLException;

}
