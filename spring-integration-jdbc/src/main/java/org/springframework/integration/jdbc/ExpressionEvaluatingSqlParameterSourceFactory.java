/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.ExpressionException;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * An implementation of {@link SqlParameterSourceFactory} which creates an {@link SqlParameterSource} that evaluates
 * Spring EL expressions. In addition the user can supply static parameters that always take precedence.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ExpressionEvaluatingSqlParameterSourceFactory extends AbstractExpressionEvaluator implements
		SqlParameterSourceFactory {

	private final static Log logger = LogFactory.getLog(ExpressionEvaluatingSqlParameterSourceFactory.class);

	private static final Object ERROR = new Object();

	private volatile Map<String, ?> staticParameters;

	private volatile Map<String, String> parameterExpressions;

	public ExpressionEvaluatingSqlParameterSourceFactory() {
		this.staticParameters = Collections.unmodifiableMap(new HashMap<String, Object>());
		this.parameterExpressions = Collections.unmodifiableMap(new HashMap<String, String>());
	}

	/**
	 * Define some static parameter values. These take precedence over those defined as expressions in the
	 * {@link #setParameterExpressions(Map) parameterExpressions}, so a parameter in the query will be filled from here
	 * first, and then from the expressions.
	 *
	 * @param staticParameters the static parameters to set
	 */
	public void setStaticParameters(Map<String, ?> staticParameters) {
		this.staticParameters = staticParameters;
		getEvaluationContext().setVariable("staticParameters", staticParameters);
	}

	/**
	 * Optionally maps parameter names to explicit expressions. The named parameter support in Spring is limited to
	 * simple parameter names with no special characters, so this feature allows you to specify a simple name in the SQL
	 * query and then have it translated into an expression at runtime. The target of the expression depends on the
	 * context: generally in an outbound setting it is a Message, and in an inbound setting it is a result set row (a
	 * Map or a domain object if a RowMapper has been provided). The {@link #setStaticParameters(Map) static parameters}
	 * can be referred to in an expression using the variable <code>#staticParameters</code>, for example:
	 * <p>
	 * <table>
	 * <caption>Parameter Expressions Samples</caption>
	 * <tr>
	 * <th><b>Key</b></th>
	 * <th><b>Value (Expression)</b></th>
	 * <th><b>Example SQL</b></th>
	 * </tr>
	 * <tr>
	 * <td>id</td>
	 * <td>{@code payload.businessKey}</td>
	 * <td>{@code select * from items where id=:id}</td>
	 * </tr>
	 * <tr>
	 * <td>date</td>
	 * <td>{@code headers['timestamp']}</td>
	 * <td>{@code select * from items where created>:date}</td>
	 * </tr>
	 * <tr>
	 * <td>key</td>
	 * <td>{@code #staticParameters['foo'].toUpperCase()}</td>
	 * <td>{@code select * from items where name=:key}</td>
	 * </tr>
	 * </table>
	 * <p>
	 *
	 * @param parameterExpressions the parameter expressions to set
	 */
	public void setParameterExpressions(Map<String, String> parameterExpressions) {
		this.parameterExpressions = parameterExpressions;
	}

	public SqlParameterSource createParameterSource(final Object input) {
		SqlParameterSource toReturn = new ExpressionEvaluatingSqlParameterSource(input, staticParameters,
				parameterExpressions);
		return toReturn;
	}

	private final class ExpressionEvaluatingSqlParameterSource extends AbstractSqlParameterSource {

		private final Object input;

		private volatile Map<String, Object> values = new HashMap<String, Object>();

		private final Map<String, String> parameterExpressions;

		private ExpressionEvaluatingSqlParameterSource(Object input, Map<String, ?> staticParameters,
				Map<String, String> parameterExpressions) {
			this.input = input;
			this.parameterExpressions = parameterExpressions;
			this.values.putAll(staticParameters);
		}

		public Object getValue(String paramName) throws IllegalArgumentException {
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
			Object value = evaluateExpression(expression, input);
			values.put(paramName, value);
			if (logger.isDebugEnabled()) {
				logger.debug("Resolved expression " + expression + " to " + value);
			}
			return value;
		}

		public boolean hasValue(String paramName) {
			try {
				Object value = getValue(paramName);
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

}
