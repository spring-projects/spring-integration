/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jdbc.storedproc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ProcedureParameterTests {

	@Test
	public void testProcedureParameterStringObjectString() {

		try {
			new ProcedureParameter(null, "value", "expression");
		}
		catch (IllegalArgumentException e) {
			assertEquals("'name' must not be empty.", e.getMessage());
			return;
		}

		fail("Expected Exception");
	}

	@Test
	public void testConvertExpressions() {
		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		Map<String, String> expressionParameters =
				ProcedureParameter.convertExpressions(procedureParameters);

        assertTrue("Expected 2 expression parameters.", expressionParameters.size() == 2);
	}

	@Test
	public void testConvertStaticParameters() {

		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		Map<String, Object> staticParameters =
				ProcedureParameter.convertStaticParameters(procedureParameters);

        assertTrue("Expected 3 static parameters.", staticParameters.size() == 3);
	}

	@Test
	public void testConvertStaticParametersWithNullValueInList() {

		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		procedureParameters.add(1, null);

		try {
			ProcedureParameter.convertStaticParameters(procedureParameters);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'procedureParameters' must not contain null values.", e.getMessage());
			return;
		}

		fail("Expected Exception");

	}

	@Test
	public void testConvertExpressionParametersWithNullValueInList() {

		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		procedureParameters.add(1, null);

		try {
			ProcedureParameter.convertExpressions(procedureParameters);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'procedureParameters' must not contain null values.", e.getMessage());
			return;
		}

		fail("Expected Exception");

	}

	private List<ProcedureParameter> getProcedureParameterList() {

		List<ProcedureParameter> procedureParameterList = new ArrayList<ProcedureParameter>();
		procedureParameterList.add(new ProcedureParameter("param1", "value1", null));
		procedureParameterList.add(new ProcedureParameter("param2", "value1", null));
		procedureParameterList.add(new ProcedureParameter("param3", "value1", null));
		procedureParameterList.add(new ProcedureParameter("param4", null, "expression1"));
		procedureParameterList.add(new ProcedureParameter("param5", null, "expression2"));

		return procedureParameterList;
	}

}
