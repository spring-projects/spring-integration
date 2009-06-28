/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
/**
 *
 * Special Set that maintains the following semantics:
 * All elements that are un-ordered (do not implement {@link Ordered} interface or annotated {@link Order} annotation) 
 * will be stored in the order in which they were added, maintaining the semantics of the {@link LinkedHashSet}.
 * However, there is a special {@link Comparator} (instantiated by default) for this implementation of {@link Set}, 
 * which is aware of the {@link Ordered} interface or and {@link Order} annotation. Those elements will have 
 * precedence over un-ordered elements. If elements have the same order but themselves do not equal to one another
 * they will be placed to the right (appended next to) of the element with the same order, thus preserving the order 
 * of the insertion and maintaining {@link LinkedHashSet} semantics.
 * 
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
class OrderedAwareLinkedHashSet<E> extends LinkedHashSet<E> {
	private TreeSet orderedSet = new TreeSet(new EqualsAwareOrderComparator());
	/**
	 * Every time when Ordered element is added via this method
	 * this Set will be re-sorted, otherwise the element is simply added to the end of the stack.
	 * If adding multiple objects for performance reasons it is recommended to use
	 * addAll(Collection c) method which first adds all the elements to this set
	 * and then calls reinitializeThis() method.
	 */
	public boolean add(E o){
		boolean present = this.doAdd(o);
		if (o instanceof Ordered){
			this.reinitializeThis();
		}	
		return present;
	}
	/**
	 * Adds all elements in this Collection and then resorts this set
	 * via call to the reinitializeThis() method
	 */
	public boolean addAll(Collection<? extends E> c){
		Assert.notNull(c,"Can not merge with NULL set");
		if (CollectionUtils.isEmpty(c)){
			return false;
		}
		for (E object : c) {
			this.doAdd(object);
		}		
		this.reinitializeThis();
		// due to the nature of previous method
		// at this point collection will always be modified
		return true;
	}
	/**
	 * 
	 * @param o
	 * @return
	 */
	private boolean doAdd(E o){
		if (o instanceof Ordered){
			return orderedSet.add(o);
		} else {
			return super.add(o);
		}
	}
	/**
	 * 
	 */
	private void reinitializeThis(){
		E[] tempUnorderedElements = (E[]) super.toArray();
		E[] tempOrderedElements = (E[]) orderedSet.toArray();
		super.clear();
		for (E object : tempOrderedElements) {
			super.add(object);
		}
		for (E object : tempUnorderedElements) {
			super.add(object);
		}
	}
	/**
	 * Will reuse most of the functionality of OrderComparator, however if 
	 * elements have the same order, then 1 will be returned, thus positioning 
	 * such element to the right of the existing element.
	 */
	private static class EqualsAwareOrderComparator extends OrderComparator {
		public int compare(Object o1, Object o2) {
			int value = super.compare(o1, o2);
			// see if objects are not equal
			if (value == 0 && !o1.equals(o2)){
				return 1;
			}
			return value;
		}
	}
}
