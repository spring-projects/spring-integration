/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.core.convert.converter.Converter;
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

	private Converter<T, U> converter;

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
		return this.converter.convert(payload);
	}

}
