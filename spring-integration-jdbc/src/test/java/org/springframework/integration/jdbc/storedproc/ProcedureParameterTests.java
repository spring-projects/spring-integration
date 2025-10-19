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

package org.springframework.integration.jdbc.storedproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class ProcedureParameterTests {

	@Test
	public void testProcedureParameterStringObjectString() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ProcedureParameter(null, "value", "expression"))
				.withMessage("'name' must not be empty.");
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

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ProcedureParameter.convertStaticParameters(procedureParameters))
				.withMessage("'procedureParameters' must not contain null values.");
	}

	@Test
	public void testConvertExpressionParametersWithNullValueInList() {

		List<ProcedureParameter> procedureParameters = getProcedureParameterList();
		procedureParameters.add(1, null);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ProcedureParameter.convertExpressions(procedureParameters))
				.withMessage("'procedureParameters' must not contain null values.");
	}

	private List<ProcedureParameter> getProcedureParameterList() {

		List<ProcedureParameter> procedureParameterList = new ArrayList<>();
		procedureParameterList.add(new ProcedureParameter("param1", "value1", null));
		procedureParameterList.add(new ProcedureParameter("param2", "value1", null));
		procedureParameterList.add(new ProcedureParameter("param3", "value1", null));
		procedureParameterList.add(new ProcedureParameter("param4", null, "expression1"));
		procedureParameterList.add(new ProcedureParameter("param5", null, "expression2"));

		return procedureParameterList;
	}

}
