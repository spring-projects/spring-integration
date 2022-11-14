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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class JdbcOutboundGatewayTests {

	private static EmbeddedDatabase dataSource;

	@BeforeClass
	public static void setup() {
		dataSource = new EmbeddedDatabaseBuilder().build();
	}

	@AfterClass
	public static void teardown() {
		dataSource.shutdown();
	}

	@Test
	public void testSetMaxRowsPerPollWithoutSelectQuery() {
		EmbeddedDatabase dataSource = new EmbeddedDatabaseBuilder().build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "update something");

		try {
			jdbcOutboundGateway.setMaxRows(10);
			jdbcOutboundGateway.setBeanFactory(mock(BeanFactory.class));
			jdbcOutboundGateway.afterPropertiesSet();

			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
					.isEqualTo("If you want to set 'maxRows', then you must provide a 'selectQuery'.");
		}

		dataSource.shutdown();
	}

	@Test
	public void testConstructorWithNullJdbcOperations() {
		JdbcOperations jdbcOperations = null;

		try {
			new JdbcOutboundGateway(jdbcOperations, "select * from DOES_NOT_EXIST");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'jdbcOperations' must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testConstructorWithEmptyAndNullQueries() {
		final String selectQuery = "   ";
		final String updateQuery = null;

		try {
			new JdbcOutboundGateway(dataSource, updateQuery, selectQuery);

			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
					.isEqualTo("The 'updateQuery' and the 'selectQuery' must not both be null or empty.");
		}
	}

	@Test
	public void testSetMaxRowsPerPoll() {
		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "select * from DOES_NOT_EXIST");

		try {
			jdbcOutboundGateway.setMaxRows(null);

			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'maxRows' must not be null.");
		}
	}

}
