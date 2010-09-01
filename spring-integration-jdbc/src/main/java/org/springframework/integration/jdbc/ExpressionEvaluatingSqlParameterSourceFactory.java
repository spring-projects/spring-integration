/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.ExpressionException;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * An implementation of {@link SqlParameterSourceFactory} which creates an {@link SqlParameterSource} that evaluates
 * Spring EL expressions.  In addition the user can supply static parameters that always take precedence.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class ExpressionEvaluatingSqlParameterSourceFactory extends AbstractExpressionEvaluator implements SqlParameterSourceFactory {

	private final static Log logger = LogFactory.getLog(ExpressionEvaluatingSqlParameterSourceFactory.class);

	private static final Object ERROR = new Object();

	private Map<String, ?> staticParameters;

	public ExpressionEvaluatingSqlParameterSourceFactory() {
		this.staticParameters = Collections.unmodifiableMap(new HashMap<String, Object>());
	}

	/**
	 * If the input is a List or a Map, the output is a map parameter source, and in that case some static parameters
	 * can be added (default is empty). If the input is not a List or a Map then this value is ignored.
	 * 
	 * @param staticParameters the static parameters to set
	 */
	public void setStaticParameters(Map<String, ?> staticParameters) {
		this.staticParameters = staticParameters;
	}

	public SqlParameterSource createParameterSource(final Object input) {
		SqlParameterSource toReturn = new ExpressionEvaluatingSqlParameterSource(input, staticParameters);
		return toReturn;
	}

	private class ExpressionEvaluatingSqlParameterSource extends AbstractSqlParameterSource {

		private final Object input;

		private Map<String, Object> values = new ConcurrentHashMap<String, Object>();

		private ExpressionEvaluatingSqlParameterSource(Object input, Map<String, ?> staticParameters) {
			this.input = input;
			this.values.putAll(staticParameters);
		}

		public Object getValue(String paramName) throws IllegalArgumentException {
			if (values.containsKey(paramName)) {
				return values.get(paramName);
			}
			String expression = paramName;
			if (input instanceof Collection<?>) {
				expression = "#root.!["+paramName+"]";
			}
			Object value = evaluateExpression(expression, input);
			values.put(paramName, value);
			return value;
		}

		public boolean hasValue(String paramName) {
			try {
				Object value = getValue(paramName);
				if (value==ERROR) {
					return false; 
				}
			} catch (ExpressionException e) {
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
