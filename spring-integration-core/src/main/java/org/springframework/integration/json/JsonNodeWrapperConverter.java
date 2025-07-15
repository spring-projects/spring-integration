/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.json;

import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.integration.json.JacksonPropertyAccessor.JsonNodeWrapper;

/**
 * The {@link org.springframework.core.convert.converter.Converter} implementation for the conversion
 * of {@link JsonNodeWrapper} to {@link JsonNode},
 * when the {@link JsonNodeWrapper} can be a result of the expression
 * for JSON in case of the {@link JacksonPropertyAccessor} usage.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class JsonNodeWrapperConverter implements GenericConverter {

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
