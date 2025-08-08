/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
