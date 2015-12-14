/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.codec.kryo.pool;

import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryo.Kryo;

/**
 * A simple pool interface for {@link Kryo} instances. Use the {@link KryoPool.Builder} to
 * construct a pool instance.
 * <p>
 * Copied from Kryo 3.0 to facilitate dropping back to 2.22 for Spring IO
 * compatibility.
 * <p>
 * Usage:
 * <pre class="code">
 * {@code
 *
 * KryoFactory factory = new KryoFactory() {
 *   public Kryo create () {
 *     Kryo kryo = new Kryo();
 *     // configure kryo instance, customize settings
 *     return kryo;
 *   }
 * };
 * // Simple pool, you might also activate SoftReferences to fight OOMEs.
 * KryoPool pool = new KryoPool.Builder(factory).build();
 * Kryo kryo = pool.borrow();
 * // do s.th. with kryo here, and afterwards release it
 * pool.release(kryo);
 *
 * // or use a callback to work with kryo (pool.run borrows+releases for you)
 * String value = pool.run(new KryoCallback<String>() {
 *   public String execute(Kryo kryo) {
 *     return kryo.readObject(input, String.class);
 *   }
 * });
 *
 * }
 * </pre>
 * @author Martin Grotzke
 */
public interface KryoPool {

	/**
	 * Takes a {@link Kryo} instance from the pool or creates a new one
	 * (using the factory) if the pool is empty.
	 * @return the {@link Kryo} instance.
	 */
	Kryo borrow();

	/**
	 * Returns the given {@link Kryo} instance to the pool.
	 * @param kryo the {@link Kryo} instance to release.
	 */
	void release(Kryo kryo);

	/**
	 * Runs the provided {@link KryoCallback} with a {@link Kryo} instance
	 * from the pool (borrow/release around {@link KryoCallback#execute(Kryo)}).
	 * @param callback the {@link KryoCallback} to call.
	 * @param <T> The type of the result of the interaction with kryo.
	 * @return the result of the interaction with {@link Kryo}.
	 */
	<T> T run(KryoCallback<T> callback);

	/**
	 * Builder for a {@link KryoPool} instance, constructs a {@link KryoPoolQueueImpl} instance.
	 */
	class Builder {

		private final KryoFactory factory;

		private Queue<Kryo> queue = new ConcurrentLinkedQueue<Kryo>();

		private boolean softReferences;

		public Builder(KryoFactory factory) {
			if (factory == null) {
				throw new IllegalArgumentException("factory must not be null");
			}
			this.factory = factory;
		}

		/**
		 * Use the given queue for pooling kryo instances (by default a {@link ConcurrentLinkedQueue}
		 * is used).
		 * @param queue the queue instance to use.
		 * @return the Builder instance.
		 */
		public Builder queue(Queue<Kryo> queue) {
			if (queue == null) {
				throw new IllegalArgumentException("queue must not be null");
			}
			this.queue = queue;
			return this;
		}

		/**
		 * Use {@link SoftReference}s for pooled {@link Kryo} instances, so that
		 * instances may be garbage collected when there's memory demand (by default
		 * disabled).
		 * @return the Builder instance.
		 */
		public Builder softReferences() {
			softReferences = true;
			return this;
		}

		/**
		 * Build the pool.
		 * @return the KryoPool instance.
		 */
		public KryoPool build() {
			Queue<Kryo> q = softReferences ? new SoftReferenceQueue(queue) : queue;
			return new KryoPoolQueueImpl(factory, q);
		}

		@Override
		public String toString() {
			return getClass().getName() + "[queue.class=" + queue.getClass() +
					", softReferences=" + softReferences + "]";
		}

	}

}
