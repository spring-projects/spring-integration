/*
 * Copyright 2014-2018 the original author or authors.
 *
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

package org.springframework.integration.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

import org.springframework.lang.Nullable;

/**
 * An {@link Iterator} implementation to convert each item from the target
 * {@link #iterator} to a new object applying the {@link #function} on {@link #next()}.
 *
 * @author Artem Bilan
 * @author Ruslan Stelmachenko
 * @author Gary Russell
 * @since 4.1
 */
public class FunctionIterator<T, V> implements CloseableIterator<V> {

	private final Object root;

	private final Iterator<T> iterator;

	private final Function<? super T, ? extends V> function;

	/**
	 * Construct an instance with the provided iterable and function.
	 * @param iterable the iterable.
	 * @param function the function.
	 * @deprecated - use {@link #FunctionIterator(Object, Iterable, Function)}
	 */
	@Deprecated
	public FunctionIterator(Iterable<T> iterable, Function<? super T, ? extends V> function) {
		this(null, iterable.iterator(), function);
	}

	/**
	 * Construct an instance with the provided root object, iterable and function.
	 * @param root the root object.
	 * @param iterable the iterable.
	 * @param function the function.
	 * @since 5.0.7
	 */
	public FunctionIterator(@Nullable Object root, Iterable<T> iterable, Function<? super T, ? extends V> function) {
		this(null, iterable.iterator(), function);
	}

	/**
	 * Construct an instance with the provided iterator and function.
	 * @param iterator the iterator.
	 * @param function the function.
	 * @deprecated - use {@link #FunctionIterator(Object, Iterator, Function)}
	 */
	@Deprecated
	public FunctionIterator(Iterator<T> newIterator, Function<? super T, ? extends V> function) {
		this(null, newIterator, function);
	}

	/**
	 * Construct an instance with the provided root object, iterator and function.
	 * @param root the root object.
	 * @param iterator the iterator.
	 * @param function the function.
	 * @since 5.0.7
	 */
	public FunctionIterator(@Nullable Object root, Iterator<T> newIterator, Function<? super T, ? extends V> function) {
		this.root = root;
		this.iterator = newIterator;
		this.function = function;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Cannot remove from a collect iterator");
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
	public void close() throws IOException {
		if (this.iterator instanceof Closeable) {
			((Closeable) this.iterator).close();
		}
		if (!this.root.equals(this.iterator)) {
			if (this.root instanceof Closeable) {
				((Closeable) this.root).close();
			}
		}
	}

}
