/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 */
public class StoredProcExecutorTests {

	private static final Logger LOGGER = Logger.getLogger(StoredProcExecutorTests.class);

	@Test
	public void testStoredProcExecutorWithNullDataSource() {

		try {
		    new StoredProcExecutor(null);
		} catch (IllegalArgumentException e) {
			assertEquals("dataSource must not be null.", e.getMessage());
			return;
		}

		fail("Exception expected.");
	}

	@Test
	public void testStoredProcExecutorWithNullProcedureName() {

		DataSource datasource = mock(DataSource.class);

		try {
			StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);
			storedProcExecutor.setBeanFactory(mock(BeanFactory.class));
			storedProcExecutor.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			assertEquals("You must either provide a "
						+ "Stored Procedure Name or a Stored Procedure Name Expression.", e.getMessage());
			return;
		}

		fail("Exception expected.");
	}

	@Test
	public void testStoredProcExecutorWithEmptyProcedureName() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		try {
			storedProcExecutor.setStoredProcedureName("      ");
		} catch (IllegalArgumentException e) {
			assertEquals("storedProcedureName must not be null and cannot be empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");
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

		assertEquals("headers['stored_procedure_name']", storedProcExecutor.getStoredProcedureNameExpressionAsString());
	}

	@Test
	public void testGetStoredProcedureNameExpressionAsString2() throws Exception {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		storedProcExecutor.setStoredProcedureName("123");
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));
		storedProcExecutor.afterPropertiesSet();

		assertEquals("123", storedProcExecutor.getStoredProcedureName());
		assertEquals("123", storedProcExecutor.getStoredProcedureNameExpressionAsString());
	}

	@Test
	public void testSetReturningResultSetRowMappersWithNullMap() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		try {
			storedProcExecutor.setReturningResultSetRowMappers(null);
		} catch (IllegalArgumentException e) {
			assertEquals("returningResultSetRowMappers must not be null.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetReturningResultSetRowMappersWithMapContainingNullValues() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		Map<String, RowMapper<?>> rowmappers = new HashMap<String, RowMapper<?>>();
		rowmappers.put("results", null);

		try {
			storedProcExecutor.setReturningResultSetRowMappers(rowmappers);
		} catch (IllegalArgumentException e) {
			assertEquals("The provided map cannot contain null values.", e.getMessage());
			return;
		}

		fail("Exception expected.");

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
		} catch (IllegalArgumentException e) {
			assertEquals("sqlParameterSourceFactory must not be null.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetSqlParametersWithNullValueInList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<SqlParameter> sqlParameters = new ArrayList<SqlParameter>();
		sqlParameters.add(null);

		try {
			storedProcExecutor.setSqlParameters(sqlParameters);
		} catch (IllegalArgumentException e) {
			assertEquals("The provided list (sqlParameters) cannot contain null values.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetSqlParametersWithEmptyList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<SqlParameter> sqlParameters = new ArrayList<SqlParameter>();

		try {
			storedProcExecutor.setSqlParameters(sqlParameters);
		} catch (IllegalArgumentException e) {
			assertEquals("sqlParameters must not be null or empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetSqlParametersWithNullList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		try {
			storedProcExecutor.setSqlParameters(null);
		} catch (IllegalArgumentException e) {
			assertEquals("sqlParameters must not be null or empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetProcedureParametersWithNullValueInList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<ProcedureParameter> procedureParameters = new ArrayList<ProcedureParameter>();
		procedureParameters.add(null);

		try {
			storedProcExecutor.setProcedureParameters(procedureParameters);
		} catch (IllegalArgumentException e) {
			assertEquals("The provided list (procedureParameters) cannot contain null values.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetProcedureParametersWithEmptyList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		List<ProcedureParameter> procedureParameters = new ArrayList<ProcedureParameter>();

		try {
			storedProcExecutor.setProcedureParameters(procedureParameters);
		} catch (IllegalArgumentException e) {
			assertEquals("procedureParameters must not be null or empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	public void testSetProcedureParametersWithNullList() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		try {
			storedProcExecutor.setProcedureParameters(null);
		} catch (IllegalArgumentException e) {
			assertEquals("procedureParameters must not be null or empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStoredProcedureLoader() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);
		storedProcExecutor.setStoredProcedureName("123");
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		List<SqlParameter> sqlParameters = TestUtils.getPropertyValue(storedProcExecutor, "sqlParameters", List.class);
		sqlParameters = spy(sqlParameters);
		DirectFieldAccessor dfa = new DirectFieldAccessor(storedProcExecutor);
		dfa.setPropertyValue("sqlParameters", sqlParameters);

		storedProcExecutor.afterPropertiesSet();

		for (int i = 0; i < 3; i++) {
			try {
				storedProcExecutor.executeStoredProcedure();
			}
			catch (Exception e) {
				//Ignore the exception. We aren't interested in the real call.
			}
		}

		verify(sqlParameters).toArray(any(SqlParameter[].class));
	}

}
