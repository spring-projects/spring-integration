/*
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.json;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * The {@link org.springframework.core.convert.converter.Converter} implementation for the conversion
 * of {@link JsonPropertyAccessor.JsonNodeWrapper} to {@link JsonNode},
 * when the {@link JsonPropertyAccessor.JsonNodeWrapper} can be a result of the expression
 * for JSON in case of the {@link JsonPropertyAccessor} usage.
 *
 * @author Pierre Lakreb
 * @author Artem Bilan
 *
 * @since 5.5
 * @deprecated Since 7.0 in favor of {@link JsonNodeWrapperConverter} for Jackson 3.
 */
@SuppressWarnings("removal")
@Deprecated(forRemoval = true, since = "7.0")
public class JsonNodeWrapperToJsonNodeConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(JsonPropertyAccessor.JsonNodeWrapper.class, JsonNode.class));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source != null) {
			return targetType.getObjectType().cast(((JsonPropertyAccessor.JsonNodeWrapper<?>) source).getRealNode());
		}
		return null;
	}

}
