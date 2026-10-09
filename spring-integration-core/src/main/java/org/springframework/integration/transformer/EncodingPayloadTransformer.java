/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.integration.codec.Codec;
import org.springframework.util.Assert;

/**
 * {@link AbstractPayloadTransformer} that delegates to a codec to encode the payload into a byte[].
 *
 * @param <T> inbound payload type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class EncodingPayloadTransformer<T> extends AbstractPayloadTransformer<T, byte[]> {

	private final Codec codec;

	public EncodingPayloadTransformer(Codec codec) {
		Assert.notNull(codec, "'codec' cannot be null");
		this.codec = codec;
	}

	@Override
	public String getComponentType() {
		return "payload-encoding-transformer";
	}

	@Override
	protected byte[] transformPayload(T payload) {
		try {
			return this.codec.encode(payload);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
