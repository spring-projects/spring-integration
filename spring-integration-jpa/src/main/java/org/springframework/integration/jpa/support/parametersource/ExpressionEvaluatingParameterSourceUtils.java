/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support.parametersource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.util.Assert;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
final class ExpressionEvaluatingParameterSourceUtils {

	private ExpressionEvaluatingParameterSourceUtils() {
	}

	/**
	 * Utility method that converts a Collection of {@link JpaParameter} to
	 * a Map containing only static parameters.
	 * @param jpaParameters Must not be null.
	 * @return Map containing only the static parameters. Will never be null.
	 */
	public static Map<String, Object> convertStaticParameters(Collection<JpaParameter> jpaParameters) {
		Assert.notNull(jpaParameters, "The Collection of jpaParameters must not be null.");

		for (JpaParameter parameter : jpaParameters) {
			Assert.notNull(parameter, "'jpaParameters' must not contain null values.");
		}

		final Map<String, Object> staticParameters = new HashMap<>();

		for (JpaParameter parameter : jpaParameters) {
			String name = parameter.getName();
			Object value = parameter.getValue();
			if (name != null && value != null) {
				staticParameters.put(name, value);
			}
		}

		return staticParameters;
	}

}
