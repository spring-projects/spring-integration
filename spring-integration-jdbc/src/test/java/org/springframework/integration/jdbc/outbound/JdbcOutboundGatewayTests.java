/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jdbc.outbound;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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

	@BeforeAll
	public static void setup() {
		dataSource = new EmbeddedDatabaseBuilder().build();
	}

	@AfterAll
	public static void teardown() {
		dataSource.shutdown();
	}

	@Test
	public void testSetMaxRowsPerPollWithoutSelectQuery() {
		EmbeddedDatabase dataSource = new EmbeddedDatabaseBuilder().build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "update something");
		jdbcOutboundGateway.setMaxRows(10);
		jdbcOutboundGateway.setBeanFactory(mock());

		assertThatIllegalArgumentException()
				.isThrownBy(jdbcOutboundGateway::afterPropertiesSet)
				.withMessage("'poller' must not be null when 'maxRows' is not null");

		dataSource.shutdown();
	}

	@Test
	public void testConstructorWithNullJdbcOperations() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JdbcOutboundGateway((JdbcOperations) null, "select * from DOES_NOT_EXIST"))
				.withMessage("'jdbcOperations' must not be null.");
	}

	@Test
	public void testConstructorWithEmptyAndNullQueries() {
		final String selectQuery = "   ";
		final String updateQuery = null;

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JdbcOutboundGateway(dataSource, updateQuery, selectQuery))
				.withMessage("The 'updateQuery' and the 'selectQuery' must not both be null or empty.");
	}

	@Test
	public void testSetMaxRowsPerPoll() {
		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "select * from DOES_NOT_EXIST");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> jdbcOutboundGateway.setMaxRows(null))
				.withMessage("'maxRows' must not be null.");
	}

}
