/*
 * Copyright 2017-2025 the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Callback to be used with the {@link org.springframework.integration.jdbc.store.JdbcChannelMessageStore}.
 * <p>
 * Behavior is same as standard {@link org.springframework.jdbc.core.PreparedStatementSetter},
 * it takes in additional {@code Message<?> requestMessage}, {@code Object groupId},
 * {@code String region} and {@code boolean priorityEnabled} parameters used
 * for {@code addMessageToGroup} method
 * in the {@link org.springframework.integration.jdbc.store.JdbcChannelMessageStore}.
 * <p>
 * This class can be extended for any custom data structure or columns types.
 * For this purpose the {@code protected} constructor is provided for inheritors.
 * In this case the {@link #serializer} is {@code null} to avoid
 * extra serialization actions if the target custom behavior doesn't imply them.
 *
 * @author Meherzad Lahewala
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see org.springframework.jdbc.core.PreparedStatementSetter
 */
public class ChannelMessageStorePreparedStatementSetter {

	private final SerializingConverter serializer;

	/**
	 * Instantiate a {@link ChannelMessageStorePreparedStatementSetter} with the provided
	 * serializer and lobHandler, which both must not be null.
	 * @param serializer the {@link SerializingConverter} to build {@code byte[]} from
	 * the request message
	 * @since 6.4
	 */
	public ChannelMessageStorePreparedStatementSetter(SerializingConverter serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.serializer = serializer;
	}

	/**
	 * The default constructor for inheritors who are not interested in the message
	 * serialization to {@code byte[]}.
	 * The {@link #serializer} is {@code null} from this constructor,
	 * therefore any serialization isn't happened in the default {@link #setValues} implementation.
	 * A target implementor must ensure the proper custom logic for storing message.
	 */
	protected ChannelMessageStorePreparedStatementSetter() {
		this.serializer = null;
	}

	/**
	 * Perform a preparedStatement parameters population according provided arguments.
	 * The default functionality is (parameter - data):
	 * <ul>
	 *     <li>1 - messageId
	 *     <li>2 - groupKey
	 *     <li>3 - region
	 *     <li>4 - createdDate
	 *     <li>5 - priority if enabled, otherwise null
	 *     <li>6 - serialized message if {@link #serializer} is provided.
	 * </ul>
	 * An inheritor may consider to call this method for population common properties and perform
	 * custom message serialization logic for the parameter #6.
	 * Any custom data structure population can be achieved with full overriding of this method.
	 * @param preparedStatement the {@link PreparedStatement} to populate columns based on the provided arguments
	 * @param requestMessage the {@link Message} to store
	 * @param groupId the group id for the message to store
	 * @param region the region in the target table to distinguish different database clients
	 * @param priorityEnabled the flag to indicate if priority has to be stored
	 * @throws SQLException the exception throws during data population
	 */
	public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage, Object groupId, String region,
			boolean priorityEnabled) throws SQLException {

		String groupKey = Objects.toString(UUIDConverter.getUUID(groupId), null);
		long createdDate = System.currentTimeMillis();
		String messageId = Objects.toString(UUIDConverter.getUUID(requestMessage.getHeaders().getId()), null);

		preparedStatement.setString(1, messageId);
		preparedStatement.setString(2, groupKey);
		preparedStatement.setString(3, region); // NOSONAR magic number
		preparedStatement.setLong(4, createdDate); // NOSONAR magic number

		Integer priority = requestMessage.getHeaders().get(IntegrationMessageHeaderAccessor.PRIORITY, Integer.class);

		if (priorityEnabled && priority != null) {
			preparedStatement.setInt(5, priority); // NOSONAR magic number
		}
		else {
			preparedStatement.setNull(5, Types.NUMERIC); // NOSONAR magic number
		}

		if (this.serializer != null) {
			byte[] messageBytes = this.serializer.convert(requestMessage);
			preparedStatement.setBytes(6, messageBytes); // NOSONAR magic number
		}
	}

}
