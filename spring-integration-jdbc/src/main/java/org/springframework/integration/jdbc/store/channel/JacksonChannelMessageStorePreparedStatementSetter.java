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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.integration.support.json.JacksonMessagingUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link ChannelMessageStorePreparedStatementSetter} implementation that uses Jackson
 * to serialize {@link Message} objects to JSON format instead of Java serialization.
 * <p>
 * This implementation stores the entire message (including headers and payload) as JSON,
 * with type information embedded using Jackson's {@code @class} property.
 * <p>
 * <b>IMPORTANT:</b> JSON serialization exposes message content in text format in the database.
 * Ensure proper database access controls and encryption for sensitive data.
 * Consider the security implications before using this in production with sensitive information.
 * <p>
 * The {@link ObjectMapper} is configured using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}
 * which includes custom serializers/deserializers for Spring Integration message types
 * and embeds class type information for secure deserialization.
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
 *         new JacksonChannelMessageStorePreparedStatementSetter());
 *     store.setMessageRowMapper(
 *         new JacksonMessageRowMapper("com.example"));
 *
 *     return store;
 * }
 * }</pre>
 *
 * @author Yoobin Yoon
 *
 * @since 7.0
 */
public class JacksonChannelMessageStorePreparedStatementSetter extends ChannelMessageStorePreparedStatementSetter {

	private final ObjectMapper objectMapper;

	/**
	 * Create a new {@link JacksonChannelMessageStorePreparedStatementSetter} with the
	 * default trusted packages from {@link JacksonMessagingUtils#DEFAULT_TRUSTED_PACKAGES}.
	 * <p>
	 * This constructor is suitable when you only need to serialize standard Spring Integration
	 * and Java classes. Custom payload types will require their package to be added to the
	 * corresponding {@link JacksonMessageRowMapper}.
	 */
	public JacksonChannelMessageStorePreparedStatementSetter() {
		super();
		this.objectMapper = JacksonMessagingUtils.messagingAwareMapper();
	}

	/**
	 * Create a new {@link JacksonChannelMessageStorePreparedStatementSetter} with a
	 * custom {@link ObjectMapper}.
	 * <p>
	 * This constructor allows full control over the JSON serialization configuration.
	 * The provided mapper should be configured appropriately for Message serialization,
	 * typically using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}.
	 * <p>
	 * <b>Note:</b> The same ObjectMapper configuration should be used in the corresponding
	 * {@link JacksonMessageRowMapper} for consistent serialization and deserialization.
	 * @param objectMapper the {@link ObjectMapper} to use for JSON serialization
	 */
	public JacksonChannelMessageStorePreparedStatementSetter(ObjectMapper objectMapper) {
		super();
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	@Override
	public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage,
			Object groupId, String region, boolean priorityEnabled) throws SQLException {

		super.setValues(preparedStatement, requestMessage, groupId, region, priorityEnabled);

		try {
			String json = this.objectMapper.writeValueAsString(requestMessage);

			String dbProduct = preparedStatement.getConnection().getMetaData().getDatabaseProductName();

			if ("PostgreSQL".equalsIgnoreCase(dbProduct)) {
				preparedStatement.setObject(6, json, Types.OTHER); // NOSONAR magic number
			}
			else {
				preparedStatement.setString(6, json); // NOSONAR magic number
			}
		}
		catch (JacksonException ex) {
			throw new SQLException("Failed to serialize message to JSON: " + requestMessage, ex);
		}
	}

}
