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

package org.springframework.integration.codec.kryo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

import org.springframework.integration.codec.Codec;
import org.springframework.util.Assert;


/**
 * Base class for Codecs using {@link com.esotericsoftware.kryo.Kryo}
 * @author David Turanski
 */
public abstract class AbstractKryoCodec implements Codec {

	protected final KryoPool pool;

	protected AbstractKryoCodec() {
		KryoFactory factory = new KryoFactory() {
			public Kryo create() {
				Kryo kryo = new Kryo();
				// configure kryo instance, customize settings
				configureKryoInstance(kryo);
				return kryo;
			}
		};
		// Build pool with SoftReferences enabled (optional)
		pool = new KryoPool.Builder(factory).softReferences().build();
	}

	/**
	 * Serialize an object using an existing output stream
	 * @param object the object to be serialized
	 * @param outputStream the output stream, e.g. a FileOutputStream
	 * @throws IOException
	 */

	public void serialize(final Object object, OutputStream outputStream) throws IOException {
		Assert.notNull(outputStream, "\'outputSteam\' cannot be null");
		final Output output = (outputStream instanceof Output ? (Output) outputStream : new Output(outputStream));
		this.pool.run(new KryoCallback<Object>() {
			@SuppressWarnings("unchecked")
			public Object execute(Kryo kryo) {
				doSerialize(kryo, object, output);
				return Void.class;
			}
		});
		output.close();
	}

	protected abstract void doSerialize(Kryo kryo, Object object, Output output);

	protected abstract Object doDeserialize(Kryo kryo, Input input, Class<?> type);

	protected abstract void configureKryoInstance(Kryo kryo);

	/**
	 * Deserialize an object of a given type given a byte array
	 * @param bytes the byte array containing the serialized object
	 * @param type the object's class
	 * @return the object
	 * @throws IOException
	 */
	@Override
	public Object deserialize(byte[] bytes, Class<?> type) throws IOException {
		final Input input = new Input(bytes);
		try {
			return deserialize(input, type);
		}
		finally {
			input.close();
		}
	}

	@Override
	public Object deserialize(InputStream inputStream, final Class<?> type) throws IOException {
		Assert.notNull(inputStream, "\'inputStream\' cannot be null");
		final Input input = (inputStream instanceof Input ? (Input) inputStream : new Input(inputStream));
		Object result = null;
		try {
			result = this.pool.run(new KryoCallback<Object>() {
				@SuppressWarnings("unchecked")
				public Object execute(Kryo kryo) {
					return doDeserialize(kryo, input, type);
				}
			});
		}
		finally {
			input.close();
		}
		return result;
	}

	@Override
	public byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serialize(object, bos);
		byte[] bytes = bos.toByteArray();
		bos.close();
		return bytes;
	}

}
