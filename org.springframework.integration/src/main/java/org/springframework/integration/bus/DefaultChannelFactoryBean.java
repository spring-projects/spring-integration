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

package org.springframework.integration.bus;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.factory.ChannelFactory;
import org.springframework.integration.channel.factory.QueueChannelFactory;
import org.springframework.util.Assert;

/**
 * Creates a channel by delegating to the current message bus' configured
 * ChannelFactory. Tries to retrieve the {@link ChannelFactory} from the
 * single {@link MessageBus} defined in the {@link ApplicationContext}. 
 * As a {@link FactoryBean}, this class is solely intended to be used within 
 * an ApplicationContext.
 * 
 * @author Marius Bogoevici
 */
public class DefaultChannelFactoryBean implements ApplicationContextAware, FactoryBean {

	private volatile ChannelFactory channelFactory;

	private volatile List<ChannelInterceptor> interceptors;

	private volatile DispatcherPolicy dispatcherPolicy;


	public DefaultChannelFactoryBean(DispatcherPolicy dispatcherPolicy) {
		this.dispatcherPolicy = dispatcherPolicy;
	}


	@SuppressWarnings("unchecked")
	public void setApplicationContext(ApplicationContext applicationContext){
		Map map = applicationContext.getBeansOfType(MessageBus.class);
		Assert.state(map.size() <= 1, "There is more than one MessageBus in the ApplicationContext");
		if (map.isEmpty()) {
			this.channelFactory = new QueueChannelFactory();
		}
		else {
			this.channelFactory = ((MessageBus) map.values().iterator().next()).getChannelFactory();
		}
	}

	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public Object getObject() throws Exception {
		Assert.notNull(channelFactory, "ChannelFactory not set on this instance. Is this used within an ApplicationContext?");
		return channelFactory.getChannel(dispatcherPolicy, interceptors);
	}

	public Class<?> getObjectType() {
		return MessageChannel.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
