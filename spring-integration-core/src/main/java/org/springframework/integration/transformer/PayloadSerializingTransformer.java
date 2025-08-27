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

import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.SerializingConverter;

/**
 * Transformer that serializes the inbound payload into a byte array
 * by delegating to the {@link SerializingConverter} using Java serialization.
 *
 * <p>The payload instance must be Serializable if the default converter is used.
 *
 * <p>A custom {@link Serializer} can be provided via {@link #setSerializer(Serializer)}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.1
 */
public class PayloadSerializingTransformer extends PayloadTypeConvertingTransformer<Object, byte[]> {

	/**
	 * Instantiate based on the {@link SerializingConverter} with the
	 * {@link org.springframework.core.serializer.DefaultSerializer}.
	 */
	@SuppressWarnings("this-escape")
	public PayloadSerializingTransformer() {
		doSetConverter(new SerializingConverter());
	}

	public void setSerializer(Serializer<Object> serializer) {
		setConverter(new SerializingConverter(serializer));
	}

	@Override
	public String getComponentType() {
		return "serializing-payload-transformer";
	}

}
