/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.dispatcher;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Special List that maintains the following semantics:
 * All elements that are un-ordered (do not implement {@link Ordered} interface or annotated {@link Order} annotation)
 * will be stored in the order in which they were added, maintaining the semantics of the {@link LinkedHashSet}.
 * However, for all {@link Ordered} elements a {@link Comparator} (instantiated by default) for this implementation
 * of {@link Set}, will be used. Those elements will have precedence over un-ordered elements. If elements have the same
 * order but themselves do not equal to one another the more recent addition will be placed to the
 * right of (appended next to) the existing element with the same order, thus preserving the order
 * of the insertion and maintaining {@link LinkedHashSet} semantics for the un-ordered elements.
 * <p>
 * The class is package-protected and only intended for use by the AbstractDispatcher.
 * It <emphasis>must</emphasis> enforce safe concurrent access for all usage by the dispatcher.
 *
 * @author Diego Belfer
 * @since 2.2
 *
 */
public class OrderedAwareCopyOnWriteArrayList<E> extends AbstractList<E> implements RandomAccess, Cloneable, Serializable {

    private static final long serialVersionUID = -7739485563455787957L;

    private final transient OrderComparator comparator = new OrderComparator();

    private final transient ReentrantLock writeLock = new ReentrantLock();

    private final transient CopyOnWriteArrayList<E> elements;

    private final transient List<E> unmodifiableElements;

    public OrderedAwareCopyOnWriteArrayList() {
        elements = new CopyOnWriteArrayList<E>();
        unmodifiableElements = Collections.unmodifiableList(elements);
    }

    private OrderedAwareCopyOnWriteArrayList(CopyOnWriteArrayList<E> elements) {
        this.elements = elements;
        this.unmodifiableElements = Collections.unmodifiableList(elements);
    }

    @Override
    public boolean add(E o) {
        Assert.notNull(o, "Can not add NULL object");
        if (o instanceof Ordered) {
            // We only need to use to lock when adding an ordered element,
            // otherwise the CopyOnWriteArrayList's lock will take care of it
            writeLock.lock();
            try {
                addOrderedElement(o);
                return true;
            } finally {
                writeLock.unlock();
            }
        }
        return elements.addIfAbsent(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Assert.notNull(c, "Can not merge with NULL Collection");
        writeLock.lock();
        try {
            for (E object : c) {
                this.add(object);
            }
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        return elements.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (CollectionUtils.isEmpty(c)) {
            return false;
        }
        return elements.removeAll(c);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return elements.toArray(a);
    }

    @Override
    public String toString() {
        return StringUtils.collectionToCommaDelimitedString(elements);
    }

    public List<E> asUnmodifiableList() {
        return unmodifiableElements;
    }

    @Override
    public E get(int index) {
        return elements.get(index);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object clone() throws CloneNotSupportedException {
        return new OrderedAwareCopyOnWriteArrayList<E>((CopyOnWriteArrayList<E>) elements.clone());
    }

    private boolean addOrderedElement(E o) {
        int pos = 0;
        for (E e : elements) {
            //All ordered elements must be at front
            if (e instanceof Ordered) {
                int result = comparator.compare(o, e);
                if (result == 0 && o.equals(e)) {
                    return false;
                } else if (result < 0) {
                    break;
                }
                pos++;
            } else  {
                break;
            }
        }
        elements.add(pos, o);
        return true;
    }

    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();

        s.writeObject(elements);
    }

    @SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        elements.addAll((CopyOnWriteArrayList<E>) s.readObject());
    }
}