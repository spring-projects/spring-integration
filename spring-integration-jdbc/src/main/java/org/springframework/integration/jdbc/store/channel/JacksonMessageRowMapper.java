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

import java.sql.ResultSet;
import java.sql.SQLException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.integration.support.json.JacksonMessagingUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageRowMapper} implementation that deserializes {@link Message} objects from
 * JSON format stored in the database.
 * <p>
 * This mapper works in conjunction with {@link JacksonChannelMessageStorePreparedStatementSetter}
 * to provide human-readable JSON serialization for Spring Integration's JDBC Channel Message Store.
 * <p>
 * Unlike the default {@link MessageRowMapper} which uses Java serialization via
 * {@link org.springframework.integration.support.converter.AllowListDeserializingConverter},
 * this implementation uses Jackson to deserialize JSON strings directly from the MESSAGE_BYTES column.
 * <p>
 * The {@link ObjectMapper} is configured using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}
 * which includes custom deserializers for Spring Integration message types and validates
 * class types using an allow-list of trusted packages for security.
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
 *
 * @author Yoobin Yoon
 * @since 7.0
 *
 * @see JacksonChannelMessageStorePreparedStatementSetter
 * @see JacksonMessagingUtils#messagingAwareMapper(String...)
 */
public class JacksonMessageRowMapper extends MessageRowMapper {

	private final ObjectMapper objectMapper;

	/**
	 * Create a new {@link JacksonMessageRowMapper} with the default trusted packages
	 * from {@link JacksonMessagingUtils#DEFAULT_TRUSTED_PACKAGES}.
	 * <p>
	 * This constructor is suitable when you only need to deserialize
	 * standard Spring Integration and Java classes.
	 */
	public JacksonMessageRowMapper() {
		this(new String[0]);
	}

	/**
	 * Create a new {@link JacksonMessageRowMapper} with additional trusted packages
	 * for deserialization.
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
	public JacksonMessageRowMapper(String... trustedPackages) {
		super(); // Call protected constructor - no deserializer needed
		this.objectMapper = JacksonMessagingUtils.messagingAwareMapper(trustedPackages);
	}

	/**
	 * Create a new {@link JacksonMessageRowMapper} with a custom {@link ObjectMapper}.
	 * <p>
	 * This constructor allows full control over the JSON deserialization configuration.
	 * The provided mapper should be configured appropriately for Message deserialization,
	 * typically using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}.
	 * @param objectMapper the {@link ObjectMapper} to use for JSON deserialization
	 */
	public JacksonMessageRowMapper(ObjectMapper objectMapper) {
		super(); // Call protected constructor - no deserializer needed
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Map a row from the result set to a {@link Message} by deserializing JSON.
	 * <p>
	 * This implementation:
	 * <ol>
	 *     <li>Reads the JSON string from the MESSAGE_BYTES column</li>
	 *     <li>Deserializes it to a Message using Jackson</li>
	 *     <li>Validates class types against trusted packages</li>
	 * </ol>
	 * The JSON must include {@code @class} properties for proper deserialization
	 * of polymorphic types and payload classes.
	 * @param rs the {@link ResultSet} to map
	 * @param rowNum the current row number
	 * @return the deserialized {@link Message}
	 * @throws SQLException if an error occurs reading from the result set or
	 * if JSON deserialization fails
	 */
	@Override
	public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
		try {
			String json = rs.getString("MESSAGE_BYTES");

			if (json == null) {
				throw new SQLException("MESSAGE_BYTES column is null at row " + rowNum);
			}

			return this.objectMapper.readValue(json, new TypeReference<Message<?>>() {

			});
		}
		catch (JacksonException ex) {
			throw new SQLException("Failed to deserialize message from JSON at row " + rowNum +
					". Ensure the JSON was created by JacksonChannelMessageStorePreparedStatementSetter "
					+
					"and contains proper @class type information.", ex);
		}
	}

}
