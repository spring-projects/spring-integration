/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.codec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.util.ClassUtils;
import org.springframework.util.Assert;

/**
 * A Codec that can delegate to one out of many Codecs, each mapped to a class.
 * @author David Turanski
 * @since 4.2
 */
public class CompositeCodec implements Codec {

	private final Codec defaultCodec;

	private final Map<Class<?>, Codec> delegates;

	public CompositeCodec(@Nullable Map<Class<?>, Codec> delegates, Codec defaultCodec) {
		Assert.notNull(defaultCodec, "'defaultCodec' cannot be null");
		this.defaultCodec = defaultCodec;
		if (delegates == null) {
			delegates = Map.of();
		}
		this.delegates = new HashMap<Class<?>, Codec>(delegates);
	}

	public CompositeCodec(Codec defaultCodec) {
		this(null, defaultCodec);
	}

	@Override
	public void encode(Object object, OutputStream outputStream) throws IOException {
		Assert.notNull(object, "cannot encode a null object");
		Assert.notNull(outputStream, "'outputStream' cannot be null");
		Codec codec = findDelegate(object.getClass());
		if (codec != null) {
			codec.encode(object, outputStream);
		}
		else {
			this.defaultCodec.encode(object, outputStream);
		}
	}

	@Override
	public byte[] encode(Object object) throws IOException {
		Assert.notNull(object, "cannot encode a null object");
		Codec codec = findDelegate(object.getClass());
		if (codec != null) {
			return codec.encode(object);
		}
		else {
			return this.defaultCodec.encode(object);
		}
	}

	@Override
	public <T> T decode(InputStream inputStream, Class<T> type) throws IOException {
		Assert.notNull(inputStream, "'inputStream' cannot be null");
		Assert.notNull(type, "'type' cannot be null");
		Codec codec = findDelegate(type);
		if (codec != null) {
			return codec.decode(inputStream, type);
		}
		else {
			return this.defaultCodec.decode(inputStream, type);
		}
	}

	@Override
	public <T> T decode(byte[] bytes, Class<T> type) throws IOException {
		return decode(new ByteArrayInputStream(bytes), type);
	}

	private @Nullable Codec findDelegate(Class<?> type) {
		if (this.delegates.isEmpty()) {
			return null;
		}

		Class<?> clazz = ClassUtils.findClosestMatch(type, this.delegates.keySet(), false);
		return this.delegates.get(clazz);
	}

}
