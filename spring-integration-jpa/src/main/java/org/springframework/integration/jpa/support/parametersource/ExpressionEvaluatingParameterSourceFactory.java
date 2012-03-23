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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.util.AbstractExpressionEvaluator;
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
		expressionEvaluator.getEvaluationContext().setVariable("staticParameters", convertStaticParameters(parameters));
	
	}

	public PositionSupportingParameterSource createParameterSource(final Object input) {
		return new ExpressionEvaluatingParameterSource(input, this.parameters, expressionEvaluator);
	}

	/**
	 * Utility method that converts a Collection of {@link JpaParameter} to
	 * a Map containing only expression parameters.
	 *  
	 * @param jpaParameters Must not be null. 
	 * @return Map containing only the Expression bound parameters. Will never be null.
	 */
    public static Map<String, String> convertExpressions(Collection<JpaParameter> jpaParameters) {
    	
    	Assert.notNull(jpaParameters, "The Collection of jpaParameters must not be null.");
    	
    	for (JpaParameter parameter : jpaParameters) {
    		Assert.notNull(parameter, "'jpaParameters' must not contain null values.");
    	}
    	
    	final Map<String, String> staticParameters = new HashMap<String, String>();
    	
    	for (JpaParameter parameter : jpaParameters) {
    		if (parameter.getExpression() != null) {
    			staticParameters.put(parameter.getName(), parameter.getExpression());
    		}
    	}
    	
    	return staticParameters;
    }

	/**
	 * Utility method that converts a Collection of {@link JpaParameter} to
	 * a Map containing only static parameters.
	 *  
	 * @param jpaParameters Must not be null. 
	 * @return Map containing only the static parameters. Will never be null.
	 */
    public static Map<String, Object> convertStaticParameters(Collection<JpaParameter> jpaParameters) {
    	
    	Assert.notNull(jpaParameters, "The Collection of jpaParameters must not be null.");
    	
    	for (JpaParameter parameter : jpaParameters) {
    		Assert.notNull(parameter, "'jpaParameters' must not contain null values.");
    	}
    	
    	final Map<String, Object> staticParameters = new HashMap<String, Object>();
    	
    	for (JpaParameter parameter : jpaParameters) {
    		if (parameter.getValue() != null) {
    			staticParameters.put(parameter.getName(), parameter.getValue());
    		}
    	}
    	
    	return staticParameters;
    }
    
    public class ParameterExpressionEvaluator extends AbstractExpressionEvaluator {

		@Override
		public StandardEvaluationContext getEvaluationContext() {
			return super.getEvaluationContext();
		}

		@Override
		public Object evaluateExpression(String expression, Object input) {
			return super.evaluateExpression(expression, input);
		}
    	
    }
    
}
