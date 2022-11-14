/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.router;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
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
		assertThat(result).isNotNull();
		assertThat(result).isSameAs(message);
		context.close();
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
		assertThat(result).isNotNull();
		assertThat(result).isSameAs(message);

		// validate dynamics
		HeaderValueRouter router = (HeaderValueRouter) context.getBean("router");
		router.setChannelMapping("testChannel", "newChannel");
		router.handleMessage(message);
		QueueChannel newChannel = (QueueChannel) context.getBean("newChannel");
		result = newChannel.receive(10);
		assertThat(result).isNotNull();

		router.removeChannelMapping("testChannel");
		router.handleMessage(message);
		result = channel.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result).isSameAs(message);
		context.close();
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
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
		assertThat(result).isNotNull();
		assertThat(result).isSameAs(message);
		context.close();
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void resolveChannelNameFromMapAndCustomeResolver() {
		final StaticApplicationContext context = new StaticApplicationContext();
		ManagedMap channelMappings = new ManagedMap();
		channelMappings.put("testKey", "testChannel");
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		routerBeanDefinition.getPropertyValues().addPropertyValue("channelMappings", channelMappings);
		routerBeanDefinition.getPropertyValues().addPropertyValue("beanFactory", context);
		routerBeanDefinition.getPropertyValues().addPropertyValue("channelResolver",
				(DestinationResolver<MessageChannel>) channelName -> context.getBean("anotherChannel",
						MessageChannel.class));
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("anotherChannel", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", "testKey").build();
		handler.handleMessage(message);
		QueueChannel channel = (QueueChannel) context.getBean("anotherChannel");
		Message<?> result = channel.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result).isSameAs(message);
		context.close();
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
		String[] channels = new String[] {"channel1", "channel2"};
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", channels).build();
		handler.handleMessage(message);
		QueueChannel channel1 = (QueueChannel) context.getBean("channel1");
		QueueChannel channel2 = (QueueChannel) context.getBean("channel2");
		Message<?> result1 = channel1.receive(1000);
		Message<?> result2 = channel2.receive(1000);
		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		assertThat(result1).isSameAs(message);
		assertThat(result2).isSameAs(message);
		context.close();
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
		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		assertThat(result1).isSameAs(message);
		assertThat(result2).isSameAs(message);
		context.close();
	}

	@Test
	public void dynamicChannelCache() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition routerBeanDefinition = new RootBeanDefinition(HeaderValueRouter.class);
		routerBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue("testHeaderName");
		routerBeanDefinition.getPropertyValues().addPropertyValue("resolutionRequired", "true");
		routerBeanDefinition.getPropertyValues().addPropertyValue("dynamicChannelLimit", "2");
		context.registerBeanDefinition("router", routerBeanDefinition);
		context.registerBeanDefinition("channel1", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("channel2", new RootBeanDefinition(QueueChannel.class));
		context.registerBeanDefinition("channel3", new RootBeanDefinition(QueueChannel.class));
		context.refresh();
		MessageHandler handler = (MessageHandler) context.getBean("router");
		String channels = "channel1, channel2, channel1, channel3";
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeaderName", channels).build();
		handler.handleMessage(message);
		QueueChannel channel1 = (QueueChannel) context.getBean("channel1");
		QueueChannel channel2 = (QueueChannel) context.getBean("channel2");
		QueueChannel channel3 = (QueueChannel) context.getBean("channel3");
		assertThat(channel1.getQueueSize()).isEqualTo(2);
		assertThat(channel2.getQueueSize()).isEqualTo(1);
		assertThat(channel3.getQueueSize()).isEqualTo(1);
		assertThat(context.getBean(HeaderValueRouter.class).getDynamicChannelNames())
				.containsExactly("channel1", "channel3");
		context.close();
	}

}
