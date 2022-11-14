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

package org.springframework.integration.jdbc.storedproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ProcedureParameterTests {

	@Test
	public void testProcedureParameterStringObjectString() {

		try {
			new ProcedureParameter(null, "value", "expression");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'name' must not be empty.");
			return;
		}

		fail("Expected Exception");
	}

	@Test
	public void testConvertExpressions() {
		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		Map<String, String> expressionParameters =
				ProcedureParameter.convertExpressions(procedureParameters);

		assertThat(expressionParameters.size() == 2).as("Expected 2 expression parameters.").isTrue();
	}

	@Test
	public void testConvertStaticParameters() {

		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		Map<String, Object> staticParameters =
				ProcedureParameter.convertStaticParameters(procedureParameters);

		assertThat(staticParameters.size() == 3).as("Expected 3 static parameters.").isTrue();
	}

	@Test
	public void testConvertStaticParametersWithNullValueInList() {

		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		procedureParameters.add(1, null);

		try {
			ProcedureParameter.convertStaticParameters(procedureParameters);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'procedureParameters' must not contain null values.");
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
			assertThat(e.getMessage()).isEqualTo("'procedureParameters' must not contain null values.");
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
