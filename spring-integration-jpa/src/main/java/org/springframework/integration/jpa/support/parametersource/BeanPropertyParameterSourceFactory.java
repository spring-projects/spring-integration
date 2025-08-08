/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support.parametersource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.2
 *
 */
public class BeanPropertyParameterSourceFactory implements ParameterSourceFactory {

	private volatile Map<String, Object> staticParameters;

	public BeanPropertyParameterSourceFactory() {
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
	public ParameterSource createParameterSource(Object input) {
		return new StaticBeanPropertyParameterSource(input, this.staticParameters);
	}

	private static final class StaticBeanPropertyParameterSource implements
			ParameterSource {

		private final BeanPropertyParameterSource input;

		private final Map<String, Object> staticParameters;

		StaticBeanPropertyParameterSource(Object input, Map<String, Object> staticParameters) {
			this.input = new BeanPropertyParameterSource(input);
			this.staticParameters = staticParameters;
		}

		@Override
		public Object getValue(String paramName) {
			return this.staticParameters.containsKey(paramName) ? this.staticParameters.get(paramName) : this.input
					.getValue(paramName);
		}

		@Override
		public boolean hasValue(String paramName) {
			return this.staticParameters.containsKey(paramName) || this.input.hasValue(paramName);
		}

	}

}
