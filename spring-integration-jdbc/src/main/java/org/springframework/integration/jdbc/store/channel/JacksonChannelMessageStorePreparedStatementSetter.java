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
 * with type information embedded using Jackson's {@code @class} property. This makes
 * the stored data human-readable and useful for debugging and troubleshooting.
 * <p>
 * The {@link ObjectMapper} is configured using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}
 * which includes custom serializers/deserializers for Spring Integration message types
 * and embeds class type information for secure deserialization.
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * &#64;Bean
 * JdbcChannelMessageStore messageStore(DataSource dataSource) {
 *     JdbcChannelMessageStore store = new JdbcChannelMessageStore(dataSource);
 *     store.setChannelMessageStoreQueryProvider(new PostgresChannelMessageStoreQueryProvider());
 *
 *     // Enable JSON serialization
 *     store.setPreparedStatementSetter(
 *         new JacksonChannelMessageStorePreparedStatementSetter("com.example"));
 *     store.setMessageRowMapper(
 *         new JacksonMessageRowMapper("com.example"));
 *
 *     return store;
 * }
 * }</pre>
 * <p>
 * <b>Database Column Type:</b>
 * This implementation requires a text-based column type that supports JSON storage:
 * <ul>
 *     <li>PostgreSQL: {@code JSONB} (recommended) or {@code JSON}</li>
 *     <li>MySQL: {@code JSON} </li>
 *     <li>H2: {@code CLOB} </li>
 * </ul>
 * <p>
 * <b>Note:</b> The standard Spring Integration schemas use {@code BLOB}/{@code BYTEA} columns
 * which are not suitable for human-readable JSON storage. Use the provided JSON-specific
 * schemas from {@code schema-*-json.sql} files.
 *
 * @author Yoobin Yoon
 * @since 7.0
 *
 * @see JacksonMessageRowMapper
 * @see JacksonMessagingUtils#messagingAwareMapper(String...)
 */
public class JacksonChannelMessageStorePreparedStatementSetter extends ChannelMessageStorePreparedStatementSetter {

	private final ObjectMapper objectMapper;

	/**
	 * Create a new {@link JacksonChannelMessageStorePreparedStatementSetter} with the
	 * default trusted packages from {@link JacksonMessagingUtils#DEFAULT_TRUSTED_PACKAGES}.
	 * <p>
	 * This constructor is suitable when you only need to serialize/deserialize
	 * standard Spring Integration and Java classes.
	 */
	public JacksonChannelMessageStorePreparedStatementSetter() {
		this(new String[0]);
	}

	/**
	 * Create a new {@link JacksonChannelMessageStorePreparedStatementSetter} with
	 * additional trusted packages for deserialization.
	 * <p>
	 * The provided packages are appended to the default allow-list from
	 * {@link JacksonMessagingUtils#DEFAULT_TRUSTED_PACKAGES}, enabling deserialization
	 * of custom payload types.
	 * <p>
	 * <b>Package Matching:</b>
	 * <ul>
	 *   <li>{@code "com.example"} – trusts {@code com.example} and all subpackages
	 *       (e.g., {@code com.example.MyClass}, {@code com.example.sub.AnotherClass})</li>
	 *   <li>{@code "com.example.MyClass"} – trusts only the specific class</li>
	 *   <li>{@code "*"} – trusts all packages (not recommended for production)</li>
	 * </ul>
	 * <p>
	 * <b>Note:</b> Subpackages are automatically included without wildcards.
	 *
	 * @param trustedPackages the additional packages to trust for deserialization
	 */
	public JacksonChannelMessageStorePreparedStatementSetter(String... trustedPackages) {
		super(); // Call protected constructor - no serializer needed
		this.objectMapper = JacksonMessagingUtils.messagingAwareMapper(trustedPackages);
	}

	/**
	 * Create a new {@link JacksonChannelMessageStorePreparedStatementSetter} with
	 * a custom {@link ObjectMapper}.
	 * <p>
	 * This constructor allows full control over the JSON serialization configuration.
	 * The provided mapper should be configured appropriately for Message serialization,
	 * typically using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}.
	 * @param objectMapper the {@link ObjectMapper} to use for JSON serialization
	 */
	public JacksonChannelMessageStorePreparedStatementSetter(ObjectMapper objectMapper) {
		super(); // Call protected constructor - no serializer needed
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Set the prepared statement values, serializing the message to JSON format.
	 * <p>
	 * This implementation:
	 * <ol>
	 *     <li>Calls {@code super.setValues()} to populate common fields (parameters 1-5)</li>
	 *     <li>Serializes the entire {@link Message} (payload and headers) to JSON</li>
	 *     <li>Sets parameter 6 using a database-specific method:
	 *         <ul>
	 *             <li><b>PostgreSQL:</b> {@link PreparedStatement#setObject(int, Object, int)}
	 *                 with {@link Types#OTHER} for automatic JSONB/JSON conversion</li>
	 *             <li><b>Other databases:</b> {@link PreparedStatement#setString(int, String)}
	 *                 for VARCHAR, CLOB, or JSON columns</li>
	 *         </ul>
	 *     </li>
	 * </ol>
	 * <p>
	 * The resulting JSON includes {@code @class} properties for type information,
	 * enabling proper deserialization of polymorphic types.
	 *
	 * @param preparedStatement the {@link PreparedStatement} to populate
	 * @param requestMessage the {@link Message} to store
	 * @param groupId the group identifier
	 * @param region the region in the target table
	 * @param priorityEnabled whether message priority should be stored
	 * @throws SQLException if statement preparation or JSON serialization fails
	 */
	@Override
	public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage, Object groupId,
			String region, boolean priorityEnabled) throws SQLException {

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
			throw new SQLException("Failed to serialize message to JSON. Message ID: " +
					requestMessage.getHeaders().getId(), ex);
		}
	}

}
