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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link RowMapper} implementation that deserializes {@link Message} objects from
 * JSON format stored in the database.
 * <p>
 * This mapper works in conjunction with {@link JacksonChannelMessageStorePreparedStatementSetter}
 * to provide JSON serialization for Spring Integration's JDBC Channel Message Store.
 * <p>
 * Unlike the default {@link MessageRowMapper} which uses Java serialization,
 * this implementation uses Jackson to deserialize JSON strings from the MESSAGE_CONTENT column.
 * <p>
 * The {@link ObjectMapper} is configured using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}
 * which validates all deserialized classes against a trusted package list to prevent
 * security vulnerabilities.
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
public class JacksonMessageRowMapper implements RowMapper<Message<?>> {

	private final ObjectMapper objectMapper;

	/**
	 * Create a new {@link JacksonMessageRowMapper} with additional trusted packages
	 * for deserialization.
	 * <p>
	 * The provided packages are appended to the default trusted packages from
	 * {@link JacksonMessagingUtils#DEFAULT_TRUSTED_PACKAGES}, enabling deserialization
	 * of custom payload types while maintaining security.
	 * @param trustedPackages the additional packages to trust for deserialization
	 */
	public JacksonMessageRowMapper(String... trustedPackages) {
		this.objectMapper = JacksonMessagingUtils.messagingAwareMapper(trustedPackages);
	}

	/**
	 * Create a new {@link JacksonMessageRowMapper} with a custom {@link ObjectMapper}.
	 * <p>
	 * This constructor allows full control over the JSON deserialization configuration.
	 * The provided mapper should be configured appropriately for Message deserialization,
	 * typically using {@link JacksonMessagingUtils#messagingAwareMapper(String...)}.
	 * <p>
	 * <b>Note:</b> The same ObjectMapper configuration should be used in the corresponding
	 * {@link JacksonChannelMessageStorePreparedStatementSetter} for consistent
	 * serialization and deserialization.
	 * @param objectMapper the {@link ObjectMapper} to use for JSON deserialization
	 */
	public JacksonMessageRowMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' must not be null");
		this.objectMapper = objectMapper;
	}

	@Override
	public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
		try {
			String json = rs.getString("MESSAGE_CONTENT");

			if (json == null) {
				throw new SQLException("MESSAGE_CONTENT column is null at row " + rowNum);
			}

			return this.objectMapper.readValue(json, new TypeReference<Message<?>>() {

			});
		}
		catch (JacksonException ex) {
			throw new SQLException(
					"Failed to deserialize message from JSON at row " + rowNum + ". "
							+ "Ensure the JSON was created by JacksonChannelMessageStorePreparedStatementSetter "
							+ "and contains proper @class type information.",
					ex);
		}
	}

}
