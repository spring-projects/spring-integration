/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * A default implementation of {@link SqlParameterSourceFactory} which creates an {@link SqlParameterSource} to
 * reference bean properties in its input.
 *
 * @author Dave Syer
 * @since 2.0
 */
public class BeanPropertySqlParameterSourceFactory implements SqlParameterSourceFactory {

	private volatile Map<String, Object> staticParameters;

	public BeanPropertySqlParameterSourceFactory() {
		this.staticParameters = Collections.unmodifiableMap(new HashMap<String, Object>());
	}

	/**
	 * If the input is a List or a Map, the output is a map parameter source, and in that case some static parameters
	 * can be added (default is empty). If the input is not a List or a Map then this value is ignored.
	 *
	 * @param staticParameters the static parameters to set
	 */
	public void setStaticParameters(Map<String, Object> staticParameters) {
		this.staticParameters = staticParameters;
	}

	@Override
	public SqlParameterSource createParameterSource(Object input) {
		SqlParameterSource toReturn = new StaticBeanPropertySqlParameterSource(input, this.staticParameters);
		return toReturn;
	}

	private static final class StaticBeanPropertySqlParameterSource extends AbstractSqlParameterSource implements
			SqlParameterSource {

		private final BeanPropertySqlParameterSource input;

		private final Map<String, Object> staticParameters;

		private StaticBeanPropertySqlParameterSource(Object input, Map<String, Object> staticParameters) {
			this.input = new BeanPropertySqlParameterSource(input);
			this.staticParameters = staticParameters;
		}

		@Override
		public Object getValue(String paramName) throws IllegalArgumentException {
			return this.staticParameters.containsKey(paramName) ? this.staticParameters.get(paramName) : this.input
					.getValue(paramName);
		}

		@Override
		public boolean hasValue(String paramName) {
			return this.staticParameters.containsKey(paramName) || this.input.hasValue(paramName);
		}

	}

}
