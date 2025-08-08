/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.expression;

import java.beans.Introspector;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.expression.PropertyAccessor;
import org.springframework.util.Assert;

/**
 * Utility class that keeps track of a Set of SpEL {@link PropertyAccessor}s
 * in order to register them with the "integrationEvaluationContext" upon initialization.
 * Accessors must be added before context refresh.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public class SpelPropertyAccessorRegistrar {

	private final Map<String, PropertyAccessor> propertyAccessors = new LinkedHashMap<String, PropertyAccessor>();

	public SpelPropertyAccessorRegistrar() {
	}

	/**
	 * Create an instance with the provided property accessors. Each accessor name
	 * will be the class simple name.
	 * @param propertyAccessors the accessors.
	 * @since 4.3.8
	 */
	public SpelPropertyAccessorRegistrar(PropertyAccessor... propertyAccessors) {
		Assert.notEmpty(propertyAccessors, "'propertyAccessors' must not be empty");
		for (PropertyAccessor propertyAccessor : propertyAccessors) {
			this.propertyAccessors.put(obtainAccessorKey(propertyAccessor), propertyAccessor);
		}
	}

	/**
	 * Create an instance with the provided named property accessors.
	 * @param propertyAccessors a map of name:accessor.
	 * @since 4.3.8
	 */
	public SpelPropertyAccessorRegistrar(Map<String, PropertyAccessor> propertyAccessors) {
		Assert.notEmpty(propertyAccessors, "'propertyAccessors' must not be empty");
		this.propertyAccessors.putAll(propertyAccessors);
	}

	/**
	 * Return the registered accessors.
	 * @return the map of name:accessor.
	 * @since 4.3.8
	 */
	public Map<String, PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	/**
	 * Add the provided named property accessor.
	 * @param name the name.
	 * @param propertyAccessor the accessor.
	 * @return this registrar.
	 * @since 4.3.8
	 */
	public SpelPropertyAccessorRegistrar add(String name, PropertyAccessor propertyAccessor) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.notNull(propertyAccessor, "'propertyAccessor' must not be null");
		this.propertyAccessors.put(name, propertyAccessor);
		return this;
	}

	/**
	 * Add the provided property accessors. Each accessor name
	 * will be the class simple name.
	 * @param propertyAccessors the accessors.
	 * @return this registrar.
	 * @since 4.3.8
	 */
	public SpelPropertyAccessorRegistrar add(PropertyAccessor... propertyAccessors) {
		Assert.notEmpty(propertyAccessors, "'propertyAccessors' must not be empty");
		for (PropertyAccessor propertyAccessor : propertyAccessors) {
			this.propertyAccessors.put(obtainAccessorKey(propertyAccessor), propertyAccessor);
		}
		return this;
	}

	private static String obtainAccessorKey(PropertyAccessor propertyAccessor) {
		return Introspector.decapitalize(propertyAccessor.getClass().getSimpleName());
	}

}
