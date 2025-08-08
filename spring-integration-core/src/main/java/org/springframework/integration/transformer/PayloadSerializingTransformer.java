/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.SerializingConverter;

/**
 * Transformer that serializes the inbound payload into a byte array by delegating to a
 * Converter&lt;Object, byte[]&gt;. Default delegate is a {@link SerializingConverter} using
 * Java serialization.
 *
 * <p>The payload instance must be Serializable if the default converter is used.
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
	public PayloadSerializingTransformer() {
		doSetConverter(new SerializingConverter());
	}

	public void setSerializer(Serializer<Object> serializer) {
		setConverter(new SerializingConverter(serializer));
	}

}
