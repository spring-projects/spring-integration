/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class OrderedAwareCopyOnWriteArraySetTests {

	/**
	 * Tests that semantics of the LinkedHashSet were not broken
	 */
	@Test
	public void testAddUnordered() {
		OrderedAwareCopyOnWriteArraySet setToTest = new OrderedAwareCopyOnWriteArraySet();
		setToTest.add("foo");
		setToTest.add("bar");
		setToTest.add("baz");
		assertThat(setToTest.size()).isEqualTo(3);
		Object[] elements = setToTest.toArray();
		assertThat(elements[0]).isEqualTo("foo");
		assertThat(elements[1]).isEqualTo("bar");
		assertThat(elements[2]).isEqualTo("baz");
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
	public void testAddOrdered() {
		OrderedAwareCopyOnWriteArraySet setToTest = new OrderedAwareCopyOnWriteArraySet();
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
		assertThat(setToTest.size()).isEqualTo(10);
		Object[] elements = setToTest.toArray();
		assertThat(elements[0]).isEqualTo(o7);
		assertThat(elements[1]).isEqualTo(o8);
		assertThat(elements[2]).isEqualTo(o2);
		assertThat(elements[3]).isEqualTo(o3);
		assertThat(elements[4]).isEqualTo(o4);
		assertThat(elements[5]).isEqualTo(o10);
		assertThat(elements[6]).isEqualTo(o1);
		assertThat(elements[7]).isEqualTo(o9);
		assertThat(elements[8]).isEqualTo(o5);
		assertThat(elements[9]).isEqualTo(o6);
	}

	@Test
	public void testAddAllOrderedUnordered() {
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
		assertThat(tempList.size()).isEqualTo(10);
		OrderedAwareCopyOnWriteArraySet orderAwareSet = new OrderedAwareCopyOnWriteArraySet();
		orderAwareSet.addAll(tempList);
		Object[] elements = orderAwareSet.toArray();
		assertThat(elements[0]).isEqualTo(o7);
		assertThat(elements[1]).isEqualTo(o8);
		assertThat(elements[2]).isEqualTo(o2);
		assertThat(elements[3]).isEqualTo(o4);
		assertThat(elements[4]).isEqualTo(o1);
		assertThat(elements[5]).isEqualTo(o9);
		assertThat(elements[6]).isEqualTo(o5);
		assertThat(elements[7]).isEqualTo(o6);
		assertThat(elements[8]).isEqualTo(o3);
		assertThat(elements[9]).isEqualTo(o10);
	}

	@Test
	public void testConcurrent() throws InterruptedException {
		final OrderedAwareCopyOnWriteArraySet setToTest = new OrderedAwareCopyOnWriteArraySet();
		final Object o1 = new Foo(3);
		final Object o2 = new Foo(1);
		final Object o3 = new Foo(2);
		final Object o4 = new Foo(2);
		final Object o5 = new Foo(Ordered.LOWEST_PRECEDENCE);
		final Object o6 = new Foo(Ordered.LOWEST_PRECEDENCE);
		final Object o7 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		final Object o8 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		final Object o9 = new Foo(4);
		final Object o10 = new Foo(2);
		Thread t1 = new Thread(() -> {
			setToTest.add(o1);
			setToTest.add(o3);
			setToTest.add(o5);
			setToTest.add(o7);
			setToTest.add(o9);
		});
		Thread t2 = new Thread(() -> {
			setToTest.add(o2);
			setToTest.add(o4);
			setToTest.add(o6);
			setToTest.add(o8);
			setToTest.add(o10);
		});
		Thread t3 = new Thread(() -> {
			setToTest.add(1);
			setToTest.add(new Foo(2));
			setToTest.add(3);
			setToTest.add(new Foo(9));
			setToTest.add(8);
		});

		t1.start();
		t2.start();
		t3.start();
		t1.join();
		t2.join();
		t3.join();

		assertThat(setToTest).hasSize(15);
	}

	/**
	 * Will test addAll operation including the removal and adding an object in the concurrent environment
	 */
	@Test
	public void testConcurrentAll() throws InterruptedException {
		List tempList = new ArrayList();
		Object o1 = new Foo(3);
		Object o2 = new Foo(1);
		Object o3 = "Bla";
		Object o4 = new Foo(2);
		final Object o5 = new Foo(Ordered.LOWEST_PRECEDENCE);
		Object o6 = new Foo(Ordered.LOWEST_PRECEDENCE);
		final Object o7 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		Object o8 = new Foo(Ordered.HIGHEST_PRECEDENCE);
		Object o9 = new Foo(4);
		Object o10 = "Baz";

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
		final OrderedAwareCopyOnWriteArraySet orderAwareSet = new OrderedAwareCopyOnWriteArraySet();
		Thread t1 = new Thread(() -> {
			orderAwareSet.addAll(tempList);
			orderAwareSet.remove(o5);
			orderAwareSet.remove(o7);
		});
		final List tempList2 = new ArrayList();
		final Foo foo5 = new Foo(5);
		Foo foo6 = new Foo(6);
		tempList2.add(foo6);
		tempList2.add(foo5);
		tempList2.add(new Foo(30));
		tempList2.add(new Foo(10));
		tempList2.add(1);
		tempList2.add(new Foo(28));
		tempList2.add(10);
		tempList2.add(13);
		tempList2.add(new Foo(63));
		Thread t2 = new Thread(() -> {
			orderAwareSet.addAll(tempList2);
			orderAwareSet.remove(foo5);
		});
		Thread t3 = new Thread(() -> {
			orderAwareSet.add("hello");
			orderAwareSet.add("hello again");
		});

		t1.start();
		t2.start();
		t3.start();
		t1.join();
		t2.join();
		t3.join();

		assertThat(orderAwareSet).hasSize(18);
	}

	private static class Foo implements Ordered {

		private final int order;

		Foo(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return order;
		}

		@Override
		public String toString() {
			return "Foo-" + order;
		}

	}

}
