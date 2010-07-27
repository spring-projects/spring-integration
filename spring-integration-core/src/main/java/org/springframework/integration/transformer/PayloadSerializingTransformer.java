/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.commons.serializer.java.SerializingConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * Transformer that serializes the inbound payload into a byte array by delegating to a 
 * Converter&lt;Object, byte[]&gt;. Default delegate is a {@link SerializingConverter}.
 * 
 * <p>The payload instance must be Serializable if the default converter is used.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0.1
 */
public class PayloadSerializingTransformer extends PayloadTypeConvertingTransformer<Object, byte[]> {

	
	public PayloadSerializingTransformer() {
		this.converter = new SerializingConverter();
	}

	@Override
	protected byte[] transformPayload(Object payload) throws Exception {
		Assert.notNull(this.converter, this.getClass().getName() + " needs a Converter<Object, byte[]>");
		return converter.convert(payload);
	}

	@Override
	public void setConverter(Converter<Object, byte[]> converter) {
		this.converter = converter;
	}
	
}
