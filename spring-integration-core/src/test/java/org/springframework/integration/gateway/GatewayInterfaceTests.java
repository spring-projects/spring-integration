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

package org.springframework.integration.gateway;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class GatewayInterfaceTests {

	@Test
	public void testWithServiceSuperclassAnnotatedMethod() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		final Method fooMethod = Foo.class.getMethod("foo", String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("foo"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Foo.foo(java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(fooMethod));
				assertThat((String) message.getPayload(), equalTo("hello"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.foo("hello");
		assertTrue(called.get());
	}

	@Test
	public void testWithServiceSuperclassAnnotatedMethodOverridePE() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests2-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		final Method fooMethod = Foo.class.getMethod("foo", String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("foo"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Foo.foo(java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(fooMethod));
				assertThat((String) message.getPayload(), equalTo("foo"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.foo("hello");
		assertTrue(called.get());
	}

	@Test
	public void testWithServiceAnnotatedMethod() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBar", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.bar("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithServiceSuperclassUnAnnotatedMethod() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		final Method bazMethod = Foo.class.getMethod("baz", String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("overrideGlobal"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Foo.baz(java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(bazMethod));
				assertThat((String) message.getPayload(), equalTo("hello"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.baz("hello");
		assertTrue(called.get());
	}

	@Test
	public void testWithServiceUnAnnotatedMethodGlobalHeaderDoesntOverride() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		final Method quxMethod = Bar.class.getMethod("qux", String.class, String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("arg1"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Bar.qux(java.lang.String,java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(quxMethod));
				assertThat((String) message.getPayload(), equalTo("hello"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.qux("hello", "arg1");
		assertTrue(called.get());
	}

	@Test
	public void testWithServiceCastAsSuperclassAnnotatedMethod() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Foo foo = ac.getBean(Foo.class);
		foo.foo("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithServiceCastAsSuperclassUnAnnotatedMethod() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Foo foo = ac.getBean(Foo.class);
		foo.baz("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithServiceHashcode() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		assertEquals(bar.hashCode(), ac.getBean(Bar.class).hashCode());
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithServiceToString() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.toString();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithServiceEquals() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		assertTrue(bar.equals(ac.getBean(Bar.class)));
		GatewayProxyFactoryBean fb = new GatewayProxyFactoryBean(Bar.class);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("requestChannelBar", channel);
		bf.registerSingleton("requestChannelBaz", channel);
		bf.registerSingleton("requestChannelFoo", channel);
		fb.setBeanFactory(bf);
		fb.afterPropertiesSet();
		assertFalse(bar.equals(fb.getObject()));
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testWithServiceGetClass() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.getClass();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithServiceAsNotAnInterface(){
		new GatewayProxyFactoryBean(NotAnInterface.class);
	}

	@Test
	public void testWithCustomMapper() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getPayload(), equalTo("fizbuz"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Baz baz = ac.getBean(Baz.class);
		baz.baz("hello");
		assertTrue(called.get());
	}



	public interface Foo {
		@Gateway(requestChannel="requestChannelFoo")
		public void foo(String payload);

		public void baz(String payload);
	}

	public static interface Bar extends Foo {
		@Gateway(requestChannel="requestChannelBar")
		public void bar(String payload);

		public void qux(String payload, @Header("name") String nameHeader);
	}

	public static class NotAnInterface {
		public void fail(String payload){}
	}

	public interface Baz {

		public void baz(String payload);
	}

	public static class BazMapper implements MethodArgsMessageMapper {

		@Override
		public Message<?> toMessage(MethodArgsHolder object) throws Exception {
			return MessageBuilder.withPayload("fizbuz").build();
		}

	}
}
