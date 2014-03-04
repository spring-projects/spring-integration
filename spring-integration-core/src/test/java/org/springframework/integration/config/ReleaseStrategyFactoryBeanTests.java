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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;

import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.SequenceSizeReleaseStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 3.0.2
 *
 */
public class ReleaseStrategyFactoryBeanTests {

	public void testRefWithMethod() throws Exception {
		Foo foo = new Foo();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(foo, "doRelease");
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate, instanceOf(MethodInvokingReleaseStrategy.class));
		assertThat(TestUtils.getPropertyValue(delegate, "adapter.delegate.targetObject", Foo.class), is(foo));
	}

	@Test
	public void testRefWithMethodWithDifferentAnnotatedMethod() throws Exception {
		Bar bar = new Bar();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(bar, "doRelease2");
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate, instanceOf(MethodInvokingReleaseStrategy.class));
		assertThat(TestUtils.getPropertyValue(delegate, "adapter.delegate.targetObject", Bar.class), is(bar));
		assertThat(TestUtils.getPropertyValue(delegate, "adapter.delegate.handlerMethod.expression.expression", String.class),
				equalTo("#target.doRelease2(messages)"));
	}

	@Test
	public void testRefWithNoMethodWithAnnotation() throws Exception {
		Bar bar = new Bar();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(bar);
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate, instanceOf(MethodInvokingReleaseStrategy.class));
		assertThat(TestUtils.getPropertyValue(delegate, "adapter.delegate.targetObject", Bar.class), is(bar));
	}

	@Test
	public void testNoRefNoMethod() throws Exception {
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(null);
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate, instanceOf(SequenceSizeReleaseStrategy.class));
	}

	@Test
	public void testRefWithNoMethodNoAnnotation() throws Exception {
		Foo foo = new Foo();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(foo);
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate, instanceOf(SequenceSizeReleaseStrategy.class));
	}

	@Test
	public void testRefThatImplements() throws Exception {
		Baz baz = new Baz();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(baz);
		ReleaseStrategy delegate = factory.getObject();
		assertThat((Baz) delegate, is(baz));
	}

	@Test
	public void testRefThatImplementsWithDifferentMethod() throws Exception {
		Baz baz = new Baz();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean(baz, "doRelease2");
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate, instanceOf(MethodInvokingReleaseStrategy.class));
		assertThat(TestUtils.getPropertyValue(delegate, "adapter.delegate.targetObject", Baz.class), is(baz));
		assertThat(TestUtils.getPropertyValue(delegate, "adapter.delegate.handlerMethod.expression.expression", String.class),
				equalTo("#target.doRelease2(messages)"));
	}

	public class Foo {

		boolean doRelease(Collection<?> foo) {
			return true;
		}

	}

	public class Bar {

		@org.springframework.integration.annotation.ReleaseStrategy
		public boolean doRelease(Collection<?> bar) {
			return true;
		}

		public boolean doRelease2(Collection<?> bar) {
			return true;
		}

	}

	public class Baz implements ReleaseStrategy {

		@Override
		public boolean canRelease(MessageGroup group) {
			return true;
		}

		public boolean doRelease2(Collection<?> bar) {
			return true;
		}

	}

}
