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

package org.springframework.integration.aop;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Mark Fisher
 * @author Jeff Maxwell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class PublisherAnnotationAdvisorTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	@BeforeEach
	public void setup() {
		context.registerSingleton("testChannel", QueueChannel.class);
		context.registerSingleton("testMetaChannel", QueueChannel.class);
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	@Test
	public void annotationAtMethodLevelOnVoidReturnWithParamAnnotation() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new AnnotationAtMethodLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestVoidBean proxy = (TestVoidBean) pf.getProxy();
		proxy.testVoidMethod("foo");
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void annotationAtMethodLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new AnnotationAtMethodLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void annotationAtClassLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new AnnotationAtClassLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void metaAnnotationAtMethodLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testMetaChannel = context.getBean("testMetaChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new MetaAnnotationAtMethodLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testMetaChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void metaAnnotationAtClassLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testMetaChannel = context.getBean("testMetaChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new MetaAnnotationAtClassLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testMetaChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void annotationViaValueAtMethodLevelOnVoidReturnWithParamAnnotation() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new AnnotationViaValueAtMethodLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestVoidBean proxy = (TestVoidBean) pf.getProxy();
		proxy.testVoidMethod("foo");
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void annotationViaValueAtMethodLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new AnnotationViaValueAtMethodLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void annotationViaValueAtClassLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testChannel = context.getBean("testChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new AnnotationViaValueAtClassLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void metaAnnotationViaValueAtMethodLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testMetaChannel = context.getBean("testMetaChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new MetaAnnotationViaValueAtMethodLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testMetaChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void metaAnnotationViaValueAtClassLevel() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(context);
		QueueChannel testMetaChannel = context.getBean("testMetaChannel", QueueChannel.class);
		ProxyFactory pf = new ProxyFactory(new MetaAnnotationViaValueAtClassLevelTestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testMetaChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	interface TestBean {

		String test();

	}

	interface TestVoidBean {

		void testVoidMethod(String s);

	}

	static class AnnotationAtMethodLevelTestBeanImpl implements TestBean, TestVoidBean {

		@Override
		@Publisher(channel = "testChannel")
		public String test() {
			return "foo";
		}

		@Override
		@Publisher(channel = "testChannel")
		public void testVoidMethod(@Payload String s) {
		}

	}

	@Publisher(channel = "testChannel")
	static class AnnotationAtClassLevelTestBeanImpl implements TestBean {

		@Override
		public String test() {
			return "foo";
		}

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Publisher(channel = "testMetaChannel")
	public @interface TestMetaPublisher {

	}

	static class MetaAnnotationAtMethodLevelTestBeanImpl implements TestBean {

		@Override
		@TestMetaPublisher
		public String test() {
			return "foo";
		}

	}

	@TestMetaPublisher
	static class MetaAnnotationAtClassLevelTestBeanImpl implements TestBean {

		@Override
		public String test() {
			return "foo";
		}

	}

	static class AnnotationViaValueAtMethodLevelTestBeanImpl implements TestBean, TestVoidBean {

		@Override
		@Publisher("testChannel")
		public String test() {
			return "foo";
		}

		@Override
		@Publisher("testChannel")
		public void testVoidMethod(@Payload String s) {
		}

	}

	@Publisher("testChannel")
	static class AnnotationViaValueAtClassLevelTestBeanImpl implements TestBean {

		@Override
		public String test() {
			return "foo";
		}

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Publisher("testMetaChannel")
	public @interface TestMetaPublisherViaValue {

	}

	static class MetaAnnotationViaValueAtMethodLevelTestBeanImpl implements TestBean {

		@Override
		@TestMetaPublisherViaValue
		public String test() {
			return "foo";
		}

	}

	@TestMetaPublisherViaValue
	static class MetaAnnotationViaValueAtClassLevelTestBeanImpl implements TestBean {

		@Override
		public String test() {
			return "foo";
		}

	}

}
