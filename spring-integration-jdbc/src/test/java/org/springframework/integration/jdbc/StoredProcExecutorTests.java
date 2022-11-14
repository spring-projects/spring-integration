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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 */
public class StoredProcExecutorTests {

	@Test
	public void testStoredProcExecutorWithNullDataSource() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new StoredProcExecutor(null))
				.withMessage("dataSource must not be null.");
	}

	@Test
	public void testStoredProcExecutorWithNullProcedureName() {
		DataSource datasource = mock(DataSource.class);

		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException()
				.isThrownBy(storedProcExecutor::afterPropertiesSet)
				.withMessage("You must either provide a "
						+ "Stored Procedure Name or a Stored Procedure Name Expression.");
	}

	@Test
	public void testStoredProcExecutorWithEmptyProcedureName() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setStoredProcedureName("      "))
				.withMessage("storedProcedureName must not be null and cannot be empty.");
	}

	@Test
	public void testGetStoredProcedureNameExpressionAsString() throws Exception {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));
		storedProcExecutor.afterPropertiesSet();

		assertThat(storedProcExecutor.getStoredProcedureNameExpressionAsString())
				.isEqualTo("headers['stored_procedure_name']");
	}

	@Test
	public void testGetStoredProcedureNameExpressionAsString2() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		storedProcExecutor.setStoredProcedureName("123");
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));
		storedProcExecutor.afterPropertiesSet();

		assertThat(storedProcExecutor.getStoredProcedureName()).isEqualTo("123");
		assertThat(storedProcExecutor.getStoredProcedureNameExpressionAsString()).isEqualTo("123");
	}

	@Test
	public void testSetReturningResultSetRowMappersWithNullMap() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setReturningResultSetRowMappers(null))
				.withMessage("'returningResultSetRowMappers' must not be null.");
	}

	@Test
	public void testSetReturningResultSetRowMappersWithMapContainingNullValues() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		Map<String, RowMapper<?>> rowMappers = new HashMap<>();
		rowMappers.put("results", null);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setReturningResultSetRowMappers(rowMappers))
				.withMessage("'returningResultSetRowMappers' cannot contain null values.");
	}

	@Test
	public void testSetReturningResultSetRowMappersWithEmptyMap() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		Map<String, RowMapper<?>> rowmappers = new HashMap<String, RowMapper<?>>();

		storedProcExecutor.setReturningResultSetRowMappers(rowmappers);

		//Should Successfully finish

	}

	@Test
	public void testSetSqlParameterSourceFactoryWithNullParameter() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		try {
			storedProcExecutor.setSqlParameterSourceFactory(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("sqlParameterSourceFactory must not be null.");
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetSqlParametersWithNullValueInList() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<SqlParameter> sqlParameters = new ArrayList<>();
		sqlParameters.add(null);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setSqlParameters(sqlParameters))
				.withMessage("'sqlParameters' cannot contain null values.");
	}

	@Test
	public void testSetSqlParametersWithEmptyList() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<SqlParameter> sqlParameters = new ArrayList<>();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setSqlParameters(sqlParameters))
				.withMessage("'sqlParameters' must not be null or empty.");
	}

	@Test
	public void testSetSqlParametersWithNullList() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setSqlParameters(null))
				.withMessage("'sqlParameters' must not be null or empty.");
	}

	@Test
	public void testSetProcedureParametersWithNullValueInList() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<ProcedureParameter> procedureParameters = new ArrayList<>();
		procedureParameters.add(null);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setProcedureParameters(procedureParameters))
				.withMessage("'procedureParameters' cannot contain null values.");
	}

	@Test
	public void testSetProcedureParametersWithEmptyList() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<ProcedureParameter> procedureParameters = new ArrayList<>();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setProcedureParameters(procedureParameters))
				.withMessage("'procedureParameters' must not be null or empty.");
	}

	@Test
	public void testSetProcedureParametersWithNullList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> storedProcExecutor.setProcedureParameters(null))
				.withMessage("'procedureParameters' must not be null or empty.");
	}

	@Test
	public void testStoredProcExecutorWithNonResolvingExpression() throws Exception {
		final DataSource datasource = mock(DataSource.class);

		final StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();

		Map<String, SimpleJdbcCallOperations> jdbcCallOperationsMap = new HashMap<>();
		jdbcCallOperationsMap.put("123", mock(SimpleJdbcCallOperations.class));

		new DirectFieldAccessor(storedProcExecutor)
				.setPropertyValue("jdbcCallOperationsMap", jdbcCallOperationsMap);
		//This should work

		storedProcExecutor.executeStoredProcedure(
				MessageBuilder.withPayload("test")
						.setHeader("stored_procedure_name", "123")
						.build());

		//This should cause an exception

		assertThatIllegalArgumentException()
				.isThrownBy(() ->
						storedProcExecutor.executeStoredProcedure(
								MessageBuilder.withPayload("test")
										.setHeader("some_other_header", "123")
										.build()))
				.withMessage("Unable to resolve Stored Procedure/Function name for the provided Expression " +
						"'headers['stored_procedure_name']'.");
	}

}
