/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;

/**
 * @author Oleg Zhurakousky
 *
 */
public class GatewayInterfaceTest {

	@Test
	public void testWithServiceSuperclassAnnotatedMethod(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.foo("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceAnnotatedMethod(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBar", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.bar("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceSuperclassUnAnnotatedMethod(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.baz("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceCastAsSuperclassAnnotatedMethod(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Foo foo = ac.getBean(Foo.class);
		foo.foo("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceCastAsSuperclassUnAnnotatedMethod(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Foo foo = ac.getBean(Foo.class);
		foo.baz("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceHashcode(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.hashCode();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceToString(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.toString();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceEquals(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.equals("");
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}
	
	@Test
	public void testWithServiceGetClass(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTest-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.getClass();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
	}

	
	public interface Foo {
		@Gateway(requestChannel="requestChannelFoo")
		public void foo(String payload);
		
		public void baz(String payload);
	}
	
	public static interface Bar extends Foo{
		@Gateway(requestChannel="requestChannelBar")
		public void bar(String payload);	
	}
}
