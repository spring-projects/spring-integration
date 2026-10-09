/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.TypedValue;

/**
 * A SpEL {@link IndexAccessor} that knows how to read indexes from JSON arrays, using
 * Jackson's {@link ArrayNode} API.
 *
 * <p>Supports indexes supplied as an integer literal &mdash; for example, {@code myJsonArray[1]}.
 * Also supports negative indexes &mdash; for example, {@code myJsonArray[-1]} which equates
 * to {@code myJsonArray[myJsonArray.length - 1]}. Furthermore, {@code null} is returned for
 * any index that is out of bounds (see {@link ArrayNode#get(int)} for details).
 *
 * @author Sam Brannen
 *
 * @since 6.4
 *
 * @see JsonPropertyAccessor
 *
 * @deprecated Since 7.0 in favor of {@link JacksonIndexAccessor} for Jackson 3.
 */
@Deprecated(forRemoval = true, since = "7.0")
@SuppressWarnings({ "removal", "NullAway"})
public class JsonIndexAccessor implements IndexAccessor {

	private static final Class<?>[] SUPPORTED_CLASSES = { ArrayNode.class };

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, Object index) {
		return (target instanceof ArrayNode && index instanceof Integer);
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, Object index) throws AccessException {
		ArrayNode arrayNode = (ArrayNode) target;
		Integer intIndex = (Integer) index;
		if (intIndex < 0) {
			// negative index: get from the end of array, for compatibility with JsonPropertyAccessor.ArrayNodeAsList.
			intIndex = arrayNode.size() + intIndex;
		}
		return JsonPropertyAccessor.typedValue(arrayNode.get(intIndex));
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, Object index) {
		return false;
	}

	@Override
	public void write(EvaluationContext context, Object target, Object index, @Nullable Object newValue) {
		throw new UnsupportedOperationException("Write is not supported");
	}

}
