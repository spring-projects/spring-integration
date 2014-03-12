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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

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
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;

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
	public void testStoredProcExecutorWithNonResolvingExpression() throws Exception {

		final DataSource datasource = mock(DataSource.class);

		final StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();

		this.mockTheOperationsCache(storedProcExecutor);

		//This should work

		storedProcExecutor.executeStoredProcedure(
			MessageBuilder.withPayload("test")
				.setHeader("stored_procedure_name", "123")
				.build());

		//This should cause an exception

		try {
			storedProcExecutor.executeStoredProcedure(
					MessageBuilder.withPayload("test")
						.setHeader("some_other_header", "123")
						.build());
		}
		catch (IllegalArgumentException e) {
			assertEquals("Unable to resolve Stored Procedure/Function name for the provided Expression 'headers['stored_procedure_name']'.", e.getMessage());
			return;
		}

		fail("IllegalArgumentException expected.");

	}

	@Test
	public void testStoredProcExecutorJdbcCallOperationsCache() throws Exception {

		final DataSource datasource = mock(DataSource.class);

		final StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();

		this.mockTheOperationsCache(storedProcExecutor);

		for (int i = 1; i <= 3; i++) {
			storedProcExecutor.executeStoredProcedure(
					MessageBuilder.withPayload("test")
						.setHeader("stored_procedure_name", "123")
						.build());
		}

		final CacheStats stats = storedProcExecutor.getJdbcCallOperationsCacheStatistics();
		LOGGER.info(stats);
		LOGGER.info(stats.totalLoadTime() / 1000 / 1000);

		assertEquals(stats.hitCount(), 2);
		assertEquals(stats.missCount(), 1);
		assertEquals(stats.loadCount(), 1);

	}

	@Test
	public void testSetJdbcCallOperationsCacheSize() throws Exception {

		final DataSource datasource = mock(DataSource.class);

		final StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource);

		storedProcExecutor.setJdbcCallOperationsCacheSize(0);

		final ExpressionFactoryBean efb = new ExpressionFactoryBean("headers['stored_procedure_name']");
		efb.afterPropertiesSet();
		final Expression expression = efb.getObject();

		storedProcExecutor.setStoredProcedureNameExpression(expression);
		storedProcExecutor.setBeanFactory(mock(BeanFactory.class));

		storedProcExecutor.afterPropertiesSet();

		this.mockTheOperationsCache(storedProcExecutor);

		for (int i = 1; i <= 10; i++) {
			storedProcExecutor.executeStoredProcedure(
					MessageBuilder.withPayload("test")
						.setHeader("stored_procedure_name", "123")
						.build());
		}

		final CacheStats stats = storedProcExecutor.getJdbcCallOperationsCacheStatistics();
		LOGGER.info(stats);
		assertEquals("Expected a cache misscount of 10", 10, stats.missCount());

	}

	private void mockTheOperationsCache(final StoredProcExecutor storedProcExecutor) {
		Object cache = TestUtils.getPropertyValue(storedProcExecutor, "jdbcCallOperationsCache.localCache");
		new DirectFieldAccessor(cache)
				.setPropertyValue("defaultLoader", new CacheLoader<String, SimpleJdbcCallOperations>() {
					@Override
					public SimpleJdbcCall load(String storedProcedureName) {
						return mock(SimpleJdbcCall.class);
					}
				});
	}

}
