/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.support.parametersource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceUtils.ParameterExpressionEvaluator;
import org.springframework.util.Assert;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class ExpressionEvaluatingParameterSourceFactory implements ParameterSourceFactory {

	private volatile List<JpaParameter> parameters;
	private ParameterExpressionEvaluator expressionEvaluator = new ParameterExpressionEvaluator();

	public ExpressionEvaluatingParameterSourceFactory() {
		this.parameters = Collections.unmodifiableList(new ArrayList<JpaParameter>());
	}

	/**
	 * Define the (optional) parameter values.
	 *
	 * @param parameters the parameters to be set
	 */
	public void setParameters(List<JpaParameter> parameters) {

		Assert.notEmpty(parameters, "parameters must not be null or empty.");

		for (JpaParameter parameter : parameters) {
			Assert.notNull(parameter, "The provided list (parameters) cannot contain null values.");
		}

		this.parameters = parameters;
		expressionEvaluator.getEvaluationContext().setVariable("staticParameters",
				ExpressionEvaluatingParameterSourceUtils.convertStaticParameters(parameters));

	}

	public PositionSupportingParameterSource createParameterSource(final Object input) {
		return new ExpressionEvaluatingParameterSource(input, this.parameters, expressionEvaluator);
	}

}
