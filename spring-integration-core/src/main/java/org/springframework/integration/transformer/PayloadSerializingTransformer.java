/*
 * Copyright 2002-present the original author or authors.
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
