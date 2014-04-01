/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.1
 *
 */
public class JdbcOutboundGatewayTests {

	@Test
	public void testSetMaxRowsPerPollWithoutSelectQuery() {


		DataSource dataSource = new EmbeddedDatabaseBuilder().build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "update something");

		try {
			jdbcOutboundGateway.setMaxRowsPerPoll(10);
			jdbcOutboundGateway.setBeanFactory(mock(BeanFactory.class));
			jdbcOutboundGateway.afterPropertiesSet();

		} catch (IllegalArgumentException e) {
			assertEquals("If you want to set 'maxRowsPerPoll', then you must provide a 'selectQuery'.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	@Test
	public void testConstructorWithNulljdbcOperations() {

		JdbcOperations jdbcOperations = null;

		try {
			new JdbcOutboundGateway(jdbcOperations, "select * from DOES_NOT_EXIST");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("'jdbcOperations' must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testConstructorWithEmptyAndNullQueries() {

		final DataSource dataSource = new EmbeddedDatabaseBuilder().build();

		final String selectQuery = "   ";
		final String updateQuery = null;

		try {
			new JdbcOutboundGateway(dataSource, updateQuery, selectQuery);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The 'updateQuery' and the 'selectQuery' must not both be null or empty.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	/**
	 * Test method for
	 * {@link org.springframework.integration.jdbc.JdbcOutboundGateway#setMaxRowsPerPoll(Integer)}.
	 */
	@Test
	public void testSetMaxRowsPerPoll() {


		DataSource dataSource = new EmbeddedDatabaseBuilder().build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "select * from DOES_NOT_EXIST");

		try {
			jdbcOutboundGateway.setMaxRowsPerPoll(null);
		} catch (IllegalArgumentException e) {
			assertEquals("MaxRowsPerPoll must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}
}
