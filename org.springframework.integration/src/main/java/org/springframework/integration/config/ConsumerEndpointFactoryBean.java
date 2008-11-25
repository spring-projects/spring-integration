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

package org.springframework.integration.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class ConsumerEndpointFactoryBean implements FactoryBean, BeanFactoryAware, BeanNameAware, InitializingBean, ApplicationListener {

	private final MessageHandler handler;

	private volatile String beanName;

	private volatile String inputChannelName;

	private volatile PollerMetadata pollerMetadata;

	private volatile boolean autoStartup = true;

	private volatile ConfigurableBeanFactory beanFactory;

	private volatile AbstractEndpoint endpoint;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public ConsumerEndpointFactoryBean(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
	}


	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory,
				"a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		this.initializeEndpoint();
	}

	public boolean isSingleton() {
		return true;
	}

	public Object getObject() throws Exception {
		if (!this.initialized) {
			this.initializeEndpoint();
		}
		return this.endpoint;
	}

	public Class<?> getObjectType() {
		if (this.endpoint == null) {
			return AbstractEndpoint.class;
		}
		return this.endpoint.getClass();
	}

	private void initializeEndpoint() throws Exception {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.hasText(this.inputChannelName, "inputChannelName is required");
			Assert.isTrue(this.beanFactory.containsBean(this.inputChannelName),
					"no such input channel '" + this.inputChannelName + "' for endpoint '" + this.beanName + "'");
			MessageChannel channel = (MessageChannel)
					this.beanFactory.getBean(this.inputChannelName, MessageChannel.class);
			if (channel instanceof SubscribableChannel) {
				Assert.isNull(this.pollerMetadata, "A poller should not be specified for endpoint '" + this.beanName
						+ "', since '" + this.inputChannelName + "' is a SubscribableChannel (not pollable).");
				this.endpoint = new EventDrivenConsumer((SubscribableChannel) channel, this.handler);
			}
			else if (channel instanceof PollableChannel) {
				PollingConsumer pollingConsumer = new PollingConsumer(
						(PollableChannel) channel, this.handler);
				if (this.pollerMetadata == null) {
					this.pollerMetadata = IntegrationContextUtils.getDefaultPollerMetadata(this.beanFactory);
					Assert.notNull(this.pollerMetadata, "No poller has been defined for endpoint '"
							+ this.beanName + "', and no default poller is available within the context.");
				}
				pollingConsumer.setTrigger(this.pollerMetadata.getTrigger());
				pollingConsumer.setMaxMessagesPerPoll(this.pollerMetadata.getMaxMessagesPerPoll());
				pollingConsumer.setReceiveTimeout(this.pollerMetadata.getReceiveTimeout());
				pollingConsumer.setTaskExecutor(this.pollerMetadata.getTaskExecutor());
				pollingConsumer.setTransactionManager(this.pollerMetadata.getTransactionManager());
				pollingConsumer.setTransactionDefinition(this.pollerMetadata.getTransactionDefinition());
				this.endpoint = pollingConsumer;
			}
			else {
				throw new IllegalArgumentException(
						"unsupported channel type: [" + channel.getClass() + "]");
			}
			this.endpoint.setAutoStartup(this.autoStartup);
			this.endpoint.setBeanName(this.beanName);
			this.endpoint.setBeanFactory(this.beanFactory);
			if (this.endpoint instanceof InitializingBean) {
				((InitializingBean) this.endpoint).afterPropertiesSet();
			}
			this.initialized = true;
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (this.endpoint instanceof ApplicationListener) {
			((ApplicationListener) this.endpoint).onApplicationEvent(event);
		}
	}

}
