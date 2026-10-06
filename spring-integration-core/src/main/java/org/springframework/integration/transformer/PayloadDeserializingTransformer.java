/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.util.Assert;

/**
 * Transformer that deserializes the inbound byte array payload to an object by delegating
 * to a Converter&lt;byte[], Object&gt;. Default delegate is a
 * {@link AllowListDeserializingConverter} using Java serialization.
 *
 * <p>
 * The byte array payload must be a result of equivalent serialization.
 * <p>
 * The trusted packages/classes should be provided via the
 * {@link #PayloadDeserializingTransformer(String...)} constructor, for example
 * {@code new PayloadDeserializingTransformer("com.example.model.*")}.
 * An explicit {@code "*"} pattern allows all classes.
 * For backward compatibility, the deprecated no-argument constructor creates an instance
 * which deserializes all classes until patterns are configured.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 1.0.1
 */
public class PayloadDeserializingTransformer extends PayloadTypeConvertingTransformer<byte[], Object> {

	/**
	 * Instantiate based on the {@link AllowListDeserializingConverter} with the
	 * {@link org.springframework.core.serializer.DefaultDeserializer}.
	 * @deprecated since 7.0.7 in favor of {@link #PayloadDeserializingTransformer(String...)}
	 * with an explicit list of trusted packages/classes.
	 * An instance created by this constructor deserializes all classes until patterns are configured.
	 */
	@Deprecated(since = "7.0.7")
	@SuppressWarnings("this-escape")
	public PayloadDeserializingTransformer() {
		doSetConverter(new AllowListDeserializingConverter());
	}

	/**
	 * Instantiate based on the {@link AllowListDeserializingConverter} with the
	 * {@link org.springframework.core.serializer.DefaultDeserializer}
	 * and the provided simple patterns for allowable packages/classes.
	 * @param allowedPatterns the patterns; must not be empty or contain null, empty or whitespace-only entries.
	 * Use {@code "*"} to explicitly allow all classes.
	 * @since 7.0.7
	 * @see AllowListDeserializingConverter#AllowListDeserializingConverter(String...)
	 */
	@SuppressWarnings("this-escape")
	public PayloadDeserializingTransformer(String... allowedPatterns) {
		doSetConverter(new AllowListDeserializingConverter(allowedPatterns));
	}

	/**
	 * Set the {@link Deserializer} to use; the allowed patterns of the current
	 * {@link AllowListDeserializingConverter} are preserved.
	 * Otherwise, a custom converter is replaced with a new {@link AllowListDeserializingConverter}
	 * without allowed patterns.
	 * If the deserializer is not a {@link org.springframework.core.serializer.DefaultDeserializer},
	 * only the class of the deserialization result is checked against the patterns.
	 * @param deserializer the deserializer to use.
	 */
	@SuppressWarnings("deprecation")
	public void setDeserializer(Deserializer<Object> deserializer) {
		if (getConverter() instanceof AllowListDeserializingConverter allowListDeserializingConverter) {
			setConverter(allowListDeserializingConverter.withDeserializer(deserializer));
		}
		else {
			setConverter(new AllowListDeserializingConverter(deserializer));
		}
	}

	/**
	 * When using a {@link AllowListDeserializingConverter} (the default) add patterns
	 * for packages/classes that are allowed to be deserialized.
	 * A class can be fully qualified, or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.example.*}, {@code *.MyClass}.
	 * The patterns must not be empty or contain null, empty or whitespace-only entries.
	 * @param patterns the patterns.
	 * @since 5.4
	 * @see AllowListDeserializingConverter#setAllowedPatterns(String...)
	 */
	public void setAllowedPatterns(String... patterns) {
		Assert.isTrue(getConverter() instanceof AllowListDeserializingConverter,
				"Patterns can only be provided when using a 'AllowListDeserializingConverter'");
		((AllowListDeserializingConverter) getConverter()).setAllowedPatterns(patterns);
	}

	@Override
	public String getComponentType() {
		return "deserializing-payload-transformer";
	}

}
