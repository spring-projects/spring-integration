/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.expression;

import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.PropertyAccessor;
import org.springframework.util.Assert;

/**
 * Utility class that keeps track of a Set of SpEL {@link PropertyAccessor}s
 * in order to register them with the "integrationEvaluationContext" upon initialization.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 */
public class SpelPropertyAccessorRegistrar {

	private final Map<String, PropertyAccessor> propertyAccessors = new HashMap<String, PropertyAccessor>();

	public SpelPropertyAccessorRegistrar() {
	}

	public SpelPropertyAccessorRegistrar(PropertyAccessor... propertyAccessors) {
		Assert.notEmpty(propertyAccessors, "'propertyAccessors' must not be empty");
		for (PropertyAccessor propertyAccessor : propertyAccessors) {
			this.propertyAccessors.put(propertyAccessors.getClass().getSimpleName(), propertyAccessor);
		}
	}

	public SpelPropertyAccessorRegistrar(Map<String, PropertyAccessor> propertyAccessors) {
		Assert.notEmpty(propertyAccessors, "'propertyAccessors' must not be empty");
		this.propertyAccessors.putAll(propertyAccessors);
	}

	public Map<String, PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	public SpelPropertyAccessorRegistrar add(String name, PropertyAccessor propertyAccessor) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.notNull(propertyAccessor, "'propertyAccessor' must not be null");
		this.propertyAccessors.put(name, propertyAccessor);
		return this;
	}

	public SpelPropertyAccessorRegistrar add(PropertyAccessor... propertyAccessors) {
		Assert.notEmpty(propertyAccessors, "'propertyAccessors' must not be empty");
		for (PropertyAccessor propertyAccessor : propertyAccessors) {
			this.propertyAccessors.put(propertyAccessors.getClass().getSimpleName(), propertyAccessor);
		}
		return this;
	}

}
