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

package org.springframework.integration.channel.factory;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAware;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;

/**
 * Creates a channel by delegating to the current message bus-configured
 * ChannelFactory.
 * @author Marius Bogoevici
 */
public class DefaultChannelFactoryBean implements FactoryBean, MessageBusAware, InitializingBean {

	private volatile ChannelFactory channelFactory;

	private volatile List<ChannelInterceptor> interceptors;

	private volatile DispatcherPolicy dispatcherPolicy;

	public void setMessageBus(MessageBus messageBus) {
		this.channelFactory = messageBus.getChannelFactory();
	}

	public void afterPropertiesSet() throws Exception {
		if (null == this.channelFactory) {
			this.channelFactory = new QueueChannelFactory();
		}

	}

	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public void setDispatcherPolicy(DispatcherPolicy dispatcherPolicy) {
		this.dispatcherPolicy = dispatcherPolicy;
	}

	public Object getObject() throws Exception {
		return channelFactory.getChannel(dispatcherPolicy, interceptors);
	}

	public Class getObjectType() {
		return MessageChannel.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
