/*
 * Copyright 2014 the original author or authors.
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import reactor.function.Function;

/**
 * An {@link Iterator} implementation to convert each item from the target
 * {@link #iterator} to a new object applying the {@link #function} on {@link #next()}.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public final class FunctionIterator<T, V> implements Iterator<V> {

	private final Iterator<T> iterator;

	private final Function<? super T, ? extends V> function;

	public FunctionIterator(Iterable<T> iterable, Function<? super T, ? extends V> function) {
		this(iterable.iterator(), function);
	}

	public FunctionIterator(Iterator<T> newIterator, Function<? super T, ? extends V> function) {
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
		if (this.hasNext()) {
			return this.function.apply(this.iterator.next());
		}
		throw new NoSuchElementException();
	}

}

