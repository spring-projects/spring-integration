/*
 * Copyright 2016-2019 the original author or authors.
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
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 5.0
 */
@WritingConverter
public class MessageToBinaryConverter implements Converter<Message<?>, Binary> {

	private final Converter<Object, byte[]> serializingConverter = new SerializingConverter();

	@Override
	public Binary convert(Message<?> source) {
		return new Binary(this.serializingConverter.convert(source));
	}

}
