/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.SimpleSequenceSizeReleaseStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0.2
 */
public class ReleaseStrategyFactoryBeanTests {

	@Test
	public void testRefWithNoMethod() {
		Foo foo = new Foo();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.setTarget(foo);
		factory.setMethodName("doRelease");
		try {
			factory.afterPropertiesSet();
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(IllegalStateException.class);
			assertThat(e.getMessage()).contains("Target object of type " +
					"[class org.springframework.integration.config.ReleaseStrategyFactoryBeanTests$Foo] " +
					"has no eligible methods for handling Messages.");
		}
	}

	@Test
	public void testRefWithMethodWithDifferentAnnotatedMethod() throws Exception {
		ReleaseStrategyTest bar = new ReleaseStrategyTest();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.setTarget(bar);
		factory.setMethodName("doRelease2");
		factory.afterPropertiesSet();
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate).isInstanceOf(MethodInvokingReleaseStrategy.class);
		assertThat(TestUtils.<ReleaseStrategyTest>getPropertyValue(delegate, "adapter.delegate.targetObject"))
				.isEqualTo(bar);
		assertThat(TestUtils.<String>getPropertyValue(delegate, "adapter.delegate.handlerMethod.expressionString"))
				.isEqualTo("#target.doRelease2(messages)");
	}

	@Test
	public void testRefWithNoMethodWithAnnotation() throws Exception {
		ReleaseStrategyTest bar = new ReleaseStrategyTest();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.setTarget(bar);
		factory.afterPropertiesSet();
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate).isInstanceOf(MethodInvokingReleaseStrategy.class);
		assertThat(TestUtils.<ReleaseStrategyTest>getPropertyValue(delegate, "adapter.delegate.targetObject"))
				.isEqualTo(bar);
	}

	@Test
	public void testNoRefNoMethod() throws Exception {
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.afterPropertiesSet();
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate).isInstanceOf(SimpleSequenceSizeReleaseStrategy.class);
	}

	@Test
	public void testRefWithNoMethodNoAnnotation() throws Exception {
		Foo foo = new Foo();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.setTarget(foo);
		factory.afterPropertiesSet();
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate).isInstanceOf(SimpleSequenceSizeReleaseStrategy.class);
	}

	@Test
	public void testRefThatImplements() throws Exception {
		Baz baz = new Baz();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.setTarget(baz);
		factory.afterPropertiesSet();
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate).isEqualTo(baz);
	}

	@Test
	public void testRefThatImplementsWithDifferentMethod() throws Exception {
		Baz baz = new Baz();
		ReleaseStrategyFactoryBean factory = new ReleaseStrategyFactoryBean();
		factory.setTarget(baz);
		factory.setMethodName("doRelease2");
		factory.afterPropertiesSet();
		ReleaseStrategy delegate = factory.getObject();
		assertThat(delegate).isInstanceOf(MethodInvokingReleaseStrategy.class);
		assertThat(TestUtils.<Baz>getPropertyValue(delegate, "adapter.delegate.targetObject")).isEqualTo(baz);
		assertThat(TestUtils.<String>getPropertyValue(delegate, "adapter.delegate.handlerMethod.expressionString"))
				.isEqualTo("#target.doRelease2(messages)");
	}

	public class Foo {

		boolean doRelease(Collection<?> foo) {
			return true;
		}

	}

	public class ReleaseStrategyTest {

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
