/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.ip.tcp.serializer;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.integration.util.SimplePool;
import org.springframework.integration.util.SimplePool.PoolItemCallback;
import org.springframework.util.Assert;

/**
 * Base class for deserializers that cannot determine the buffer size needed.
 * Optionally pools buffers.
 *
 * @author Gary Russell
 * @author Ngoc Nhan
 * @since 4.3
 *
 */
public abstract class AbstractPooledBufferByteArraySerializer extends AbstractByteArraySerializer {

	private SimplePool<byte[]> pool;

	private long poolWaitTimeout = Long.MAX_VALUE;

	/**
	 * Set the pool size for deserialization buffers.
	 * @param size the size, -1 for unlimited.
	 * @since 4.3
	 */
	public void setPoolSize(int size) {
		Assert.isNull(this.pool, "Cannot change pool size once set");
		this.pool = new SimplePool<>(size, new PoolItemCallback<>() {

			@Override
			public byte[] createForPool() {
				return new byte[getMaxMessageSize()];
			}

			@Override
			public boolean isStale(byte[] item) {
				return false; // never stale
			}

			@Override
			public void removedFromPool(byte[] item) {
			}

		});
		this.pool.setWaitTimeout(this.poolWaitTimeout);
	}

	/**
	 * Set the pool wait timeout if a pool is configured, default unlimited.
	 * @param poolWaitTimeout the timeout.
	 */
	public void setPoolWaitTimeout(long poolWaitTimeout) {
		this.poolWaitTimeout = poolWaitTimeout;
		if (this.pool != null) {
			this.pool.setWaitTimeout(poolWaitTimeout);
		}
	}

	@Override
	public final byte[] deserialize(InputStream inputStream) throws IOException {
		byte[] buffer = this.pool == null ? new byte[getMaxMessageSize()] : this.pool.getItem();
		try {
			return doDeserialize(inputStream, buffer);
		}
		finally {
			if (this.pool != null) {
				this.pool.releaseItem(buffer);
			}
		}
	}

	/**
	 * @param inputStream the input stream.
	 * @param buffer the raw working buffer (maxMessageSize).
	 * @return the decoded bytes.
	 * @throws IOException an io exception.
	 * @since 4.3
	 */
	protected abstract byte[] doDeserialize(InputStream inputStream, byte[] buffer) throws IOException;

	/**
	 * Copy size bytes to a new buffer exactly size bytes long. If a pool is not
	 * in use and the array is already the correct length, it is simply returned.
	 * @param buffer The buffer containing the data.
	 * @param size The number of bytes to copy.
	 * @return The new buffer, or the buffer parameter if it is
	 * already the correct size and there is no pool.
	 */
	protected byte[] copyToSizedArray(byte[] buffer, int size) {
		if (size == buffer.length && this.pool == null) {
			return buffer;
		}
		byte[] assembledData = new byte[size];
		System.arraycopy(buffer, 0, assembledData, 0, size);
		return assembledData;
	}

}
