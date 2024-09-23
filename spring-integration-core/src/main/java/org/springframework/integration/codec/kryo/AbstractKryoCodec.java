/*
 * Copyright 2015-2024 the original author or authors.
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import org.springframework.integration.codec.Codec;
import org.springframework.util.Assert;

/**
 * Base class for {@link Codec}s using {@link Kryo}.
 * Manages pooled {@link Kryo} instances.
 *
 * @author David Turanski
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 4.2
 */
public abstract class AbstractKryoCodec implements Codec {

	protected final Pool<Kryo> pool; // NOSONAR final

	protected AbstractKryoCodec() {
		this.pool = new Pool<>(true, true) {

			@Override
			protected Kryo create() {
				Kryo kryo = new Kryo();
				kryo.setRegistrationRequired(true);
				// configure Kryo instance, customize settings
				configureKryoInstance(kryo);
				return kryo;
			}

		};
	}

	@Override
	public void encode(final Object object, OutputStream outputStream) {
		Assert.notNull(object, "cannot encode a null object");
		Assert.notNull(outputStream, "'outputSteam' cannot be null");

		Kryo kryo = this.pool.obtain();
		try (Output output = (outputStream instanceof Output castOutput ? castOutput : new Output(outputStream))) {
			doEncode(kryo, object, output);
		}
		finally {
			this.pool.free(kryo);
		}

	}

	@Override
	public <T> T decode(byte[] bytes, Class<T> type) throws IOException {
		Assert.notNull(bytes, "'bytes' cannot be null");
		try (Input input = new Input(bytes)) {
			return decode(input, type);
		}
	}

	@Override
	public <T> T decode(InputStream inputStream, final Class<T> type) {
		Assert.notNull(inputStream, "'inputStream' cannot be null");
		Assert.notNull(type, "'type' cannot be null");

		Kryo kryo = this.pool.obtain();
		try (Input input = (inputStream instanceof Input castInput ? castInput : new Input(inputStream))) {
			return doDecode(kryo, input, type);
		}
		finally {
			this.pool.free(kryo);
		}
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
