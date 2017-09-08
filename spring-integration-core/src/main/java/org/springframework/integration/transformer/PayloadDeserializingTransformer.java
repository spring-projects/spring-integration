/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.support.converter.WhiteListDeserializingConverter;
import org.springframework.util.Assert;

/**
 * Transformer that deserializes the inbound byte array payload to an object by delegating
 * to a Converter&lt;byte[], Object&gt;. Default delegate is a
 * {@link WhiteListDeserializingConverter} using Java serialization.
 *
 * <p>
 * The byte array payload must be a result of equivalent serialization.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0.1
 */
public class PayloadDeserializingTransformer extends PayloadTypeConvertingTransformer<byte[], Object> {


	public PayloadDeserializingTransformer() {
		doSetConverter(new WhiteListDeserializingConverter());
	}

	private void doSetConverter(Converter<byte[], Object> converter) {
		this.converter = converter;
	}

	public void setDeserializer(Deserializer<Object> deserializer) {
		setConverter(new WhiteListDeserializingConverter(deserializer));
	}

	/**
	 * When using a {@link WhiteListDeserializingConverter} (the default) add patterns
	 * for packages/classes that are allowed to be deserialized.
	 * A class can be fully qualified or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 4.2.13
	 */
	public void setWhiteListPatterns(String... patterns) {
		Assert.isTrue(this.converter instanceof WhiteListDeserializingConverter,
				"Patterns can only be provided when using a 'WhiteListDeserializingConverter'");
		((WhiteListDeserializingConverter) this.converter).setWhiteListPatterns(patterns);
	}

	@Override
	protected Object transformPayload(byte[] payload) throws Exception {
		return this.converter.convert(payload);
	}

}
