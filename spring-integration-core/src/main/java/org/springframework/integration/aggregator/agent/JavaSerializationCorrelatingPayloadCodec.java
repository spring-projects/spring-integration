/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import com.google.protobuf.ByteString;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.aggregator.agent.grpc.SerializedObject;
import org.springframework.util.Assert;

/**
 * A {@link CorrelatingPayloadCodec} based on Java object serialization.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
public final class JavaSerializationCorrelatingPayloadCodec implements CorrelatingPayloadCodec {

	public static final String CONTENT_TYPE = "application/x-java-serialized-object";

	private static final int FORMAT_VERSION = 1;

	private final ClassLoader classLoader;

	public JavaSerializationCorrelatingPayloadCodec() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public JavaSerializationCorrelatingPayloadCodec(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader != null ? classLoader : JavaSerializationCorrelatingPayloadCodec.class.getClassLoader();
	}

	@Override
	public SerializedObject encode(@Nullable Object payload) {
		if (payload == null) {
			return SerializedObject.newBuilder()
					.setNullValue(true)
					.setFormatVersion(FORMAT_VERSION)
					.setContentType(CONTENT_TYPE)
					.build();
		}
		Assert.isInstanceOf(Serializable.class, payload,
				() -> "Correlating agent payload must implement Serializable: " + payload.getClass().getName());
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ObjectOutputStream output = new ObjectOutputStream(bytes)) {

			output.writeObject(payload);
			output.flush();
			return SerializedObject.newBuilder()
					.setData(ByteString.copyFrom(bytes.toByteArray()))
					.setClassName(payload.getClass().getName())
					.setFormatVersion(FORMAT_VERSION)
					.setContentType(CONTENT_TYPE)
					.build();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to serialize correlating agent payload of type "
					+ payload.getClass().getName(), ex);
		}
	}

	@Override
	@Nullable
	public Object decode(SerializedObject payload, ObjectInputFilter filter) {
		Assert.notNull(payload, "'payload' must not be null");
		Assert.notNull(filter, "'filter' must not be null");
		Assert.isTrue(payload.getFormatVersion() == FORMAT_VERSION,
				() -> "Unsupported correlating payload format version: " + payload.getFormatVersion());
		Assert.isTrue(CONTENT_TYPE.equals(payload.getContentType()),
				() -> "Unsupported correlating payload content type: " + payload.getContentType());
		if (payload.getNullValue()) {
			return null;
		}
		try (ByteArrayInputStream bytes = new ByteArrayInputStream(payload.getData().toByteArray());
				ObjectInputStream input = new ClassLoaderObjectInputStream(bytes, this.classLoader)) {

			input.setObjectInputFilter(filter);
			Object result = input.readObject();
			Assert.state(result != null, "Serialized correlating payload resolved to null");
			Assert.state(payload.getClassName().equals(result.getClass().getName()),
					() -> "Serialized correlating payload declared " + payload.getClassName()
							+ " but resolved to " + result.getClass().getName());
			return result;
		}
		catch (IOException | ClassNotFoundException ex) {
			throw new IllegalArgumentException("Failed to deserialize correlating agent payload of type "
					+ payload.getClassName(), ex);
		}
	}

	/**
	 * Return the default filter used by correlating handlers. It permits bounded graphs
	 * composed of JDK, Spring, and Jackson value types. Applications transporting domain objects
	 * must provide an explicit filter through the handler configuration.
	 * @return the default filter
	 */
	public static ObjectInputFilter defaultFilter() {
		return filterInfo -> {
			if (filterInfo.depth() > 64 || filterInfo.references() > 10_000 || filterInfo.streamBytes() > 10_485_760) {
				return ObjectInputFilter.Status.REJECTED;
			}
			Class<?> serialClass = filterInfo.serialClass();
			if (serialClass == null) {
				return ObjectInputFilter.Status.UNDECIDED;
			}
			while (serialClass.isArray()) {
				serialClass = serialClass.getComponentType();
			}
			if (serialClass.isPrimitive()) {
				return ObjectInputFilter.Status.ALLOWED;
			}
			String packageName = serialClass.getPackageName();
			return packageName.startsWith("java.") || packageName.startsWith("org.springframework.")
					|| packageName.startsWith("tools.jackson.") || packageName.startsWith("com.fasterxml.jackson.")
					? ObjectInputFilter.Status.ALLOWED
					: ObjectInputFilter.Status.REJECTED;
		};
	}

	private static final class ClassLoaderObjectInputStream extends ObjectInputStream {

		private final ClassLoader classLoader;

		ClassLoaderObjectInputStream(InputStream inputStream, ClassLoader classLoader) throws IOException {
			super(inputStream);
			this.classLoader = classLoader;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass descriptor) throws IOException, ClassNotFoundException {
			try {
				return Class.forName(descriptor.getName(), false, this.classLoader);
			}
			catch (ClassNotFoundException ex) {
				return super.resolveClass(descriptor);
			}
		}

	}

}
