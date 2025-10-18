/*
 * Copyright 2025-present the original author or authors.
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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link ChannelMessageStorePreparedStatementSetter} implementation that uses JSON
 * serialization for {@link Message} objects instead of Java serialization.
 * <p>
 * By default, this implementation stores the entire message (including headers and payload) as JSON,
 * with type information embedded using Jackson's {@code @class} property for proper deserialization.
 * <p>
 * <b>IMPORTANT:</b> JSON serialization exposes message content in text format in the database.
 * Ensure proper database access controls and encryption for sensitive data.
 * Consider the security implications before using this in production with sensitive information.
 * <p>
 * <b>Database Requirements:</b>
 * This implementation requires modifying the MESSAGE_CONTENT column to a text-based type:
 * <ul>
 *     <li>PostgreSQL: Change from {@code BYTEA} to {@code JSONB}</li>
 *     <li>MySQL: Change from {@code BLOB} to {@code JSON}</li>
 *     <li>H2: Change from {@code LONGVARBINARY} to {@code CLOB}</li>
 * </ul>
 * See the reference documentation for schema migration instructions.
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * &#64;Bean
 * JdbcChannelMessageStore messageStore(DataSource dataSource) {
 *     JdbcChannelMessageStore store = new JdbcChannelMessageStore(dataSource);
 *     store.setChannelMessageStoreQueryProvider(new PostgresChannelMessageStoreQueryProvider());
 *
 *     // Enable JSON serialization (requires schema modification)
 *     store.setPreparedStatementSetter(
 *         new JsonChannelMessageStorePreparedStatementSetter());
 *     store.setMessageRowMapper(
 *         new JsonMessageRowMapper("com.example"));
 *
 *     return store;
 * }
 * }</pre>
 *
 * @author Yoobin Yoon
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class JsonChannelMessageStorePreparedStatementSetter extends ChannelMessageStorePreparedStatementSetter {

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	/**
	 * Create a new {@link JsonChannelMessageStorePreparedStatementSetter} with the
	 * default {@link JsonObjectMapper} configured for Spring Integration message serialization.
	 * <p>
	 * This constructor is suitable when serializing standard Spring Integration
	 * and Java classes. Custom payload types will require their package to be added to the
	 * corresponding {@link JsonMessageRowMapper}.
	 */
	public JsonChannelMessageStorePreparedStatementSetter() {
		this(JsonObjectMapperProvider.newMessagingAwareInstance());
	}

	/**
	 * Create a new {@link JsonChannelMessageStorePreparedStatementSetter} with a
	 * custom {@link JsonObjectMapper}.
	 * <p>
	 * This constructor allows full control over the JSON serialization configuration.
	 * <p>
	 * <b>Note:</b> The same JsonObjectMapper configuration should be used in the corresponding
	 * {@link JsonMessageRowMapper} for consistent serialization and deserialization.
	 * @param jsonObjectMapper the {@link JsonObjectMapper} to use for JSON serialization
	 */
	public JsonChannelMessageStorePreparedStatementSetter(JsonObjectMapper<?, ?> jsonObjectMapper) {
		super();
		Assert.notNull(jsonObjectMapper, "'jsonObjectMapper' must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	@Override
	public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage,
			Object groupId, String region, boolean priorityEnabled) throws SQLException {

		super.setValues(preparedStatement, requestMessage, groupId, region, priorityEnabled);

		try {
			String json = this.jsonObjectMapper.toJson(requestMessage);

			String dbProduct = preparedStatement.getConnection().getMetaData().getDatabaseProductName();

			if ("PostgreSQL".equalsIgnoreCase(dbProduct)) {
				preparedStatement.setObject(6, json, Types.OTHER);
			}
			else {
				preparedStatement.setString(6, json);
			}
		}
		catch (IOException ex) {
			throw new SQLException("Failed to serialize message to JSON: " + requestMessage, ex);
		}
	}

}
