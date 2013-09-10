/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.router;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class HeaderValueRouterTests {

	@Test
	public void channelAsHeaderValue() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		QueueChannel testChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", testChannel).build();
		handler.handleMessage(message);
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertSame(message, result);
	}

	@Test
	public void resolveChannelNameFromContext() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("newChannel", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", "testChannel").build();
		handler.handleMessage(message);
		QueueChannel channel = (QueueChannel) context.getBean("testChannel");
		Message<?> result = channel.receive(1000);
		assertNotNull(result);
		assertSame(message, result);

		// validate dynamics
		HeaderValueRouter router = (HeaderValueRouter) context.getBean("router");
		router.setChannelMapping("testChannel", "newChannel");
		router.handleMessage(message);
		QueueChannel newChannel = (QueueChannel) context.getBean("newChannel");
		result = newChannel.receive(10);
		assertNotNull(result);

		router.removeChannelMapping("testChannel");
		router.handleMessage(message);
		result = channel.receive(1000);
		assertNotNull(result);
		assertSame(message, result);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void resolveChannelNameFromMap() {
		StaticApplicationContext context = new StaticApplicationContext();
		ManagedMap channelMappings = new ManagedMap();
		channelMappings.put("testKey", "testChannel");
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		routerBeanDefinition.getPropertyValues().addPropertyValue("channelMappings", channelMappings);
		routerBeanDefinition.getPropertyValues().addPropertyValue("beanFactory", context);
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", "testKey").build();
		handler.handleMessage(message);
		QueueChannel channel = (QueueChannel) context.getBean("testChannel");
		Message<?> result = channel.receive(1000);
		assertNotNull(result);
		assertSame(message, result);
	}
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void resolveChannelNameFromMapAndCustomeResolver() {
		final StaticApplicationContext context = new StaticApplicationContext();
		ManagedMap channelMappings = new ManagedMap();
		channelMappings.put("testKey", "testChannel");
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		routerBeanDefinition.getPropertyValues().addPropertyValue("channelMappings", channelMappings);
		routerBeanDefinition.getPropertyValues().addPropertyValue("beanFactory", context);
		routerBeanDefinition.getPropertyValues().addPropertyValue("channelResolver", new DestinationResolver<MessageChannel>() {
			public MessageChannel resolveDestination(String channelName) {
				return context.getBean("anotherChannel", MessageChannel.class);
			}
		});
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("anotherChannel", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", "testKey").build();
		handler.handleMessage(message);
		QueueChannel channel = (QueueChannel) context.getBean("anotherChannel");
		Message<?> result = channel.receive(1000);
		assertNotNull(result);
		assertSame(message, result);
	}

	@Test
	public void resolveMultipleChannelsWithStringArray() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("channel1", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("channel2", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		String[] channels = new String[] { "channel1", "channel2" };
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", channels).build();
		handler.handleMessage(message);
		QueueChannel channel1 = (QueueChannel) context.getBean("channel1");
		QueueChannel channel2 = (QueueChannel) context.getBean("channel2");
		Message<?> result1 = channel1.receive(1000);
		Message<?> result2 = channel2.receive(1000);
		assertNotNull(result1);
		assertNotNull(result2);
		assertSame(message, result1);
		assertSame(message, result2);
	}

	@Test
	public void resolveMultipleChannelsWithCommaDelimitedString() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("channel1", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("channel2", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		String channels = "channel1, channel2";
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", channels).build();
		handler.handleMessage(message);
		QueueChannel channel1 = (QueueChannel) context.getBean("channel1");
		QueueChannel channel2 = (QueueChannel) context.getBean("channel2");
		Message<?> result1 = channel1.receive(1000);
		Message<?> result2 = channel2.receive(1000);
		assertNotNull(result1);
		assertNotNull(result2);
		assertSame(message, result1);
		assertSame(message, result2);
	}


}
