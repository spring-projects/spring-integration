/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * A message handler that executes an SQL update. Dynamic query parameters are supported through the
 * {@link SqlParameterSourceFactory} abstraction, the default implementation of which wraps the message so that its bean
 * properties can be referred to by name in the query string, e.g.
 *
 * <pre class="code">
 * INSERT INTO ITEMS (MESSAGE_ID, PAYLOAD) VALUES (:headers[id], :payload)
 * </pre>
 *
 * <p>
 * When a message payload is an instance of {@link Iterable}, a
 * {@link NamedParameterJdbcOperations#batchUpdate(String, SqlParameterSource[])} is performed, where each
 * {@link SqlParameterSource} instance is based on items wrapped into an internal {@link Message} implementation with
 * headers from the request message. The item is wrapped only if it is not a {@link Message} already.
 * <p>
 * When a {@link #preparedStatementSetter} is configured, it is applied for each item in the appropriate
 * {@link JdbcOperations#batchUpdate(String, BatchPreparedStatementSetter)} function.
 * <p>
 * NOTE: The batch update is not supported when {@link #keysGenerated} is in use.
 *
 * N.B. do not use quotes to escape the header keys. The default SQL parameter source (from Spring JDBC) can also handle
 * headers with dotted names (e.g. <code>business.id</code>)
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 2.0
 */
public class JdbcMessageHandler extends AbstractMessageHandler {

	private final ResultSetExtractor<List<Map<String, Object>>> generatedKeysResultSetExtractor =
			new RowMapperResultSetExtractor<>(new ColumnMapRowMapper(), 1);

	private final NamedParameterJdbcOperations jdbcOperations;

	private final String updateSql;

	private PreparedStatementCreator generatedKeysStatementCreator;

	private SqlParameterSourceFactory sqlParameterSourceFactory;

	private boolean keysGenerated;

	private MessagePreparedStatementSetter preparedStatementSetter;

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be obtained and the select query to
	 * execute to retrieve new rows.
	 * @param dataSource Must not be null
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(DataSource dataSource, String updateSql) {
		this(new JdbcTemplate(dataSource), updateSql);
	}

	/**
	 * Constructor taking {@link JdbcOperations} instance to use for query execution and the select query to execute to
	 * retrieve new rows.
	 * @param jdbcOperations instance to use for query execution
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(JdbcOperations jdbcOperations, String updateSql) {
		Assert.notNull(jdbcOperations, "'jdbcOperations' must not be null.");
		Assert.hasText(updateSql, "'updateSql' must not be empty.");
		this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations);
		this.updateSql = updateSql;
	}

	/**
	 * Flag to indicate that the update query is an insert with auto-generated keys,
	 * which will be logged at debug level.
	 * @param keysGenerated the flag value to set
	 */
	public void setKeysGenerated(boolean keysGenerated) {
		this.keysGenerated = keysGenerated;
	}

	/**
	 * Configure an {@link SqlParameterSourceFactory}.
	 * @param sqlParameterSourceFactory the {@link SqlParameterSourceFactory} to use.
	 */
	public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
	}

	/**
	 * Specify a {@link MessagePreparedStatementSetter} to populate parameters on the
	 * {@link PreparedStatement} with the {@link Message} context.
	 * <p>This is a low-level alternative to the {@link SqlParameterSourceFactory}.
	 * @param preparedStatementSetter the {@link MessagePreparedStatementSetter} to set.
	 * @since 4.2
	 */
	public void setPreparedStatementSetter(@Nullable MessagePreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
		if (preparedStatementSetter != null) {
			PreparedStatementCreatorFactory preparedStatementCreatorFactory =
					new PreparedStatementCreatorFactory(this.updateSql);
			preparedStatementCreatorFactory.setReturnGeneratedKeys(true);
			this.generatedKeysStatementCreator =
					preparedStatementCreatorFactory.newPreparedStatementCreator((Object[]) null);
		}
	}

	@Override
	public String getComponentType() {
		return "jdbc:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(!(this.sqlParameterSourceFactory != null && this.preparedStatementSetter != null),
				"'sqlParameterSourceFactory' and 'preparedStatementSetter' are mutually exclusive.");
		if (this.sqlParameterSourceFactory == null && this.preparedStatementSetter == null) {
			this.sqlParameterSourceFactory = new BeanPropertySqlParameterSourceFactory();
		}
	}

	/**
	 * Executes the update, passing the message into the {@link SqlParameterSourceFactory}.
	 */
	@Override
	protected void handleMessageInternal(Message<?> message) {
		List<? extends Map<String, Object>> keys = executeUpdateQuery(message, this.keysGenerated);
		if (!keys.isEmpty()) {
			logger.debug(() -> "Generated keys: " + keys);
		}
	}

	/**
	 * Execute an update for the provided message and generated keys flag.
	 * @param message the message to use for update query params.
	 * @param keysGenerated generate key or not.
	 * @return a generated keys for update.
	 */
	protected List<? extends Map<String, Object>> executeUpdateQuery(final Message<?> message, boolean keysGenerated) {
		if (keysGenerated) {
			if (this.preparedStatementSetter != null) {
				return this.jdbcOperations.getJdbcOperations()
						.execute(this.generatedKeysStatementCreator,
								ps -> {
									this.preparedStatementSetter.setValues(ps, message);
									ps.executeUpdate();
									ResultSet keys = ps.getGeneratedKeys(); // NOSONAR closed in JdbcUtils
									if (keys != null) {
										try {

											return this.generatedKeysResultSetExtractor.extractData(keys);
										}
										finally {
											JdbcUtils.closeResultSet(keys);
										}
									}
									return new LinkedList<>();
								});
			}
			else {
				KeyHolder keyHolder = new GeneratedKeyHolder();
				this.jdbcOperations.update(this.updateSql,
						this.sqlParameterSourceFactory.createParameterSource(message), keyHolder);
				return keyHolder.getKeyList();
			}
		}
		else {
			if (message.getPayload() instanceof Iterable) {
				Stream<? extends Message<?>> messageStream =
						StreamSupport.stream(((Iterable<?>) message.getPayload()).spliterator(), false)
								.map(payload -> payloadToMessage(payload, message.getHeaders()));

				int[] updates;

				if (this.preparedStatementSetter != null) {
					Message<?>[] messages = messageStream.toArray(Message<?>[]::new);

					updates = this.jdbcOperations.getJdbcOperations()
							.batchUpdate(this.updateSql, new BatchPreparedStatementSetter() {

								@Override
								public void setValues(PreparedStatement ps, int i) throws SQLException {
									JdbcMessageHandler.this.preparedStatementSetter.setValues(ps, messages[i]);
								}

								@Override
								public int getBatchSize() {
									return messages.length;
								}

							});
				}
				else {
					SqlParameterSource[] sqlParameterSources =
							messageStream.map(this.sqlParameterSourceFactory::createParameterSource)
									.toArray(SqlParameterSource[]::new);

					updates = this.jdbcOperations.batchUpdate(this.updateSql, sqlParameterSources);
				}

				return Arrays.stream(updates)
						.mapToObj(updated -> {
							Map<String, Object> map = new LinkedCaseInsensitiveMap<>();
							map.put("UPDATED", updated);
							return map;
						})
						.collect(Collectors.toList());
			}
			else {
				int updated;

				if (this.preparedStatementSetter != null) {
					updated = this.jdbcOperations.getJdbcOperations()
							.update(this.updateSql, ps -> this.preparedStatementSetter.setValues(ps, message));
				}
				else {
					updated = this.jdbcOperations.update(this.updateSql,
							this.sqlParameterSourceFactory.createParameterSource(message));
				}

				LinkedCaseInsensitiveMap<Object> map = new LinkedCaseInsensitiveMap<>();
				map.put("UPDATED", updated);
				return Collections.singletonList(map);
			}
		}
	}

	private static Message<?> payloadToMessage(Object payload, MessageHeaders messageHeaders) {
		if (payload instanceof Message) {
			return (Message<?>) payload;
		}

		return new Message<>() {

			@Override
			public Object getPayload() {
				return payload;
			}

			@Override
			public MessageHeaders getHeaders() {
				return messageHeaders;
			}

		};
	}

}
