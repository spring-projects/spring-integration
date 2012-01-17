package org.springframework.integration.jpa.support.parametersource;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.ExpressionException;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory.ParameterExpressionEvaluator;
import org.springframework.util.Assert;

class ExpressionEvaluatingParameterSource implements PositionSupportingParameterSource {

	private final static Log logger = LogFactory.getLog(ExpressionEvaluatingParameterSource.class);
	
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
		this.parameterExpressions = ExpressionEvaluatingParameterSourceFactory.convertExpressions(parameters);
		this.values.putAll(ExpressionEvaluatingParameterSourceFactory.convertStaticParameters(parameters));

	}

	public Object getValueByPosition(int position) throws IllegalArgumentException {
		
		Assert.isTrue(position >= 0, "The position must be be non-negative.");
		
		if (position <= parameters.size()) {
			
			JpaParameter parameter = parameters.get(position);
			
			if (parameter.getValue() != null) {
				return parameter.getValue();
			}
			
			if (parameter.getExpression() != null) {
				String expression = parameter.getExpression();
				
				if (input instanceof Collection<?>) {
					expression = "#root.![" + expression + "]";
				}
				
				Object value = this.expressionEvaluator.evaluateExpression(expression, input);
				//FIXME values.put(paramName, value);
				if (logger.isDebugEnabled()) {
					logger.debug("Resolved expression " + expression + " to " + value);
				}
				return value;
				
			}

		}

		return null;
		
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
		Object value = this.expressionEvaluator.evaluateExpression(expression, input);
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