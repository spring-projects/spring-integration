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

package org.springframework.integration.jpa.support.parametersource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.util.Assert;

/**
 * A SpEL expression based {@link ParameterSourceFactory} implementation.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class ExpressionEvaluatingParameterSourceFactory implements ParameterSourceFactory {

	private static final Log LOGGER = LogFactory.getLog(ExpressionEvaluatingParameterSourceFactory.class);

	private static final Object ERROR = new Object();

	private final ParameterExpressionEvaluator expressionEvaluator = new ParameterExpressionEvaluator();

	private final List<JpaParameter> parameters = new ArrayList<>();

	public ExpressionEvaluatingParameterSourceFactory() {
		this(null);
	}

	public ExpressionEvaluatingParameterSourceFactory(@Nullable BeanFactory beanFactory) {
		if (beanFactory != null) {
			this.expressionEvaluator.setBeanFactory(beanFactory);
		}
	}

	/**
	 * Define the (optional) parameter values.
	 * @param parameters the parameters to be set
	 */
	public void setParameters(List<JpaParameter> parameters) {
		Assert.notEmpty(parameters, "parameters must not be null or empty.");
		Assert.noNullElements(parameters, "The provided list (parameters) cannot contain null values.");

		this.parameters.addAll(parameters);
		this.expressionEvaluator.getEvaluationContext().setVariable("staticParameters",
				ExpressionEvaluatingParameterSourceUtils.convertStaticParameters(parameters));

	}

	@Override
	public PositionSupportingParameterSource createParameterSource(final Object input) {
		return new ExpressionEvaluatingParameterSource(input, this.parameters, this.expressionEvaluator);
	}

	protected class ExpressionEvaluatingParameterSource implements PositionSupportingParameterSource {

		private final Object input;

		private final Map<String, Object> values = new HashMap<>();

		private final List<JpaParameter> parameters;

		private final Map<String, JpaParameter> parametersMap;

		private final ParameterExpressionEvaluator expressionEvaluator;

		protected ExpressionEvaluatingParameterSource(Object input, List<JpaParameter> parameters,
				ParameterExpressionEvaluator expressionEvaluator) {

			this.input = input;
			this.expressionEvaluator = expressionEvaluator;
			this.parameters = parameters;
			this.parametersMap = new HashMap<>(parameters.size());
			for (JpaParameter parameter : parameters) {
				String name = parameter.getName();
				if (name != null) {
					this.parametersMap.put(name, parameter);
				}
			}
			this.values.putAll(ExpressionEvaluatingParameterSourceUtils.convertStaticParameters(parameters));
		}

		@Override
		public @Nullable Object getValueByPosition(int position) {
			Assert.isTrue(position > 0, "The position must be non-negative.");
			if (position <= this.parameters.size()) {
				JpaParameter parameter = this.parameters.get(position - 1);
				String parameterName = parameter.getName();
				if (parameterName != null) {
					return getValue(parameterName);
				}
				else {
					return obtainParameterValue(parameter);
				}
			}
			return null;
		}

		@Override
		public @Nullable Object getValue(String paramName) {
			return this.values.computeIfAbsent(paramName,
					(key) -> {
						JpaParameter jpaParameter =
								this.parametersMap.computeIfAbsent(paramName,
										(name) -> {
											JpaParameter parameter = new JpaParameter(paramName, null, paramName);
											ExpressionEvaluatingParameterSourceFactory.this.parameters.add(parameter);
											return parameter;
										});
						return obtainParameterValue(jpaParameter);
					});
		}

		private @Nullable Object obtainParameterValue(JpaParameter jpaParameter) {
			Object value = jpaParameter.getValue();
			if (value == null) {
				Assert.notNull(jpaParameter.getExpression(),
						() -> "One of the 'value' or 'expression' must be provided for 'JpaParameter': " + jpaParameter);
				Expression expression;
				if (this.input instanceof Collection<?>) {
					expression = jpaParameter.getProjectionExpression();
				}
				else {
					expression = jpaParameter.getSpelExpression();
				}
				if (expression != null) {
					value = this.expressionEvaluator.evaluateExpression(expression, this.input);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Resolved expression " + expression + " to " + value);
					}
				}
			}
			return value;
		}

		@Override
		public boolean hasValue(String paramName) {
			try {
				final Object value = getValue(paramName);
				if (ERROR.equals(value)) {
					return false;
				}
			}
			catch (ExpressionException e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Could not evaluate expression", e);
				}
				this.values.put(paramName, ERROR);
				return false;
			}
			return true;
		}

	}

}
