/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Special Set that maintains the following semantics:
 * All elements that are un-ordered (do not implement {@link Ordered} interface or annotated
 * {@link Order} annotation) will be stored in the order in which they were added.
 * However, for all {@link Ordered} elements a
 * {@link Comparator} (instantiated by default) for this implementation of {@link Set}, will be
 * used. Those elements will have precedence over un-ordered elements. If elements have the same
 * order but themselves do not equal to one another the more recent addition will be placed to the
 * right of (appended next to) the existing element with the same order, thus preserving the order
 * of the insertion while maintaining the order of insertion for the un-ordered elements.
 * <p>
 * The class is package-protected and only intended for use by the AbstractDispatcher. It
 * <em>must</em> enforce safe concurrent access for all usage by the dispatcher.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Diego Belfer
 * @since 1.0.3
 */
@SuppressWarnings({"unchecked"})
class OrderedAwareCopyOnWriteArraySet<E> implements Set<E> {

	private final OrderComparator comparator = new OrderComparator();

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	private final ReadLock readLock = rwl.readLock();

	private final WriteLock writeLock = rwl.writeLock();

	private final CopyOnWriteArraySet<E> elements;

    private final Set<E> unmodifiableElements;

    public OrderedAwareCopyOnWriteArraySet() {
        elements = new CopyOnWriteArraySet<E>();
        unmodifiableElements = Collections.unmodifiableSet(elements);
    }

    public Set<E> asUnmodifiableSet() {
        return unmodifiableElements;
    }


	/**
	 * Every time an Ordered element is added via this method this
	 * Set will be re-sorted, otherwise the element is simply added
	 * to the end. Added element must not be null.
	 */
	public boolean add(E o) {
		Assert.notNull(o,"Can not add NULL object");
		writeLock.lock();
		try {
			boolean present = false;
			if (o instanceof Ordered){
				present = this.addOrderedElement((Ordered) o);
			}
			else {
				present = elements.add(o);
			}
			return present;
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * Adds all elements in this Collection.
	 */
	public boolean addAll(Collection<? extends E> c) {
		Assert.notNull(c,"Can not merge with NULL set");
		writeLock.lock();
		try {
			for (E object : c) {
				this.add(object);
			}
			return true;
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(Object o) {
		writeLock.lock();
		try {
			boolean removed = elements.remove(o);
			//unmodifiableElements = Collections.unmodifiableSet(this);
			return removed;
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeAll(Collection<?> c){
		if (CollectionUtils.isEmpty(c)){
			return false;
		}
		writeLock.lock();
		try {
			return elements.removeAll(c);
		}
		finally {
			writeLock.unlock();
		}
	}

	public <T> T[] toArray(T[] a) {
		readLock.lock();
		try {
			return elements.toArray(a);
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public String toString() {
		readLock.lock();
		try {
			return StringUtils.collectionToCommaDelimitedString(elements);
		}
		finally {
			readLock.unlock();
		}
	}

	@SuppressWarnings("rawtypes")
	private boolean addOrderedElement(Ordered adding) {
		boolean added = false;
		E[] tempUnorderedElements = (E[]) elements.toArray();
		if (elements.contains(adding)) {
			return false;
		}
		elements.clear();

		if (tempUnorderedElements.length == 0) {
			added = elements.add((E) adding);
		}
		else {
			Set tempSet = new LinkedHashSet();
			for (E current : tempUnorderedElements) {
				if (current instanceof Ordered) {
					if (this.comparator.compare(adding, current) < 0) {
						added = elements.add((E) adding);
						elements.add(current);
					}
					else {
						elements.add(current);
					}
				}
				else {
					tempSet.add(current);
				}
			}
			if (!added) {
				added = elements.add((E) adding);
			}
			for (Object object : tempSet) {
				elements.add((E) object);
			}
		}
		return added;
	}

    public Iterator<E> iterator() {
        return this.elements.iterator();
    }

	public int size(){
		return this.elements.size();
	}

	public boolean isEmpty() {
		return this.elements.isEmpty();
	}

	public boolean contains(Object o) {
		return this.elements.contains(o);
	}

	public Object[] toArray() {
		return this.elements.toArray();
	}

	public boolean containsAll(Collection<?> c) {
		return this.elements.containsAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return this.elements.retainAll(c);
	}

	public void clear() {
		this.elements.clear();
	}
}
