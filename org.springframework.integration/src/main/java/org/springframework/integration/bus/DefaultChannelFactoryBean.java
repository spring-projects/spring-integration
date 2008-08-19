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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.factory.ChannelFactory;
import org.springframework.integration.channel.factory.QueueChannelFactory;

/**
 * Creates a channel by delegating to the "channelFactory" bean defined
 * within the {@link ApplicationContext} or else the default implementation
 * (QueueChannelFactory).
 * <p>
 * As a {@link FactoryBean}, this class is solely intended to be used within
 * an ApplicationContext.
 *
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class DefaultChannelFactoryBean implements ApplicationContextAware, FactoryBean, BeanNameAware {

	public static final String CHANNEL_FACTORY_BEAN_NAME = "channelFactory";

	private volatile String beanName;

	private volatile List<ChannelInterceptor> interceptors;

	private volatile ApplicationContext applicationContext;

	private volatile MessageChannel channel;

	private final Object initializationMonitor = new Object();


	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public Object getObject() throws Exception {
		synchronized (this.initializationMonitor) {
			if (this.channel == null) {
				ChannelFactory channelFactory = null;
				if (this.applicationContext.containsBean(CHANNEL_FACTORY_BEAN_NAME)) {
					channelFactory = (ChannelFactory) this.applicationContext.getBean(CHANNEL_FACTORY_BEAN_NAME);
				}
				else {
					channelFactory = new QueueChannelFactory();
				}
				this.channel = channelFactory.getChannel(this.beanName, this.interceptors);
			}
		}
		return this.channel;
	}

	public Class<?> getObjectType() {
		if (this.channel == null) {
			return MessageChannel.class;
		}
		return this.channel.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

}
