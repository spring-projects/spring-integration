/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Abstraction of Procedure parameters allowing to provide static parameters
 * and SpEl Expression based parameters.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class ProcedureParameter {

	private String name;

	private Object value;

	private String expression;

	/**
	 * Instantiates a new Procedure Parameter.
	 * @param name Name of the procedure parameter, must not be null or empty
	 * @param value If null, the expression property must be set
	 * @param expression If null, the value property must be set
	 */
	public ProcedureParameter(String name, Object value, String expression) {
		Assert.hasText(name, "'name' must not be empty.");
		this.name = name;
		this.value = value;
		this.expression = expression;
	}

	/**
	 * Default constructor.
	 */
	public ProcedureParameter() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		return this.value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getExpression() {
		return this.expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	public String toString() {
		return "ProcedureParameter [name=" + this.name +
				", value=" + this.value +
				", expression=" + this.expression +
				"]";
	}

	/**
	 * Utility method that converts a Collection of {@link ProcedureParameter} to
	 * a Map containing only expression parameters.
	 * @param procedureParameters Must not be null.
	 * @return Map containing only the Expression bound parameters. Will never be null.
	 */
	public static Map<String, String> convertExpressions(Collection<ProcedureParameter> procedureParameters) {
		Assert.notNull(procedureParameters, "The Collection of procedureParameters must not be null.");
		for (ProcedureParameter parameter : procedureParameters) {
			Assert.notNull(parameter, "'procedureParameters' must not contain null values.");
		}

		Map<String, String> staticParameters = new HashMap<>();

		for (ProcedureParameter parameter : procedureParameters) {
			if (parameter.getExpression() != null) {
				staticParameters.put(parameter.getName(), parameter.getExpression());
			}
		}

		return staticParameters;
	}

	/**
	 * Utility method that converts a Collection of {@link ProcedureParameter} to
	 * a Map containing only static parameters.
	 * @param procedureParameters Must not be null.
	 * @return Map containing only the static parameters. Will never be null.
	 */
	public static Map<String, Object> convertStaticParameters(Collection<ProcedureParameter> procedureParameters) {
		Assert.notNull(procedureParameters, "The Collection of procedureParameters must not be null.");

		for (ProcedureParameter parameter : procedureParameters) {
			Assert.notNull(parameter, "'procedureParameters' must not contain null values.");
		}

		Map<String, Object> staticParameters = new HashMap<>();

		for (ProcedureParameter parameter : procedureParameters) {
			if (parameter.getValue() != null) {
				staticParameters.put(parameter.getName(), parameter.getValue());
			}
		}

		return staticParameters;
	}

}
