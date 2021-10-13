/*
 * Copyright 2015-2021 the original author or authors.
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

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.integration.codec.Codec;
import org.springframework.util.Assert;

/**
 * {@link AbstractPayloadTransformer} that delegates to a codec to encode the
 * payload into a byte[].
 *
 * @param <T> inbound payload type.
 *
 * @author Gary Russell
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
	protected byte[] transformPayload(T payload) {
		try {
			return this.codec.encode(payload);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
