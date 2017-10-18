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
import java.sql.Types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.messaging.Message;

/**
 * Callback to be used with {@link JdbcChannelMessageStore}.
 * <p>
 * Behavior is same as standard {@link PreparedStatementSetter}, it takes in
 * extra {@code Message<?> requestMessage}, {@code Object groupId}, {@code String region}
 * and {@code boolean priorityEnabled} as
 * parameters used for {@code addMessageToGroup} method in {@link JdbcChannelMessageStore}.
 * Implementors of this class just can use the default constructor
 * {@link #MessageGroupPreparedStatementSetter()} if default implementation of
 * {@code SerializingConverter} and {@code LobHandler} is required.
 *
 * @author Meherzad Lahewala
 * @since 5.0
 * @see PreparedStatementSetter
 */
public class MessageGroupPreparedStatementSetter {

	private static final Log logger = LogFactory.getLog(MessageGroupPreparedStatementSetter.class);

	private final SerializingConverter serializer;

	private final LobHandler lobHandler;

	public MessageGroupPreparedStatementSetter(SerializingConverter serializer, LobHandler lobHandler) {
		this.serializer = serializer;
		this.lobHandler = lobHandler;
	}

	/**
	 * Default Constructor using default serializer as
	 * {@link SerializingConverter} and lobHandler as {@link DefaultLobHandler}.
	 */
	protected MessageGroupPreparedStatementSetter() {
		this.serializer = new SerializingConverter();
		this.lobHandler = new DefaultLobHandler();
	}

	public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage, Object groupId, String region,
			boolean priorityEnabled) throws SQLException {
		String groupKey = getKey(groupId);
		long createdDate = System.currentTimeMillis();
		String messageId = getKey(requestMessage.getHeaders().getId());
		byte[] messageBytes = this.serializer.convert(requestMessage);

		if (logger.isDebugEnabled()) {
			logger.debug("Inserting message with id key=" + messageId);
		}

		preparedStatement.setString(1, messageId);
		preparedStatement.setString(2, groupKey);
		preparedStatement.setString(3, region);
		preparedStatement.setLong(4, createdDate);

		Integer priority = requestMessage.getHeaders().get(IntegrationMessageHeaderAccessor.PRIORITY, Integer.class);

		if (priorityEnabled && priority != null) {
			preparedStatement.setInt(5, priority);
		}
		else {
			preparedStatement.setNull(5, Types.NUMERIC);
		}
		this.lobHandler.getLobCreator().setBlobAsBytes(preparedStatement, 6, messageBytes);
	}

	protected String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	public SerializingConverter getSerializer() {
		return serializer;
	}

	public LobHandler getLobHandler() {
		return lobHandler;
	}
}
