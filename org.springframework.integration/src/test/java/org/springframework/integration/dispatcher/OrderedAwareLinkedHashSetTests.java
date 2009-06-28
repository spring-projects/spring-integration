/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.core.Ordered;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
public class OrderedAwareLinkedHashSetTests {

	/**
	 * Tests that semantics of the LinkedHashSet were not broken
	 */
	@Test
	public void testAddUnordered(){
		OrderedAwareLinkedHashSet setToTest = new OrderedAwareLinkedHashSet();
		setToTest.add("foo");
		setToTest.add("bar");
		setToTest.add("baz");
		assertEquals(3, setToTest.size());
		Object[] elements =  setToTest.toArray();
		assertEquals("foo", elements[0]);
		assertEquals("bar", elements[1]);
		assertEquals("baz", elements[2]);
	}
	/**
	 * Tests that semantics of TreeSet(Comparator) were not broken.
	 * However, there is a special Comparator (instantiated by default) for this implementation of Set, 
	 * which allows elements with the same "order" as long as these elements themselves are not equal.
	 * In this case element with the same order will be placed to the right (appended next to) of 
	 * the already existing element, thus preserving the order of insertion (LinkedHashset semantics) 
	 * within the elements that have the same "order" value. 
	 */
	@Test
	public void testAddOrdered(){
		OrderedAwareLinkedHashSet setToTest = new OrderedAwareLinkedHashSet();
		Object o1 = new Foo(3);
		Object o2 = new Foo(1);
		Object o3 = new Foo(2);
		Object o4 = new Foo(2);
		Object o5 = new Foo(Ordered.LOWEST_PRECEDENCE);
		Object o6 = new Foo(Ordered.LOWEST_PRECEDENCE);
		Object o7 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		Object o8 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		Object o9 = new Foo(4);
		Object o10 = new Foo(2);
		setToTest.add(o1);
		setToTest.add(o2);
		setToTest.add(o3);
		setToTest.add(o4);
		setToTest.add(o5);
		setToTest.add(o6);
		setToTest.add(o7);
		setToTest.add(o8);
		setToTest.add(o9);
		setToTest.add(o10);		
		assertEquals(10, setToTest.size());
		System.out.println(setToTest);
		Object[] elements = setToTest.toArray();
		assertEquals(o7, elements[0]);
		assertEquals(o8, elements[1]);
		assertEquals(o2, elements[2]);
		assertEquals(o3, elements[3]);
		assertEquals(o4, elements[4]);
		assertEquals(o10, elements[5]);
		assertEquals(o1, elements[6]);
		assertEquals(o9, elements[7]);
		assertEquals(o5, elements[8]);
		assertEquals(o6, elements[9]);
	}
	
	@Test
	public void testAddAllOrderedUnordered(){
		List tempList = new ArrayList();
		Object o1 = new Foo(3);
		Object o2 = new Foo(1);
		Object o3 = "FooA";
		Object o4 = new Foo(2);
		Object o5 = new Foo(Ordered.LOWEST_PRECEDENCE);
		Object o6 = new Foo(Ordered.LOWEST_PRECEDENCE);
		Object o7 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		Object o8 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		Object o9 = new Foo(4);
		Object o10 = "FooB";
		tempList.add(o1);
		tempList.add(o2);
		tempList.add(o3);
		tempList.add(o4);
		tempList.add(o5);
		tempList.add(o6);
		tempList.add(o7);
		tempList.add(o8);
		tempList.add(o9);
		tempList.add(o10);		
		assertEquals(10, tempList.size());
		OrderedAwareLinkedHashSet orderAwareSet = new OrderedAwareLinkedHashSet();
		orderAwareSet.addAll(tempList);
		System.out.println(orderAwareSet);
		Object[] elements = orderAwareSet.toArray();
		assertEquals(o7, elements[0]);
		assertEquals(o8, elements[1]);
		assertEquals(o2, elements[2]);
		assertEquals(o4, elements[3]);
		assertEquals(o1, elements[4]);
		assertEquals(o9, elements[5]);
		assertEquals(o5, elements[6]);
		assertEquals(o6, elements[7]);
		assertEquals(o3, elements[8]);
		assertEquals(o10, elements[9]);
	}
	
	private static class Foo implements Ordered {
		private int order;
		public Foo(int order){
			this.order = order;
		}
		public int getOrder() {
			return order;
		}	
		public String toString(){
			return "Foo-" + order;
		}
	}
}
