/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.util;

import java.util.Iterator;
import java.util.function.Function;

import org.springframework.lang.Nullable;

/**
 * An {@link Iterator} implementation to convert each item from the target
 * {@link #iterator} to a new object applying the {@link #function} on {@link #next()}.
 *
 * @param <T> inbound iterator element type.
 * @param <V> outbound element type.
 *
 * @author Artem Bilan
 * @author Ruslan Stelmachenko
 * @author Gary Russell
 *
 * @since 4.1
 */
public class FunctionIterator<T, V> implements CloseableIterator<V> {

	private final AutoCloseable closeable;

	private final Iterator<T> iterator;

	private final Function<? super T, ? extends V> function;

	/**
	 * Construct an instance with the provided iterable and function.
	 * @param iterable the iterable.
	 * @param function the function.
	 */
	public FunctionIterator(Iterable<T> iterable, Function<? super T, ? extends V> function) {
		this(null, iterable.iterator(), function);
	}

	/**
	 * Construct an instance with the provided root object, iterable and function.
	 * @param closeable an {@link AutoCloseable} to close when iteration is complete.
	 * @param iterable the iterable.
	 * @param function the function.
	 * @since 5.0.7
	 */
	public FunctionIterator(@Nullable AutoCloseable closeable, Iterable<T> iterable,
			Function<? super T, ? extends V> function) {

		this(closeable, iterable.iterator(), function);
	}

	/**
	 * Construct an instance with the provided iterator and function.
	 * @param newIterator the iterator.
	 * @param function the function.
	 */
	public FunctionIterator(Iterator<T> newIterator, Function<? super T, ? extends V> function) {
		this(null, newIterator, function);
	}

	/**
	 * Construct an instance with the provided root object, iterator and function.
	 * @param closeable an {@link AutoCloseable} to close when iteration is complete.
	 * @param newIterator the iterator.
	 * @param function the function.
	 * @since 5.0.7
	 */
	public FunctionIterator(@Nullable AutoCloseable closeable, Iterator<T> newIterator,
			Function<? super T, ? extends V> function) {

		this.closeable = closeable;
		this.iterator = newIterator;
		this.function = function;
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public V next() {
		return this.function.apply(this.iterator.next());
	}

	@Override
	public void close() {
		if (this.iterator instanceof AutoCloseable) {
			try {
				((AutoCloseable) this.iterator).close();
			}
			catch (Exception e) {
				// NOSONAR
			}
		}
		if (this.closeable != null) {
			try {
				this.closeable.close();
			}
			catch (Exception e) {
				// NOSONAR
			}
		}
	}

}
