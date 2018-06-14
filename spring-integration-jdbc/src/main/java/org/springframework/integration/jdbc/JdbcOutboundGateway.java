/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JdbcOutboundGateway extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private final JdbcMessageHandler handler;

	private final JdbcPollingChannelAdapter poller;

	private SqlParameterSourceFactory sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();

	private boolean sqlParameterSourceFactorySet;

	private boolean keysGenerated;

	private Integer maxRows;

	public JdbcOutboundGateway(DataSource dataSource, String updateQuery) {
		this(new JdbcTemplate(dataSource), updateQuery, null);
	}

	public JdbcOutboundGateway(DataSource dataSource, String updateQuery, String selectQuery) {
		this(new JdbcTemplate(dataSource), updateQuery, selectQuery);
	}

	public JdbcOutboundGateway(JdbcOperations jdbcOperations, String updateQuery) {
		this(jdbcOperations, updateQuery, null);
	}

	public JdbcOutboundGateway(JdbcOperations jdbcOperations, String updateQuery, String selectQuery) {
		Assert.notNull(jdbcOperations, "'jdbcOperations' must not be null.");

		if (!StringUtils.hasText(updateQuery) && !StringUtils.hasText(selectQuery)) {
			throw new IllegalArgumentException(
					"The 'updateQuery' and the 'selectQuery' must not both be null or empty.");
		}

		if (StringUtils.hasText(selectQuery)) {
			this.poller = new JdbcPollingChannelAdapter(jdbcOperations, selectQuery);
			this.poller.setMaxRows(1);
		}
		else {
			this.poller = null;
		}

		if (StringUtils.hasText(updateQuery)) {
			this.handler = new JdbcMessageHandler(jdbcOperations, updateQuery);
		}
		else {
			this.handler = null;
		}

	}

	/**
	 * The maximum number of rows to pull out of the query results per poll (if
	 * greater than zero, otherwise all rows will be packed into the outgoing message).
	 * The value is ultimately set on the underlying {@link JdbcPollingChannelAdapter}.
	 * If not specified this value will default to {@code 1}.
	 * This parameter is only applicable if a selectQuery was provided. Null values
	 * are not permitted.
	 * @param maxRowsPerPoll the number of rows to select. Must not be null.
	 * @deprecated since 5.1 in favor of {@link #setMaxRows(Integer)}
	 */
	@Deprecated
	public void setMaxRowsPerPoll(Integer maxRowsPerPoll) {
		setMaxRows(maxRowsPerPoll);
	}

	/**
	 * The maximum number of rows to query.
	 * The value is ultimately set on the underlying {@link JdbcPollingChannelAdapter}.
	 * If not specified this value will default to {@code 1}.
	 * This parameter is only applicable if a selectQuery was provided. Null values
	 * are not permitted.
	 * @param maxRows the number of rows to select. Must not be null.
	 * @since 5.1
	 * @see JdbcPollingChannelAdapter#setMaxRows(int)
	 */
	public void setMaxRows(Integer maxRows) {
		Assert.notNull(maxRows, "'maxRows' must not be null.");
		this.maxRows = maxRows;
	}

	/**
	 * Flag to indicate that the update query is an insert with auto-generated keys,
	 * which will be logged at debug level.
	 * @param keysGenerated the flag value to set
	 */
	public void setKeysGenerated(boolean keysGenerated) {
		this.keysGenerated = keysGenerated;
	}

	public void setRequestSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		Assert.notNull(this.handler, "'handler' cannot be null");
		this.handler.setSqlParameterSourceFactory(sqlParameterSourceFactory);
	}

	public void setRequestPreparedStatementSetter(MessagePreparedStatementSetter requestPreparedStatementSetter) {
		Assert.notNull(this.handler, "'handler' cannot be null");
		this.handler.setPreparedStatementSetter(requestPreparedStatementSetter);
	}

	public void setReplySqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
		this.sqlParameterSourceFactorySet = true;
	}

	public void setRowMapper(RowMapper<?> rowMapper) {
		this.poller.setRowMapper(rowMapper);
	}

	@Override
	public String getComponentType() {
		return "jdbc:outbound-gateway";
	}

	@Override
	protected void doInit() {
		if (this.maxRows != null) {
			Assert.notNull(this.poller, "If you want to set 'maxRows', then you must provide a 'selectQuery'.");
			this.poller.setMaxRows(this.maxRows);
		}

		if (this.handler != null) {
			this.handler.setBeanFactory(this.getBeanFactory());
			this.handler.afterPropertiesSet();
		}

		if (!this.sqlParameterSourceFactorySet && this.getBeanFactory() != null) {
			((ExpressionEvaluatingSqlParameterSourceFactory) this.sqlParameterSourceFactory)
					.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		List<?> list;

		if (this.handler != null) {
			list = this.handler.executeUpdateQuery(requestMessage, this.keysGenerated);
		}
		else {
			list = Collections.emptyList();
		}

		if (this.poller != null) {
			SqlParameterSource sqlQueryParameterSource = this.sqlParameterSourceFactory
					.createParameterSource(requestMessage);
			if (this.keysGenerated) {
				if (!list.isEmpty()) {
					if (list.size() == 1) {
						sqlQueryParameterSource = this.sqlParameterSourceFactory.createParameterSource(list.get(0));
					}
					else {
						sqlQueryParameterSource = this.sqlParameterSourceFactory.createParameterSource(list);
					}
				}
			}
			list = this.poller.doPoll(sqlQueryParameterSource);
			if (list.isEmpty()) {
				return null;
			}
		}
		Object payload = list;
		if (list.isEmpty()) {
			return null;
		}
		if (list.size() == 1) {
			payload = list.get(0);
		}
		return payload;
	}

}
