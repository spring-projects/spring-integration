/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * A default implementation of {@link SqlParameterSourceFactory} which creates an {@link SqlParameterSource} to
 * reference bean properties (or map keys) in its input.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class BeanPropertySqlParameterSourceFactory implements SqlParameterSourceFactory {

	private final Map<String, Object> staticParameters = new HashMap<>();

	/**
	 * If the input is a List or a Map, the output is a map parameter source, and in that case some static parameters
	 * can be added (default is empty). If the input is not a List or a Map then this value is ignored.
	 * @param staticParameters the static parameters to set
	 */
	public void setStaticParameters(Map<String, Object> staticParameters) {
		this.staticParameters.putAll(staticParameters);
	}

	@Override
	public SqlParameterSource createParameterSource(Object input) {
		return new StaticBeanPropertySqlParameterSource(input, this.staticParameters);
	}

	private static final class StaticBeanPropertySqlParameterSource extends AbstractSqlParameterSource {

		private final SqlParameterSource input;

		private final Map<String, Object> staticParameters;

		@SuppressWarnings("unchecked")
		StaticBeanPropertySqlParameterSource(Object input, Map<String, Object> staticParameters) {
			this.input =
					input instanceof Map
							? new MapSqlParameterSource((Map<String, Object>) input)
							: new BeanPropertySqlParameterSource(input);

			this.staticParameters = staticParameters;
		}

		@Override
		public @Nullable Object getValue(String paramName) throws IllegalArgumentException {
			return this.staticParameters.containsKey(paramName)
					? this.staticParameters.get(paramName)
					: this.input.getValue(paramName);
		}

		@Override
		public boolean hasValue(String paramName) {
			return this.staticParameters.containsKey(paramName) || this.input.hasValue(paramName);
		}

	}

}
