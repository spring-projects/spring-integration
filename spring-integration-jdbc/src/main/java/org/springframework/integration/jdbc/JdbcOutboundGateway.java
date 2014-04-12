/*
 * Copyright 2002-2013 the original author or authors.
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
 *
 * @since 2.0
 */
public class JdbcOutboundGateway extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private final JdbcMessageHandler handler;

	private final JdbcPollingChannelAdapter poller;

	private volatile SqlParameterSourceFactory sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();

	private volatile boolean sqlParameterSourceFactorySet;

	private volatile boolean keysGenerated;

	private volatile Integer maxRowsPerPoll;

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
			throw new IllegalArgumentException("The 'updateQuery' and the 'selectQuery' must not both be null or empty.");
		}

		if (StringUtils.hasText(selectQuery)) {
			poller = new JdbcPollingChannelAdapter(jdbcOperations, selectQuery);
			poller.setMaxRowsPerPoll(1);
		}
		else {
			poller = null;
		}

		if (StringUtils.hasText(updateQuery)) {
			handler = new JdbcMessageHandler(jdbcOperations, updateQuery);
		}
		else {
			handler = null;
		}

	}

	/**
	 * The maximum number of rows to pull out of the query results per poll (if
	 * greater than zero, otherwise all rows will be packed into the outgoing
	 * message).
	 *
	 * The value is ultimately set on the underlying {@link JdbcPollingChannelAdapter}.
	 * If not specified this value will default to <code>zero</code>.
	 *
	 * This parameter is only applicable if a selectQuery was provided. Null values
	 * are not permitted.
	 *
	 * @param maxRowsPerPoll Must not be null.
	 */
	public void setMaxRowsPerPoll(Integer maxRowsPerPoll) {
		Assert.notNull(maxRowsPerPoll, "MaxRowsPerPoll must not be null.");
		this.maxRowsPerPoll = maxRowsPerPoll;
	}

	@Override
	public String getComponentType() {
		return "jdbc:outbound-gateway";
	}

	@Override
	protected void doInit() {
		if (this.maxRowsPerPoll != null) {
			Assert.notNull(poller, "If you want to set 'maxRowsPerPoll', then you must provide a 'selectQuery'.");
			poller.setMaxRowsPerPoll(this.maxRowsPerPoll);
		}

		if (this.handler!= null) {
			handler.setBeanFactory(this.getBeanFactory());
			handler.afterPropertiesSet();
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
			list = handler.executeUpdateQuery(requestMessage, keysGenerated);
		}
		else {
			list = Collections.emptyList();
		}

		if (poller != null) {
			SqlParameterSource sqlQueryParameterSource = sqlParameterSourceFactory
					.createParameterSource(requestMessage);
			if (keysGenerated) {
				if (!list.isEmpty()) {
					if (list.size() == 1) {
						sqlQueryParameterSource = sqlParameterSourceFactory.createParameterSource(list.get(0));
					}
					else {
						sqlQueryParameterSource = sqlParameterSourceFactory.createParameterSource(list);
					}
				}
			}
			list = poller.doPoll(sqlQueryParameterSource);
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
		return this.getMessageBuilderFactory().withPayload(payload).copyHeaders(requestMessage.getHeaders()).build();
	}

	/**
	 * Flag to indicate that the update query is an insert with autogenerated keys, which will be logged at debug level.
	 * @param keysGenerated the flag value to set
	 */
	public void setKeysGenerated(boolean keysGenerated) {
		this.keysGenerated = keysGenerated;
	}

	public void setRequestSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		Assert.notNull(this.handler);
		this.handler.setSqlParameterSourceFactory(sqlParameterSourceFactory);
	}

	public void setReplySqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
		this.sqlParameterSourceFactorySet = true;
	}

	public void setRowMapper(RowMapper<?> rowMapper) {
		poller.setRowMapper(rowMapper);
	}

}
