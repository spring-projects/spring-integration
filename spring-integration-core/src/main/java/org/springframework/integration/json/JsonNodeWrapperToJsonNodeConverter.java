/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.json;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.integration.json.JsonPropertyAccessor.JsonNodeWrapper;
import org.springframework.lang.Nullable;

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
 */
public class JsonNodeWrapperToJsonNodeConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(JsonNodeWrapper.class, JsonNode.class));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source != null) {
			return targetType.getObjectType().cast(((JsonNodeWrapper<?>) source).getRealNode());
		}
		return null;
	}

}
