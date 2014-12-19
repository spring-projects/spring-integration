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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;

/**
 * Base class for json {@link PropertyAccessor}s.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public abstract class AbstractJsonPropertyAccessor implements PropertyAccessor {

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = asJson(target);
		Integer index = maybeIndex(name);
		return ((index != null && container.has(index)) || container.has(name));
	}

	protected ContainerNode<?> assertContainerNode(JsonNode json) throws AccessException {
		if (json instanceof ContainerNode) {
			return (ContainerNode<?>) json;
		}
		else {
			throw new AccessException("Can not act on json that is not a ContainerNode: "
					+ json.getClass().getSimpleName());
		}
	}

	abstract ContainerNode<?> asJson(Object target) throws AccessException;

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

	protected ToStringFriendlyJsonNode wrap(JsonNode json) {
		return new ToStringFriendlyJsonNode(json);
	}

	public static class ToStringFriendlyJsonNode {

		protected final JsonNode node;

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

		@Override
		public boolean equals(Object o) {
			return this == o
					|| (!(o == null || getClass() != o.getClass())
							&& this.node.equals(((ToStringFriendlyJsonNode) o).node));
		}

		@Override
		public int hashCode() {
			return this.node.toString().hashCode();
		}

	}

}
