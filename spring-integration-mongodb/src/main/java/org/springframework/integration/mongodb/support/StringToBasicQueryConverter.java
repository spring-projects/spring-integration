/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.mongodb.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link GenericConverter} implementation for converting {@link String} to {@link BasicQuery}.
 *
 * @since 4.3.6
 */
public class StringToBasicQueryConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
		convertiblePairs.add(new ConvertiblePair(String.class, BasicQuery.class));
		return convertiblePairs;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		if (ClassUtils.isAssignable(source.getClass(), String.class)) {
			return new BasicQuery((String) source);
		}
		else {
			throw new IllegalArgumentException("Expecting source object of type String, got - " + source.getClass().getName() + " - instead.");
		}
	}

}
