/*
 * Copyright 2016-present the original author or authors.
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
 * A {@link Converter} to deserialize a {@link Message} from a {@link Binary} using Java serialization.
 * <p>
 * The trusted packages/classes should be provided via the
 * {@link #BinaryToMessageConverter(String...)} constructor.
 * The patterns must cover the whole serialized object graph of the stored messages:
 * for example, the message and headers classes, the header values and the payload.
 * For backward compatibility, the deprecated no-argument constructor creates an instance
 * which deserializes all classes until patterns are configured.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 5.0
 */
@ReadingConverter
public class BinaryToMessageConverter implements Converter<Binary, Message<?>> {

	private final AllowListDeserializingConverter deserializingConverter;

	/**
	 * Create an instance which deserializes all classes until patterns are configured.
	 * @deprecated since 7.2.0 in favor of {@link #BinaryToMessageConverter(String...)}
	 * with an explicit list of trusted packages/classes.
	 */
	@Deprecated(since = "7.2.0")
	@SuppressWarnings("deprecation")
	public BinaryToMessageConverter() {
		this.deserializingConverter = new AllowListDeserializingConverter();
	}

	/**
	 * Create an instance with simple patterns for allowable packages/classes for deserialization.
	 * @param allowedPatterns the patterns; must not be empty or contain null, empty or whitespace-only entries.
	 * Use {@code "*"} to explicitly allow all classes.
	 * @since 7.2.0
	 * @see AllowListDeserializingConverter#AllowListDeserializingConverter(String...)
	 */
	public BinaryToMessageConverter(String... allowedPatterns) {
		this.deserializingConverter = new AllowListDeserializingConverter(allowedPatterns);
	}

	@Override
	public Message<?> convert(Binary source) {
		return (Message<?>) this.deserializingConverter.convert(source.getData());
	}

	/**
	 * Add patterns for packages/classes that are allowed to be deserialized. A class can
	 * be fully qualified or a wildcard '*' is allowed at the beginning or end of the
	 * class name. Examples: {@code com.example.*}, {@code *.MyClass}.
	 * The patterns must not be empty or contain null, empty or whitespace-only entries.
	 * @param patterns the patterns.
	 * @since 5.4
	 * @deprecated since 7.2.0 in favor of {@link #BinaryToMessageConverter(String...)}
	 * with an explicit list of trusted packages/classes.
	 */
	@Deprecated(since = "7.2.0")
	public void addAllowedPatterns(String... patterns) {
		this.deserializingConverter.addAllowedPatterns(patterns);
	}

}
