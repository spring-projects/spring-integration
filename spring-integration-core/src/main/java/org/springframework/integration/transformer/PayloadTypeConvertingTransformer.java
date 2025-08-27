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

import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;

/**
 * Transformer that converts the inbound payload to an object by delegating to a
 * Converter&lt;Object, Object&gt;. A reference to the delegate must be provided.
 *
 * @param <T> inbound payload type.
 * @param <U> outbound payload type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class PayloadTypeConvertingTransformer<T, U> extends AbstractPayloadTransformer<T, U> {

	@SuppressWarnings("NullAway.Init")
	private Converter<T, U> converter;

	@Override
	public String getComponentType() {
		return "converting-payload-transformer";
	}

	/**
	 * Specify the converter to use.
	 * @param converter The Converter.
	 */
	public void setConverter(Converter<T, U> converter) {
		doSetConverter(converter);
	}

	protected final void doSetConverter(Converter<T, U> converter) {
		Assert.notNull(converter, "'converter' must not be null");
		this.converter = converter;
	}

	/**
	 * Get the configured {@link Converter}.
	 * @return the converter.
	 */
	protected Converter<T, U> getConverter() {
		return this.converter;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.converter, () -> getClass().getName() + " requires a Converter<Object, Object>");
	}

	@Override
	protected U transformPayload(T payload) {
		U converted = this.converter.convert(payload);
		if (converted == null) {
			throw new MessageConversionException("The converter '" + this.converter + "' returned null");
		}
		return converted;
	}

}
