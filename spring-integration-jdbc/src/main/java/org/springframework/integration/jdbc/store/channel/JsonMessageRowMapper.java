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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link RowMapper} implementation that deserializes {@link Message} objects from
 * JSON format stored in the database.
 * <p>
 * This mapper works in conjunction with {@link JsonChannelMessageStorePreparedStatementSetter}
 * to provide JSON serialization for Spring Integration's JDBC Channel Message Store.
 * <p>
 * Unlike the default {@link MessageRowMapper} which uses Java serialization,
 * this implementation uses JSON to deserialize message strings from the MESSAGE_CONTENT column.
 * <p>
 * The underlying {@link JsonObjectMapper} validates all deserialized classes against a
 * trusted package list to prevent security vulnerabilities.
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
 *         new JsonChannelMessageStorePreparedStatementSetter());
 *     store.setMessageRowMapper(
 *         new JsonMessageRowMapper("com.example"));
 *
 *     return store;
 * }
 * }</pre>
 *
 * @author Yoobin Yoon
 *
 * @since 7.0
 */
public class JsonMessageRowMapper implements RowMapper<Message<?>> {

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	/**
	 * Create a new {@link JsonMessageRowMapper} with additional trusted packages
	 * for deserialization.
	 * <p>
	 * The provided packages are appended to the default trusted packages,
	 * enabling deserialization of custom payload types while maintaining security.
	 * If no packages are provided, only the default trusted packages are used.
	 * @param trustedPackages the additional packages to trust for deserialization.
	 * Can be {@code null} or empty to use only default packages
	 */
	public JsonMessageRowMapper(String @Nullable ... trustedPackages) {
		this(JsonObjectMapperProvider.newMessagingAwareInstance(trustedPackages));
	}

	/**
	 * Create a new {@link JsonMessageRowMapper} with a custom {@link JsonObjectMapper}.
	 * <p>
	 * This constructor allows full control over the JSON deserialization configuration.
	 * <p>
	 * <b>Note:</b> The same JsonObjectMapper configuration should be used in the corresponding
	 * {@link JsonChannelMessageStorePreparedStatementSetter} for consistent
	 * serialization and deserialization.
	 * @param jsonObjectMapper the {@link JsonObjectMapper} to use for JSON deserialization
	 */
	public JsonMessageRowMapper(JsonObjectMapper<?, ?> jsonObjectMapper) {
		Assert.notNull(jsonObjectMapper, "'jsonObjectMapper' must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	@Override
	public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
		try {
			String json = rs.getString("MESSAGE_CONTENT");

			if (json == null) {
				throw new SQLException("MESSAGE_CONTENT column is null at row " + rowNum);
			}

			return Objects.requireNonNull(this.jsonObjectMapper.fromJson(json, Message.class));
		}
		catch (IOException ex) {
			throw new SQLException(
					"Failed to deserialize message from JSON at row " + rowNum + ". "
							+ "Ensure the JSON and the configured JsonObjectMapper use compatible type handling.",
					ex);
		}
	}

}
