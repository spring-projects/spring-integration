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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.ExpressionException;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceUtils.ParameterExpressionEvaluator;
import org.springframework.util.Assert;

/**
*
* @author Gunnar Hillert
* @since 2.2
*
*/
class ExpressionEvaluatingParameterSource implements PositionSupportingParameterSource {

	private static final Log logger = LogFactory.getLog(ExpressionEvaluatingParameterSource.class);

	private static final Object ERROR = new Object();

	private final Object input;

	private volatile Map<String, Object> values = new HashMap<String, Object>();

	private final Map<String, String> parameterExpressions;

	private final List<JpaParameter> parameters;

	private final ParameterExpressionEvaluator expressionEvaluator;

	ExpressionEvaluatingParameterSource(Object input, List<JpaParameter> parameters, ParameterExpressionEvaluator expressionEvaluator) {

		this.input      = input;
		this.expressionEvaluator = expressionEvaluator;
		this.parameters = parameters;
		this.parameterExpressions = ExpressionEvaluatingParameterSourceUtils.convertExpressions(parameters);
		this.values.putAll(ExpressionEvaluatingParameterSourceUtils.convertStaticParameters(parameters));

	}

	public Object getValueByPosition(int position) {

		Assert.isTrue(position >= 0, "The position must be be non-negative.");

		if (position <= parameters.size()) {

			final JpaParameter parameter = parameters.get(position);

			if (parameter.getValue() != null) {
				return parameter.getValue();
			}

			if (parameter.getExpression() != null) {
				String expression = parameter.getExpression();

				if (input instanceof Collection<?>) {
					expression = "#root.![" + expression + "]";
				}

				final Object value = this.expressionEvaluator.evaluateExpression(expression, input);
				//FIXME values.put(paramName, value);
				if (logger.isDebugEnabled()) {
					logger.debug("Resolved expression " + expression + " to " + value);
				}
				return value;

			}

		}

		return null;

	}

	public Object getValue(String paramName) {
		if (values.containsKey(paramName)) {
			return values.get(paramName);
		}
		String expression = paramName;
		if (parameterExpressions.containsKey(expression)) {
			expression = parameterExpressions.get(expression);
		}
		if (input instanceof Collection<?>) {
			expression = "#root.![" + expression + "]";
		}
		final Object value = this.expressionEvaluator.evaluateExpression(expression, input);
		values.put(paramName, value);
		if (logger.isDebugEnabled()) {
			logger.debug("Resolved expression " + expression + " to " + value);
		}
		return value;
	}

	public boolean hasValue(String paramName) {
		try {
			final Object value = getValue(paramName);
			if (value == ERROR) {
				return false;
			}
		}
		catch (ExpressionException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not evaluate expression", e);
			}
			values.put(paramName, ERROR);
			return false;
		}
		return true;
	}
}
