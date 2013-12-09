/*
 * Copyright 2013 the original author or authors.
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

import java.io.IOException;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A SpEL {@link PropertyAccessor} that knows how to read on Jackson JSON objects.
 *
 * @author Eric Bottard
 */
public class JsonPropertyAccessor implements PropertyAccessor {

	/**
	 * The kind of types this can work with.
	 */
	private static final Class<?>[] SUPPORTED_CLASSES = new Class<?>[] { String.class, ToStringFriendlyJsonNode.class,
			ObjectNode.class, ArrayNode.class };

	// Note: ObjectMapper is thread-safe
	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = asJson(target);
		Integer index = maybeIndex(name);
		return ((index != null && container.has(index)) || container.has(name));
	}

	private ContainerNode<?> assertContainerNode(JsonNode json) throws AccessException {
		if (json instanceof ContainerNode) {
			return (ContainerNode<?>) json;
		}
		else {
			throw new AccessException("Can not act on json that is not a ContainerNode: "
					+ json.getClass().getSimpleName());
		}
	}

	private ContainerNode<?> asJson(Object target) throws AccessException {
		if (target instanceof ContainerNode) {
			return (ContainerNode<?>) target;
		}
		else if (target instanceof ToStringFriendlyJsonNode) {
			ToStringFriendlyJsonNode wrapper = (ToStringFriendlyJsonNode) target;
			return assertContainerNode(wrapper.node);
		}
		else if (target instanceof String) {
			try {
				JsonNode json = this.objectMapper.readTree((String) target);
				return assertContainerNode(json);
			}
			catch (JsonProcessingException e) {
				throw new AccessException("Exception while trying to deserialize String", e);
			}
			catch (IOException e) {
				throw new AccessException("Exception while trying to deserialize String", e);
			}
		}
		else {
			throw new IllegalStateException("Can't happen. Check SUPPORTED_CLASSES");
		}
	}

	/**
	 * Return an integer if the String property name can be parsed as an int, or null otherwise.
	 */
	private Integer maybeIndex(String name) {
		try {
			return Integer.valueOf(name);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = asJson(target);
		Integer index = maybeIndex(name);
		if (index != null && container.has(index)) {
			return new TypedValue(wrap(container.get(index)));
		}
		else {
			return new TypedValue(wrap(container.get(name)));
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
		Assert.notNull(objectMapper, "'objectMapper' cannot be null");
		this.objectMapper = objectMapper;
	}

	private ToStringFriendlyJsonNode wrap(JsonNode json) {
		return new ToStringFriendlyJsonNode(json);
	}

	public static class ToStringFriendlyJsonNode {
		private final JsonNode node;

		public ToStringFriendlyJsonNode(JsonNode node) {
			this.node = node;
		}

		@Override
		public String toString() {
			if (node.isValueNode()) {
				// This is to avoid quotes around a TextNode for example
				return node.asText();
			}
			else {
				return node.toString();
			}

		}
	}

}
