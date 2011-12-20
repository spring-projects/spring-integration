/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.Test;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;

public class StoredProcExecutorTests {

	@Test
	public void testStoredProcExecutorWithNullDataSource() {

		try {
		    new StoredProcExecutor(null, "storedProcedureName");
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
		    new StoredProcExecutor(datasource, null);
		} catch (IllegalArgumentException e) {
			assertEquals("storedProcedureName must not be null and cannot be empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");
	}

	@Test
	public void testStoredProcExecutorWithEmptyProcedureName() {

		DataSource datasource = mock(DataSource.class);

		try {
		    new StoredProcExecutor(datasource, "      ");
		} catch (IllegalArgumentException e) {
			assertEquals("storedProcedureName must not be null and cannot be empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");
	}

	@Test
	public void testSetReturningResultSetRowMappersWithNullMap() {

		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

		Map<String, RowMapper<?>> rowmappers = new HashMap<String, RowMapper<?>>();

	    storedProcExecutor.setReturningResultSetRowMappers(rowmappers);

		//Should Successfully finish

	}

	@Test
	public void testSetSqlParameterSourceFactoryWithNullParameter() {
		DataSource datasource = mock(DataSource.class);
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

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
		StoredProcExecutor storedProcExecutor = new StoredProcExecutor(datasource, "storedProcedureName");

		try {
			storedProcExecutor.setProcedureParameters(null);
		} catch (IllegalArgumentException e) {
			assertEquals("procedureParameters must not be null or empty.", e.getMessage());
			return;
		}

		fail("Exception expected.");

	}

}
