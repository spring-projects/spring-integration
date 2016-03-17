/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.config;

import java.util.Map;

import org.springframework.expression.PropertyAccessor;

/**
 * Utility class that keeps track of a Set of SpEL {@link PropertyAccessor}s
 * in order to register them with the "integrationEvaluationContext" upon initialization.
 *
 * @author Artem Bilan
 * @since 3.0
 */
class SpelPropertyAccessorRegistrar {

	private final Map<String, PropertyAccessor> propertyAccessors;

	SpelPropertyAccessorRegistrar(Map<String, PropertyAccessor> propertyAccessors) {
		this.propertyAccessors = propertyAccessors;
	}

	Map<String, PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	void addPropertyAccessor(String name, PropertyAccessor propertyAccessor) {
		this.propertyAccessors.put(name, propertyAccessor);
	}

}
