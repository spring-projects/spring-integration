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
package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.support.IdGenerators.JdkIdGenerator;
import org.springframework.integration.support.IdGenerators.SimpleIncrementingIdGenerator;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class IdGeneratorConfigurerTests {

	@Test
	public void testOneBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context.refresh();
		MessageHeaders headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(2, headers.getId().getLeastSignificantBits());
		context.destroy();
		headers = new MessageHeaders(null);
		assertNotEquals(1, headers.getId().getMostSignificantBits());
		assertNotEquals(2, headers.getId().getLeastSignificantBits());
		assertNull(TestUtils.getPropertyValue(headers, "idGenerator"));
	}

	@Test
	public void testTwoBeans() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(JdkIdGenerator.class));
		context.registerBeanDefinition("bar", new RootBeanDefinition(SimpleIncrementingIdGenerator.class));
		context.refresh();

		// multiple beans are ignored with warning
		MessageHeaders headers = new MessageHeaders(null);
		assertNull(TestUtils.getPropertyValue(headers, "idGenerator"));

		context.destroy();
	}

	@Test
	public void testNoBeans() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.refresh();

		MessageHeaders headers = new MessageHeaders(null);
		assertNull(TestUtils.getPropertyValue(headers, "idGenerator"));

		context.destroy();
	}

	@Test
	public void testTwoContextsSameClass() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context.refresh();
		MessageHeaders headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(2, headers.getId().getLeastSignificantBits());

		GenericApplicationContext context2 = new GenericApplicationContext();
		context2.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context2.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context2.refresh();

		context.destroy();
		context2.destroy();

		headers = new MessageHeaders(null);
		assertNotEquals(1, headers.getId().getMostSignificantBits());
		assertNotEquals(2, headers.getId().getLeastSignificantBits());

		assertNull(TestUtils.getPropertyValue(headers, "idGenerator"));
	}

	@Test
	public void testTwoContextsSameClassFirstDestroyed() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context.refresh();
		MessageHeaders headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(2, headers.getId().getLeastSignificantBits());

		GenericApplicationContext context2 = new GenericApplicationContext();
		context2.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context2.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context2.refresh();

		context.destroy();
		// we should still use the custom strategy
		headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(2, headers.getId().getLeastSignificantBits());

		context2.destroy();
		// back to default
		headers = new MessageHeaders(null);
		assertNotEquals(1, headers.getId().getMostSignificantBits());
		assertNotEquals(2, headers.getId().getLeastSignificantBits());

		assertNull(TestUtils.getPropertyValue(headers, "idGenerator"));
	}

	@Test
	public void testTwoContextDifferentClass() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context.refresh();
		MessageHeaders headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(2, headers.getId().getLeastSignificantBits());

		GenericApplicationContext context2 = new GenericApplicationContext();
		context2.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context2.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator2.class));
		try {
			context2.refresh();
			fail("Expected exception");
		}
		catch (BeanDefinitionStoreException e) {
			assertEquals("'MessageHeaders.idGenerator' has already been set and can not be set again",
					e.getMessage());
		}

		context.destroy();
		context2.destroy();
	}

	@Test
	public void testJdk() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(JdkIdGenerator.class));
		context.refresh();
		MessageHeaders headers = new MessageHeaders(null);
		assertSame(context.getBean(IdGenerator.class), TestUtils.getPropertyValue(headers, "idGenerator"));

		context.destroy();
	}

	@Test
	public void testIncrementing() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(SimpleIncrementingIdGenerator.class));
		context.refresh();
		IdGenerator idGenerator = context.getBean(IdGenerator.class);
		MessageHeaders headers = new MessageHeaders(null);
		assertEquals(0, headers.getId().getMostSignificantBits());
		assertEquals(1, headers.getId().getLeastSignificantBits());
		headers = new MessageHeaders(null);
		assertEquals(0, headers.getId().getMostSignificantBits());
		assertEquals(2, headers.getId().getLeastSignificantBits());
		AtomicLong bottomBits = TestUtils.getPropertyValue(idGenerator, "bottomBits", AtomicLong.class);
		bottomBits.set(0xffffffff);
		headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(0, headers.getId().getLeastSignificantBits());
		headers = new MessageHeaders(null);
		assertEquals(1, headers.getId().getMostSignificantBits());
		assertEquals(1, headers.getId().getLeastSignificantBits());

		context.destroy();
	}

	public static class MyIdGenerator implements IdGenerator {

		@Override
		public UUID generateId() {
			return new UUID(1, 2);
		}
	};

	public static class MyIdGenerator2 implements IdGenerator {

		@Override
		public UUID generateId() {
			return new UUID(3, 4);
		}
	};
}
