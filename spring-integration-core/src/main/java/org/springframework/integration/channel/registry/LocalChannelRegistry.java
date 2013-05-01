/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.channel.registry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.util.Assert;

/**
 * A simple implementation of {@link ChannelRegistry} for in-process use. For inbound and outbound, creates a {@link DirectChannel} and bridges the passed {@link MessageChannel} to the new channel which is registered in the given application 
 * context. For tap, it adds a {@link WireTap} for the passed in channel which outputs to the registered channel.  
 *
 * @author David Turanski
 * @since 3.0
 *
 */
public class LocalChannelRegistry implements ChannelRegistry, ApplicationContextAware, InitializingBean {
	private AbstractApplicationContext applicationContext;

	/* (non-Javadoc)
	 * @see org.springframework.integration.module.registry.ChannelRegistry#inbound(java.lang.String, org.springframework.integration.MessageChannel)
	 */
	@Override
	public synchronized void inbound(String name, MessageChannel channel) {
		Assert.hasText(name, "A valid name is required to register an inbound channel");
		Assert.notNull(channel, "channel cannot be null");

		DirectChannel registeredChannel = lookupOrCreateSharedChannel(name, DirectChannel.class);
		bridge(registeredChannel, channel);
		createSharedTapChannelIfNecessary(registeredChannel);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.module.registry.ChannelRegistry#outbound(java.lang.String, org.springframework.integration.MessageChannel)
	 */
	@Override
	public synchronized void outbound(String name, MessageChannel channel) {
		Assert.hasText(name, "A valid name is required to register an outbound channel");
		Assert.notNull(channel, "channel cannot be null");
		Assert.isTrue(channel instanceof SubscribableChannel,
				"channel must be of type " + SubscribableChannel.class.getName());

		DirectChannel registeredChannel = lookupOrCreateSharedChannel(name, DirectChannel.class);
		bridge((SubscribableChannel) channel, registeredChannel);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.module.registry.ChannelRegistry#tap(java.lang.String, org.springframework.integration.MessageChannel)
	 */
	@Override
	public synchronized void tap(String name, MessageChannel channel) {
		Assert.hasText(name, "A valid name is required to register a tap channel");
		Assert.notNull(channel, "channel cannot be null");

		SubscribableChannel tapChannel = null;
		String tapName = name + ".tap";

		try {
			tapChannel = applicationContext.getBean(tapName, SubscribableChannel.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("No tap channel is exists for '" + name
					+ "'. A tap is only valid for a registered inbound channel.");
		}
		bridge(tapChannel, channel);
	}

	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		this.applicationContext = (AbstractApplicationContext) applicationContext;

	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(applicationContext, "The 'applicationContext' property cannot be null");
	}

	private <T extends AbstractMessageChannel> T lookupOrCreateSharedChannel(String name, Class<T> requiredType) {
		T channel = null;
		if (applicationContext.containsBean(name)) {
			try {
				channel = applicationContext.getBean(name, requiredType);
			} catch (Exception e) {
				throw new IllegalArgumentException("bean '" + name
						+ "' is already registered but does not match the required type");
			}
		} else {
			try {
				channel = requiredType.newInstance();
				channel.setComponentName(name);
				channel.setBeanFactory(applicationContext);
				channel.setBeanName(name);
				channel.afterPropertiesSet();
				applicationContext.getBeanFactory().registerSingleton(name, channel);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return channel;
	}

	private BridgeHandler bridge(SubscribableChannel from, MessageChannel to) {
		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(to);
		handler.afterPropertiesSet();
		from.subscribe(handler);
		return handler;
	}

	private void createSharedTapChannelIfNecessary(AbstractMessageChannel channel) {
		String tapName = channel.getComponentName() + ".tap";
		PublishSubscribeChannel tapChannel = null;
		if (!applicationContext.containsBean(tapName)) {
			//Will always create one here
			tapChannel = lookupOrCreateSharedChannel(tapName, PublishSubscribeChannel.class);

			WireTap wireTap = new WireTap(tapChannel);
			((AbstractMessageChannel) channel).addInterceptor(wireTap);

			// WireTap will fail if there is not at least one subscriber
			MessageHandler noOpTapHandler = new MessageHandler() {
				@Override
				public void handleMessage(Message<?> message) throws MessagingException {
				}
			};
			tapChannel.subscribe(noOpTapHandler);
		} else {
			try {
				tapChannel = applicationContext.getBean(tapName, PublishSubscribeChannel.class);
			} catch (Exception e) {
				throw new IllegalArgumentException("bean '" + tapName
						+ "' is already registered but does not match the required type");
			}
		}
	}
}
