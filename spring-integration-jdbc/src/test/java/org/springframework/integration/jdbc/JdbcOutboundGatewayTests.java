/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
