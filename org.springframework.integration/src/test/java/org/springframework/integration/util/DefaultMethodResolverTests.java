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

package org.springframework.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.ServiceActivatorEndpoint;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultMethodResolverTests {

	@Test
	public void singleAnnotation() {
		DefaultMethodResolver resolver = new DefaultMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(SingleAnnotationTestBean.class);
		assertNotNull(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotations() {
		DefaultMethodResolver resolver = new DefaultMethodResolver(TestAnnotation.class);
		resolver.findMethod(MultipleAnnotationTestBean.class);
	}

	@Test
	public void singlePublicMethod() {
		DefaultMethodResolver resolver = new DefaultMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(SinglePublicMethodTestBean.class);
		assertNotNull(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethods() {
		DefaultMethodResolver resolver = new DefaultMethodResolver(TestAnnotation.class);
		resolver.findMethod(MultiplePublicMethodTestBean.class);
	}

	@Test
	public void noPublicMethods() {
		DefaultMethodResolver resolver = new DefaultMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(NoPublicMethodTestBean.class);
		assertNull(method);
	}

	@Test
	public void jdkProxy() {
		DirectChannel input = new DirectChannel();
		QueueChannel output = new QueueChannel();
		GreetingService testBean = new GreetingBean();
		ProxyFactory proxyFactory = new ProxyFactory(testBean);
		proxyFactory.setProxyTargetClass(false);
		testBean = (GreetingService) proxyFactory.getProxy();
		ServiceActivatorEndpoint consumer = new ServiceActivatorEndpoint(testBean);
		consumer.setOutputChannel(output);
		SubscribingConsumerEndpoint endpoint = new SubscribingConsumerEndpoint(consumer, input);
		endpoint.start();
		input.send(new StringMessage("proxy"));
		assertEquals("hello proxy", output.receive(0).getPayload());;
	}

	@Test
	public void cglibProxy() {
		DirectChannel input = new DirectChannel();
		QueueChannel output = new QueueChannel();
		GreetingService testBean = new GreetingBean();
		ProxyFactory proxyFactory = new ProxyFactory(testBean);
		proxyFactory.setProxyTargetClass(true);
		testBean = (GreetingService) proxyFactory.getProxy();
		ServiceActivatorEndpoint consumer = new ServiceActivatorEndpoint(testBean);
		consumer.setOutputChannel(output);
		SubscribingConsumerEndpoint endpoint = new SubscribingConsumerEndpoint(consumer, input);
		endpoint.start();
		input.send(new StringMessage("proxy"));
		assertEquals("hello proxy", output.receive(0).getPayload());;
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation {
	}


	private static class SingleAnnotationTestBean {

		@TestAnnotation
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class MultipleAnnotationTestBean {

		@TestAnnotation
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		@TestAnnotation
		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class SinglePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class MultiplePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	private static class NoPublicMethodTestBean {

		String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	public interface GreetingService {

		String sayHello(String s);

	}


	public static class GreetingBean implements GreetingService {

		private String greeting = "hello";

		public void setGreeting(String greeting) {
			this.greeting = greeting;
		}

		@ServiceActivator
		public String sayHello(String name) {
			return greeting + " " + name;
		}

	}

}
