/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.json;

import org.springframework.expression.AccessException;
import org.springframework.expression.PropertyAccessor;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A SpEL {@link PropertyAccessor} that knows how to read on Jackson JSON objects.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @since 3.0
 */
public class JsonNodePropertyAccessor extends AbstractJsonPropertyAccessor {

	/**
	 * The kind of types this can work with.
	 */
	private static final Class<?>[] SUPPORTED_CLASSES = new Class<?>[] {ToStringFriendlyJsonNode.class,
			ObjectNode.class, ArrayNode.class};

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	ContainerNode<?> asJson(Object target) throws AccessException {
		if (target instanceof ContainerNode) {
			return (ContainerNode<?>) target;
		}
		else if (target instanceof ToStringFriendlyJsonNode) {
			ToStringFriendlyJsonNode wrapper = (ToStringFriendlyJsonNode) target;
			return assertContainerNode(wrapper.node);
		}
		else {
			throw new IllegalStateException("Can't happen. Check SUPPORTED_CLASSES");
		}
	}

}
