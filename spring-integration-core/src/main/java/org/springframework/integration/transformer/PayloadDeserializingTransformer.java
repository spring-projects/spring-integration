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

import org.springframework.commons.serializer.DeserializingConverter;
import org.springframework.commons.serializer.java.JavaStreamingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * Transformer that deserializes the inbound byte array payload to an object by delegating to a
 * Converter&lt;byte[], Object&gt;. Default delegate is a {@link DeserializingConverter} using
 * Java serialization.
 * 
 * <p>The byte array payload must be a result of equivalent serialization.
 * 
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0.1
 */
public class PayloadDeserializingTransformer extends PayloadTypeConvertingTransformer<byte[], Object> {
	
	@Override
	protected Object transformPayload(byte[] payload) throws Exception {
		if (this.converter == null) {
			this.converter = new DeserializingConverter(new JavaStreamingConverter());
		}
		return converter.convert(payload);
	}

	@Override
	public void setConverter(Converter<byte[], Object> converter) {
		this.converter = converter;
	}
	

}
