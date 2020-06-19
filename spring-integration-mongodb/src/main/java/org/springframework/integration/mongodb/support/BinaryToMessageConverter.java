/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.mongodb.support;

import org.bson.types.Binary;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 5.0
 */
@ReadingConverter
public class BinaryToMessageConverter implements Converter<Binary, Message<?>> {

	private final AllowListDeserializingConverter deserializingConverter = new AllowListDeserializingConverter();

	@Override
	public Message<?> convert(Binary source) {
		return (Message<?>) this.deserializingConverter.convert(source.getData());
	}

	/**
	 * Add patterns for packages/classes that are allowed to be deserialized. A class can
	 * be fully qualified or a wildcard '*' is allowed at the beginning or end of the
	 * class name. Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 5.4
	 */
	public void addAllowedPatterns(String... patterns) {
		this.deserializingConverter.addAllowedPatterns(patterns);
	}

}
