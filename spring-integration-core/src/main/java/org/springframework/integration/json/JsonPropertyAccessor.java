/*
 * Copyright 2013-2018 the original author or authors.
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
import java.util.AbstractList;
import java.util.Iterator;

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
 * A SpEL {@link PropertyAccessor} that knows how to read properties from JSON objects.
 * Uses Jackson {@link JsonNode} API for nested properties access.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Paul Martin
 * @author Gary Russell
 *
 * @since 3.0
 */
public class JsonPropertyAccessor implements PropertyAccessor {

	/**
	 * The kind of types this can work with.
	 */
	private static final Class<?>[] SUPPORTED_CLASSES =
			new Class<?>[] {
					String.class,
					ToStringFriendlyJsonNode.class,
					ArrayNodeAsList.class,
					ObjectNode.class,
					ArrayNode.class
			};

	// Note: ObjectMapper is thread-safe
	private ObjectMapper objectMapper = new ObjectMapper();

	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' cannot be null");
		this.objectMapper = objectMapper;
	}

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = asJson(target);
		Integer index = maybeIndex(name);
		if (container instanceof ArrayNode) {
			return index != null;
		}
		else {
			return ((index != null && container.has(index)) || container.has(name));
		}
	}

	private ContainerNode<?> assertContainerNode(JsonNode json) throws AccessException {
		if (json instanceof ContainerNode) {
			return (ContainerNode<?>) json;
		}
		else {
			throw new AccessException(
					"Can not act on json that is not a ContainerNode: " + json.getClass().getSimpleName());
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
		else if (target instanceof ArrayNodeAsList) {
			ArrayNodeAsList wrapper = (ArrayNodeAsList) target;
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
			return typedValue(container.get(index));
		}
		else {
			return typedValue(container.get(name));
		}
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) {
		throw new UnsupportedOperationException("Write is not supported");
	}

	private static TypedValue typedValue(JsonNode json) {
		if (json == null) {
			return TypedValue.NULL;
		}
		else {
			return new TypedValue(wrap(json));
		}
	}

	public static WrappedJsonNode<?> wrap(JsonNode json) {
		if (json == null) {
			return null;
		}
		else if (json instanceof ArrayNode) {
			return new ArrayNodeAsList((ArrayNode) json);
		}
		else {
			return new ToStringFriendlyJsonNode(json);
		}
	}

	/**
	 * The base interface for wrapped {@link JsonNode}.
	 * @since 5.0
	 */
	public interface WrappedJsonNode<T extends JsonNode> {

		/**
		 * Return the wrapped {@link JsonNode}
		 * @return the wrapped JsonNode
		 */
		T getTarget();

	}

	/**
	 * A {@link WrappedJsonNode} implementation to represent {@link JsonNode} as string.
	 */
	public static class ToStringFriendlyJsonNode implements WrappedJsonNode<JsonNode> {

		private final JsonNode node;

		ToStringFriendlyJsonNode(JsonNode node) {
			this.node = node;
		}

		@Override
		public JsonNode getTarget() {
			return this.node;
		}

		@Override
		public String toString() {
			if (this.node == null) {
				return "null";
			}
			if (this.node.isValueNode()) {
				// This is to avoid quotes around a TextNode for example
				return this.node.asText();
			}
			else {
				return this.node.toString();
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ToStringFriendlyJsonNode that = (ToStringFriendlyJsonNode) o;
			return (this.node == that.node) || (this.node != null && this.node.equals(that.node));
		}

		@Override
		public int hashCode() {
			return this.node != null ? this.node.toString().hashCode() : 0;
		}

	}

	/**
	 * An {@link AbstractList} implementation around {@link ArrayNode} with {@link WrappedJsonNode} aspect.
	 * @since 5.0
	 */
	public static class ArrayNodeAsList extends AbstractList<WrappedJsonNode<?>>
			implements WrappedJsonNode<ArrayNode> {

		private final ArrayNode node;

		ArrayNodeAsList(ArrayNode node) {
			this.node = node;
		}

		@Override
		public ArrayNode getTarget() {
			return this.node;
		}

		@Override
		public WrappedJsonNode<?> get(int index) {
			return wrap(this.node.get(index));
		}

		@Override
		public int size() {
			return this.node.size();
		}

		@Override
		public Iterator<WrappedJsonNode<?>> iterator() {

			return new Iterator<WrappedJsonNode<?>>() {

				private final Iterator<JsonNode> delegate = ArrayNodeAsList.this.node.iterator();

				@Override
				public boolean hasNext() {
					return this.delegate.hasNext();
				}

				@Override
				public WrappedJsonNode<?> next() {
					return wrap(this.delegate.next());
				}

			};
		}

	}

}
