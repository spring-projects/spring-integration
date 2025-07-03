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

import org.springframework.integration.util.ClassUtils;
import org.springframework.util.Assert;

/**
 * A Codec that can delegate to one out of many Codecs, each mapped to a class.
 * @author David Turanski
 * @author Glenn Renfro
 * @since 4.2
 */
public class CompositeCodec implements Codec {

	private final Codec defaultCodec;

	private final Map<Class<?>, Codec> delegates;

	public CompositeCodec(Map<Class<?>, Codec> delegates, Codec defaultCodec) {
		this.defaultCodec = defaultCodec;
		Assert.notEmpty(delegates, "delegates must not be empty");
		this.delegates = new HashMap<>(delegates);
	}

	/**
	 * @deprecated since 7.0 in favor of {@link #CompositeCodec(Map, Codec)}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public CompositeCodec(Codec defaultCodec) {
		this(Map.of(), defaultCodec);
	}

	@Override
	public void encode(Object object, OutputStream outputStream) throws IOException {
		Assert.notNull(object, "cannot encode a null object");
		Assert.notNull(outputStream, "'outputStream' cannot be null");
		findDelegate(object.getClass()).encode(object, outputStream);
	}

	@Override
	public byte[] encode(Object object) throws IOException {
		Assert.notNull(object, "cannot encode a null object");
		return findDelegate(object.getClass()).encode(object);
	}

	@Override
	public <T> T decode(InputStream inputStream, Class<T> type) throws IOException {
		Assert.notNull(inputStream, "'inputStream' cannot be null");
		Assert.notNull(type, "'type' cannot be null");
		return findDelegate(type).decode(inputStream, type);
	}

	@Override
	public <T> T decode(byte[] bytes, Class<T> type) throws IOException {
		return decode(new ByteArrayInputStream(bytes), type);
	}

	private Codec findDelegate(Class<?> type) {
		Class<?> clazz = ClassUtils.findClosestMatch(type, this.delegates.keySet(), false);
		Codec codec = this.delegates.get(clazz);
		if (codec == null) {
			codec = this.defaultCodec;
		}
		return codec;
	}

}
