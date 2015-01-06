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
package org.springframework.integration.config;

import static org.junit.Assert.assertSame;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author Gary Russell
 * @since 4.1.2
 *
 */
public class IntegrationEvaluationContextFactoryBeanTests {

	@Test
	public void testPropertyAccessorOrder1() throws Exception {
		IntegrationEvaluationContextFactoryBean factoryBean = new IntegrationEvaluationContextFactoryBean();
		Foo foo1 = new Foo();
		Foo foo2 = new Foo();
		Foo foo3 = new Foo();
		Foo foo4 = new Foo();
		Bar bar1 = new Bar(10);
		Bar bar2 = new Bar(10);
		Bar bar3 = new Bar(-10);
		Bar bar4 = new Bar(-10);
		Bar bar5 = new Bar(-20);
		Bar bar6 = new Bar(5);
		Map<String, PropertyAccessor> map = new LinkedHashMap<String, PropertyAccessor>();
		map.put("foo1", foo1); // unordered first
		map.put("foo2", foo2);
		map.put("bar3", bar3);
		map.put("bar2", bar2);
		map.put("bar1", bar1);
		map.put("bar4", bar4);
		map.put("foo3", foo3);
		map.put("foo4", foo4);
		map.put("bar5", bar5);
		map.put("bar6", bar6);
		factoryBean.setPropertyAccessors(map);
		factoryBean.afterPropertiesSet();
		StandardEvaluationContext context = factoryBean.getObject();
		List<PropertyAccessor> propertyAccessors = context.getPropertyAccessors();
		Iterator<PropertyAccessor> iterator = propertyAccessors.iterator();
		assertSame(foo1, iterator.next()); // unordered, then < 0; then reflect; then >= 0
		assertSame(foo2, iterator.next());
		assertSame(foo3, iterator.next());
		assertSame(foo4, iterator.next());
		assertSame(bar5, iterator.next());
		assertSame(bar3, iterator.next());
		assertSame(bar4, iterator.next());
		iterator.next(); // map
		iterator.next(); // reflective
		assertSame(bar6, iterator.next());
		assertSame(bar2, iterator.next());
		assertSame(bar1, iterator.next());
	}

	@Test
	public void testPropertyAccessorOrder2() throws Exception {
		IntegrationEvaluationContextFactoryBean factoryBean = new IntegrationEvaluationContextFactoryBean();
		Foo foo1 = new Foo();
		Foo foo2 = new Foo();
		Foo foo3 = new Foo();
		Foo foo4 = new Foo();
		Bar bar1 = new Bar(10);
		Bar bar2 = new Bar(10);
		Bar bar3 = new Bar(-10);
		Bar bar4 = new Bar(-10);
		Bar bar5 = new Bar(-20);
		Bar bar6 = new Bar(5);
		Map<String, PropertyAccessor> map = new LinkedHashMap<String, PropertyAccessor>();
		map.put("bar3", bar3); // ordered first
		map.put("bar2", bar2);
		map.put("foo1", foo1);
		map.put("foo2", foo2);
		map.put("bar1", bar1);
		map.put("bar4", bar4);
		map.put("foo3", foo3);
		map.put("foo4", foo4);
		map.put("bar5", bar5);
		map.put("bar6", bar6);
		factoryBean.setPropertyAccessors(map);
		factoryBean.afterPropertiesSet();
		StandardEvaluationContext context = factoryBean.getObject();
		List<PropertyAccessor> propertyAccessors = context.getPropertyAccessors();
		Iterator<PropertyAccessor> iterator = propertyAccessors.iterator();
		assertSame(foo1, iterator.next());
		assertSame(foo2, iterator.next());
		assertSame(foo3, iterator.next());
		assertSame(foo4, iterator.next());
		assertSame(bar5, iterator.next());
		assertSame(bar3, iterator.next());
		assertSame(bar4, iterator.next());
		iterator.next(); // map
		iterator.next(); // reflective
		assertSame(bar6, iterator.next());
		assertSame(bar2, iterator.next());
		assertSame(bar1, iterator.next());
	}

	public class Foo implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return null;
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
		}

	}

	public class Bar extends Foo implements Ordered {

		private final int order;

		public Bar(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

}
