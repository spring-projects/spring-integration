/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.support.cloudevents;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import io.cloudevents.fun.DataMarshaller;
import io.cloudevents.json.Json;

/**
 * A {@link DataMarshaller} implementation for delegating
 * to the provided {@link Encoder}s according a {@link MessageHeaders#CONTENT_TYPE}
 * header value.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ContentTypeDelegatingDataMarshaller implements DataMarshaller<byte[], Object, String> {

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

	private final List<Encoder<?>> encoders = new ArrayList<>();

	public ContentTypeDelegatingDataMarshaller(Encoder<?>... encoders) {
		this.encoders.add(CharSequenceEncoder.allMimeTypes());
		setEncoders(encoders);
	}

	public final void setEncoders(Encoder<?>... encoders) {
		Assert.notNull(encoders, "'encoders' must not be null");
		Assert.noNullElements(encoders, "'encoders' must not contain null elements");
		this.encoders.addAll(Arrays.asList(encoders));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public byte[] marshal(Object data, Map<String, String> headers) throws RuntimeException {
		String contentType = headers.get(MessageHeaders.CONTENT_TYPE);
		if (contentType == null) { // Assume JSON by default
			return Json.binaryMarshal(data, headers);
		}
		else {
			ResolvableType elementType = ResolvableType.forClass(data.getClass());
			MimeType mimeType = MimeType.valueOf(contentType);
			Encoder<Object> encoder = encoder(elementType, mimeType);
			DataBuffer dataBuffer =
					encoder.encodeValue(data, this.dataBufferFactory, elementType,
							mimeType, (Map<String, Object>) (Map) headers);

			ByteBuffer buf = dataBuffer.asByteBuffer();
			byte[] result = new byte[buf.remaining()];
			buf.get(result);
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	private Encoder<Object> encoder(ResolvableType elementType, MimeType mimeType) {
		for (Encoder<?> encoder : this.encoders) {
			if (encoder.canEncode(elementType, mimeType)) {
				return (Encoder<Object>) encoder;
			}
		}
		throw new IllegalArgumentException("No encoder for " + elementType);
	}

}
