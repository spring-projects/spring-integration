/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.integration.codec.Codec;
import org.springframework.util.Assert;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

/**
 * Base class for {@link Codec}s using {@link Kryo}.
 * Manages pooled {@link Kryo} instances.
 *
 * @author David Turanski
 * @since 4.2
 */
public abstract class AbstractKryoCodec implements Codec {

	protected final KryoPool pool; // NOSONAR final

	protected AbstractKryoCodec() {
		KryoFactory factory = () -> {
			Kryo kryo = new Kryo();
			kryo.setRegistrationRequired(true);
			// configure Kryo instance, customize settings
			configureKryoInstance(kryo);
			return kryo;
		};
		// Build pool with SoftReferences enabled (optional)
		this.pool = new KryoPool.Builder(factory).softReferences().build();
	}

	@Override
	public void encode(final Object object, OutputStream outputStream) {
		Assert.notNull(object, "cannot encode a null object");
		Assert.notNull(outputStream, "'outputSteam' cannot be null");
		final Output output = (outputStream instanceof Output ? (Output) outputStream : new Output(outputStream));
		this.pool.run(kryo -> {
			doEncode(kryo, object, output);
			return Void.class;
		});
		output.close();
	}

	@Override
	public <T> T decode(byte[] bytes, Class<T> type) throws IOException {
		Assert.notNull(bytes, "'bytes' cannot be null");
		final Input input = new Input(bytes);
		try {
			return decode(input, type);
		}
		finally {
			input.close();
		}
	}

	@Override
	public <T> T decode(InputStream inputStream, final Class<T> type) {
		Assert.notNull(inputStream, "'inputStream' cannot be null");
		Assert.notNull(type, "'type' cannot be null");
		final Input input = (inputStream instanceof Input ? (Input) inputStream : new Input(inputStream));
		T result = null;
		try {
			result = this.pool.run(kryo -> doDecode(kryo, input, type));
		}
		finally {
			input.close();
		}
		return result;
	}

	@Override
	public byte[] encode(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		encode(object, bos);
		byte[] bytes = bos.toByteArray();
		bos.close();
		return bytes;
	}

	/**
	 * Subclasses implement this method to encode with Kryo.
	 * @param kryo the Kryo instance
	 * @param object the object to encode
	 * @param output the Kryo Output instance
	 */
	protected abstract void doEncode(Kryo kryo, Object object, Output output);

	/**
	 * Subclasses implement this method to decode with Kryo.
	 * @param kryo the Kryo instance
	 * @param input the Kryo Input instance
	 * @param type the class of the decoded object
	 * @param <T> the type for decoded object
	 * @return the decoded object
	 */
	protected abstract <T> T doDecode(Kryo kryo, Input input, Class<T> type);

	/**
	 * Subclasses implement this to configure the kryo instance. This is invoked on each new Kryo instance
	 * when it is created.
	 * @param kryo the Kryo instance
	 */
	protected abstract void configureKryoInstance(Kryo kryo);

}
