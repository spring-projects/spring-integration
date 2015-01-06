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

import java.util.Arrays;
import java.util.Collection;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.integration.support.CompositePropertyAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A SpEL {@link CompositePropertyAccessor} that wraps node and String accessors.
 *
 * Also delegates to the appropriate accessor for backwards compatibility if used
 * programmatically.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class JsonPropertyAccessor implements CompositePropertyAccessor {

	private final JsonNodePropertyAccessor nodeAccessor = new JsonNodePropertyAccessor();

	private final JsonStringPropertyAccessor stringAccessor = new JsonStringPropertyAccessor();

	@Override
	public Collection<PropertyAccessor> accessors() {
		return Arrays.<PropertyAccessor>asList(nodeAccessor, stringAccessor);
	}

	/**
	 * The kind of types this can work with.
	 */
	private static final Class<?>[] SUPPORTED_CLASSES = new Class<?>[] { String.class,
			JsonNodePropertyAccessor.ToStringFriendlyJsonNode.class, ObjectNode.class, ArrayNode.class };

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		if (target instanceof String) {
			return this.stringAccessor.canRead(context, target, name);
		}
		else {
			return this.nodeAccessor.canRead(context, target, name);
		}
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		if (target instanceof String) {
			return this.stringAccessor.read(context, target, name);
		}
		else {
			return this.nodeAccessor.read(context, target, name);
		}
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new UnsupportedOperationException("Write is not supported");
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.stringAccessor.setObjectMapper(objectMapper);
	}

}
