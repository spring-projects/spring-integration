/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * Transformer that converts the inbound payload to an object by delegating to a
 * Converter&lt;Object, Object&gt;. A reference to the delegate must be provided.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class PayloadTypeConvertingTransformer<T, U> extends AbstractPayloadTransformer<T, U> {

	protected Converter<T, U> converter;

	/**
	 * Specify the converter to use.
	 *
	 * @param converter The Converter.
	 */
	public void setConverter(Converter<T, U> converter) {
		this.converter = converter;
	}

	@Override
	protected U transformPayload(T payload) throws Exception {
		Assert.notNull(this.converter, this.getClass().getName() + " requires a Converter<Object, Object>");
		return this.converter.convert(payload);
	}

}
