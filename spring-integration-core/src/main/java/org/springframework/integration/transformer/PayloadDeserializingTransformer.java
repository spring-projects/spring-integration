/*
 * Copyright 2002-2020 the original author or authors.
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
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.1
 */
public class PayloadDeserializingTransformer extends PayloadTypeConvertingTransformer<byte[], Object> {

	/**
	 * Instantiate based on the {@link AllowListDeserializingConverter} with the
	 * {@link org.springframework.core.serializer.DefaultDeserializer}.
	 */
	public PayloadDeserializingTransformer() {
		doSetConverter(new AllowListDeserializingConverter());
	}

	public void setDeserializer(Deserializer<Object> deserializer) {
		setConverter(new AllowListDeserializingConverter(deserializer));
	}

	/**
	 * When using a {@link AllowListDeserializingConverter} (the default) add patterns
	 * for packages/classes that are allowed to be deserialized.
	 * A class can be fully qualified or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 5.4
	 */
	public void setAllowedPatterns(String... patterns) {
		Assert.isTrue(getConverter() instanceof AllowListDeserializingConverter,
				"Patterns can only be provided when using a 'AllowListDeserializingConverter'");
		((AllowListDeserializingConverter) getConverter()).setAllowedPatterns(patterns);
	}

}
