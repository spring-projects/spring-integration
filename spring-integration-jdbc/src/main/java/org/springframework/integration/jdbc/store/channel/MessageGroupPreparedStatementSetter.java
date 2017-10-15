/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.store.channel;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.messaging.Message;

/**
 * Callback to be used with {@link JdbcChannelMessageStore}.
 * <p>
 * Behavior is same as standard {@link PreparedStatementSetter}, it takes in
 * extra {@code Message<?> requestMessage} and {@code Object groupId} as
 * parameter used for {@code addMessageToGroup} method in
 * {@link JdbcChannelMessageStore}.
 *
 * @author Meherzad Lahewala
 * @since 5.0
 * @see PreparedStatementSetter
 */
@FunctionalInterface
public interface MessageGroupPreparedStatementSetter {

	void setValues(PreparedStatement ps, Message<?> requestMessage, Object groupId) throws SQLException;

}
