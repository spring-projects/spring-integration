/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;

import org.springframework.integration.util.ClassUtils;
import org.springframework.util.Assert;


/**
 * A codec that can delegate to one out of many codecs, depending on the type of the object to serialize/deserialize.
 * @author David Turanski
 */
public class CompositeCodec implements Codec {

	private final Codec defaultCodec;

	private final Map<Class<?>, Codec> delegates;

	public CompositeCodec(Map<Class<?>, Codec> delegates, Codec defaultCodec) {
		Assert.notNull(defaultCodec, "'defaultCodec' cannot be null");
		this.defaultCodec = defaultCodec;
		this.delegates = delegates;
	}

	public CompositeCodec(Codec defaultCodec) {
		this(null, defaultCodec);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serialize(Object object, OutputStream outputStream) throws IOException {
		Assert.notNull(object, "cannot serialize a null object");
		Codec codec = findDelegate(object.getClass());
		if (codec != null) {
			codec.serialize(object, outputStream);
		}
		else {
			defaultCodec.serialize(object, outputStream);
		}
	}

	@Override
	public byte[] serialize(Object object) throws IOException {
		Codec codec = findDelegate(object.getClass());
		if (codec != null) {
			return codec.serialize(object);
		}
		else {
			return defaultCodec.serialize(object);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object deserialize(InputStream inputStream, Class<?> type) throws IOException {
		Codec codec = findDelegate(type);
		if (codec != null) {
			return codec.deserialize(inputStream, type);
		}
		else {
			return defaultCodec.deserialize(inputStream, type);
		}
	}

	@Override
	public Object deserialize(byte[] bytes, Class<?> type) throws IOException {
		return deserialize(new ByteArrayInputStream(bytes), type);
	}

	private Codec findDelegate(Class<?> type) {
		if (delegates == null) {
			return null;
		}

		Class<?> clazz = ClassUtils.findClosestMatch(type, delegates.keySet(), false);
		return delegates.get(clazz);
	}
}
