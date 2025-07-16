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
 * An implementation of {@link Codec} that combines multiple codecs into a single codec,
 * delegating encoding and decoding operations to the appropriate type-specific codec.
 * This implementation associates object types with their appropriate codecs while providing a fallback default codec
 * for unregistered types.
 * This class uses {@code ClassUtils.findClosestMatch} to select the appropriate codec for a given object type.
 * When multiple codecs match an object type, {@code ClassUtils.findClosestMatch} offers the
 * {@code failOnTie} option. If {@code failOnTie} is {@code false}, it will return any one of the matching codecs.
 * If {@code failOnTie} is {@code true} and multiple codecs match, it will throw an {@code IllegalStateException}.
 * {@link CompositeCodec} sets {@code failOnTie} to {@code true}, so if multiple codecs match, an
 * {@code IllegalStateException} is thrown.
 *
 * @author David Turanski
 * @author Glenn Renfro
 *
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
		Class<?> clazz = ClassUtils.findClosestMatch(type, this.delegates.keySet(), true);
		return clazz == null ? this.defaultCodec : this.delegates.getOrDefault(clazz, this.defaultCodec);
	}

}
