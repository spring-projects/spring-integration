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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A SpEL {@link PropertyAccessor} that knows how to read and write on Jackson JSON objects.
 * 
 * @author Eric Bottard
 */
public class JsonPropertyAccessor implements PropertyAccessor {

	/**
	 * The kind of types this can work with.
	 */
	private static final Class<?>[] SUPPORTED_CLASSES = new Class[] { ObjectNode.class, ArrayNode.class };

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = (ContainerNode<?>) target;
		Integer index = maybeIndex(name);
		return (index != null && container.has(index) || container.has(name));
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
		ContainerNode<?> container = (ContainerNode<?>) target;
		Integer index = maybeIndex(name);
		if (index != null && container.has(index)) {
			return new TypedValue(container.get(index));
		}
		else {
			return new TypedValue(container.get(name));
		}
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		if (target instanceof ArrayNode) {
			// TODO: consider array auto-expansion?
			ArrayNode new_name = (ArrayNode) target;
			Integer index = maybeIndex(name);
			return new_name.has(index);
		}
		else if (target instanceof ObjectNode) {
			return true;
		}
		else {
			throw new AssertionError("Can't happen");
		}
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		ContainerNode<?> container = (ContainerNode<?>) target;
		JsonNode wrapped = wrap(newValue, container);

		if (target instanceof ArrayNode) {
			ArrayNode array = (ArrayNode) target;
			array.set(Integer.parseInt(name), wrapped);
		}
		else if (target instanceof ObjectNode) {
			ObjectNode hash = (ObjectNode) target;
			hash.set(name, wrapped);
		}
		else {
			throw new AccessException(String.format("Can't set property '%s' on object '%s'", name, target));
		}
	}

	private JsonNode wrap(Object raw, ContainerNode<?> factory) {
		if (raw instanceof JsonNode) {
			return (JsonNode) raw;
		}
		else if (raw instanceof String) {
			return factory.textNode((String) raw);
		}
		else if (raw instanceof Byte) {
			return factory.numberNode((Byte) raw);
		}
		else if (raw instanceof Short) {
			return factory.numberNode((Short) raw);
		}
		else if (raw instanceof Character) {
			return factory.textNode(raw.toString());
		}
		else if (raw instanceof Integer) {
			return factory.numberNode((Integer) raw);
		}
		else if (raw instanceof Long) {
			return factory.numberNode((Long) raw);
		}
		else if (raw instanceof Float) {
			return factory.numberNode((Float) raw);
		}
		else if (raw instanceof Double) {
			return factory.numberNode((Double) raw);
		}
		else if (raw instanceof BigInteger) {
			return factory.numberNode((BigInteger) raw);
		}
		else if (raw instanceof BigDecimal) {
			return factory.numberNode((BigDecimal) raw);
		}
		else if (raw == null) {
			return factory.nullNode();
		}
		else if (raw instanceof Object[]) {
			Object[] array = (Object[]) raw;
			ArrayNode result = factory.arrayNode();
			for (Object o : array) {
				result.add(wrap(o, factory));
			}
			return result;
		}
		else if (raw instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, ?> map = (Map<String, ?>) raw;
			ObjectNode result = factory.objectNode();
			for (String key : map.keySet()) {
				result.set(key, wrap(map.get(key), factory));
			}
			return result;
		}
		else
			throw new IllegalArgumentException();
	}
}