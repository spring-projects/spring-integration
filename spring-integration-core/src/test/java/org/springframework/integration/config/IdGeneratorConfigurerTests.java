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

package org.springframework.integration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

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
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class IdGeneratorConfigurerTests {

	@Test
	public void testOneBean() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
			context.refresh();
			MessageHeaders headers = new MessageHeaders(null);
			UUID id = headers.getId();
			assertThat(id.getMostSignificantBits()).isEqualTo(1);
			assertThat(id.getLeastSignificantBits()).isEqualTo(2);
		}

		MessageHeaders headers = new MessageHeaders(null);
		UUID id = headers.getId();
		assertThat(id.getMostSignificantBits()).isNotEqualTo(1);
		assertThat(id.getLeastSignificantBits()).isNotEqualTo(2);
		assertThat(TestUtils.getPropertyValue(headers, "idGenerator")).isNull();
	}

	@Test
	public void testTwoBeans() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.registerBeanDefinition("foo", new RootBeanDefinition(JdkIdGenerator.class));
			context.registerBeanDefinition("bar", new RootBeanDefinition(SimpleIncrementingIdGenerator.class));
			context.refresh();

			// multiple beans are ignored with warning
			MessageHeaders headers = new MessageHeaders(null);
			assertThat(TestUtils.getPropertyValue(headers, "idGenerator")).isNull();
		}
	}

	@Test
	public void testNoBeans() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.refresh();

			MessageHeaders headers = new MessageHeaders(null);
			assertThat(TestUtils.getPropertyValue(headers, "idGenerator")).isNull();
		}
	}

	@Test
	public void testTwoContextsSameClass() {
		try (GenericApplicationContext context = new GenericApplicationContext();
				GenericApplicationContext context2 = new GenericApplicationContext()) {

			context.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
			context.refresh();
			MessageHeaders headers = new MessageHeaders(null);
			UUID id = headers.getId();
			assertThat(id.getMostSignificantBits()).isEqualTo(1);
			assertThat(id.getLeastSignificantBits()).isEqualTo(2);

			context2.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context2.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
			context2.refresh();
		}

		MessageHeaders headers = new MessageHeaders(null);
		UUID id = headers.getId();
		assertThat(id.getMostSignificantBits()).isNotEqualTo(1);
		assertThat(id.getLeastSignificantBits()).isNotEqualTo(2);

		assertThat(TestUtils.getPropertyValue(headers, "idGenerator")).isNull();
	}

	@Test
	public void testTwoContextsSameClassFirstDestroyed() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bfpp",
				new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context.refresh();
		MessageHeaders headers = new MessageHeaders(null);
		UUID id = headers.getId();
		assertThat(id.getMostSignificantBits()).isEqualTo(1);
		assertThat(id.getLeastSignificantBits()).isEqualTo(2);

		GenericApplicationContext context2 = new GenericApplicationContext();
		context2.registerBeanDefinition("bfpp",
				new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
		context2.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
		context2.refresh();

		context.close();
		// we should still use the custom strategy
		headers = new MessageHeaders(null);
		id = headers.getId();
		assertThat(id.getMostSignificantBits()).isEqualTo(1);
		assertThat(id.getLeastSignificantBits()).isEqualTo(2);

		context2.close();
		// back to default
		headers = new MessageHeaders(null);
		id = headers.getId();
		assertThat(id.getMostSignificantBits()).isNotEqualTo(1);
		assertThat(id.getLeastSignificantBits()).isNotEqualTo(2);

		assertThat(TestUtils.getPropertyValue(headers, "idGenerator")).isNull();
	}

	@Test
	public void testTwoContextDifferentClass() {
		try (GenericApplicationContext context = new GenericApplicationContext();
				GenericApplicationContext context2 = new GenericApplicationContext()) {

			context.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator.class));
			context.refresh();
			MessageHeaders headers = new MessageHeaders(null);
			assertThat(headers.getId().getMostSignificantBits()).isEqualTo(1);
			assertThat(headers.getId().getLeastSignificantBits()).isEqualTo(2);

			context2.registerBeanDefinition("bfpp", new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context2.registerBeanDefinition("foo", new RootBeanDefinition(MyIdGenerator2.class));

			assertThatExceptionOfType(BeanDefinitionStoreException.class)
					.isThrownBy(context2::refresh)
					.withMessage("'MessageHeaders.idGenerator' has already been set and can not be set again");
		}
	}

	@Test
	public void testJdk() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.registerBeanDefinition("foo", new RootBeanDefinition(JdkIdGenerator.class));
			context.refresh();
			MessageHeaders headers = new MessageHeaders(null);
			assertThat(TestUtils.getPropertyValue(headers, "idGenerator")).isSameAs(context.getBean(IdGenerator.class));
		}
	}

	@Test
	public void testIncrementing() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBeanDefinition("bfpp",
					new RootBeanDefinition(DefaultConfiguringBeanFactoryPostProcessor.class));
			context.registerBeanDefinition("foo", new RootBeanDefinition(SimpleIncrementingIdGenerator.class));
			context.refresh();
			IdGenerator idGenerator = context.getBean(IdGenerator.class);
			MessageHeaders headers = new MessageHeaders(null);
			assertThat(headers.getId().getMostSignificantBits()).isEqualTo(0);
			assertThat(headers.getId().getLeastSignificantBits()).isEqualTo(1);
			headers = new MessageHeaders(null);
			assertThat(headers.getId().getMostSignificantBits()).isEqualTo(0);
			assertThat(headers.getId().getLeastSignificantBits()).isEqualTo(2);
			AtomicLong bottomBits = TestUtils.getPropertyValue(idGenerator, "bottomBits", AtomicLong.class);
			bottomBits.set(0xffffffff);
			headers = new MessageHeaders(null);
			assertThat(headers.getId().getMostSignificantBits()).isEqualTo(1);
			assertThat(headers.getId().getLeastSignificantBits()).isEqualTo(0);
			headers = new MessageHeaders(null);
			assertThat(headers.getId().getMostSignificantBits()).isEqualTo(1);
			assertThat(headers.getId().getLeastSignificantBits()).isEqualTo(1);
		}
	}

	public static class MyIdGenerator implements IdGenerator {

		@Override
		public UUID generateId() {
			return new UUID(1, 2);
		}

	}

	public static class MyIdGenerator2 implements IdGenerator {

		@Override
		public UUID generateId() {
			return new UUID(3, 4);
		}

	}

}
