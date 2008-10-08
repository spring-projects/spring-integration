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

package org.springframework.integration.endpoint.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class ConsumerEndpointFactoryBean implements FactoryBean, ChannelRegistryAware, BeanFactoryAware, InitializingBean {

	private final MessageConsumer consumer;

	private volatile String inputChannelName;

	private volatile Trigger trigger;

	private volatile int maxMessagesPerPoll;

	private volatile long receiveTimeout = 1000;

	private volatile TaskExecutor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionDefinition transactionDefinition;

	private volatile ConfigurableBeanFactory beanFactory;

	private volatile MessageEndpoint endpoint;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public ConsumerEndpointFactoryBean(MessageConsumer consumer) {
		Assert.notNull(consumer, "consumer must not be null");
		this.consumer = consumer;
	}


	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		if (this.consumer instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.consumer).setChannelRegistry(channelRegistry);
		}
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTransactionDefinition(TransactionDefinition transactionDefinition) {
		this.transactionDefinition = transactionDefinition;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory,
				"a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() {
		Assert.hasText(this.inputChannelName, "inputChannelName is required");
	}

	public Object getObject() throws Exception {
		if (!this.initialized) {
			this.initializeEndpoint();
		}
		return this.endpoint;
	}

	public Class<?> getObjectType() {
		if (this.endpoint == null) {
			return MessageEndpoint.class;
		}
		return endpoint.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

	private void initializeEndpoint() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.isTrue(this.beanFactory.containsBean(this.inputChannelName),
					"no such input channel '" + this.inputChannelName + "'");
			MessageChannel channel = (MessageChannel)
					this.beanFactory.getBean(this.inputChannelName, MessageChannel.class);
			if (channel instanceof SubscribableChannel) {
				Assert.isNull(trigger, "A trigger should not be specified when using a SubscribableChannel");
				this.endpoint = new SubscribingConsumerEndpoint(
						this.consumer, (SubscribableChannel) channel);
			}
			else if (channel instanceof PollableChannel) {
				if (this.trigger == null) {
					this.trigger = new IntervalTrigger(0);
				}
				PollingConsumerEndpoint pollingEndpoint = new PollingConsumerEndpoint(
						this.consumer, (PollableChannel) channel);
				pollingEndpoint.setTrigger(this.trigger);
				pollingEndpoint.setMaxMessagesPerPoll(this.maxMessagesPerPoll);
				pollingEndpoint.setReceiveTimeout(this.receiveTimeout);
				pollingEndpoint.setTaskExecutor(this.taskExecutor);
				pollingEndpoint.setTransactionManager(this.transactionManager);
				pollingEndpoint.setTransactionDefinition(this.transactionDefinition);
				this.endpoint = pollingEndpoint;
			}
			else {
				throw new IllegalArgumentException(
						"unsupported channel type: [" + channel.getClass() + "]");
			}
			this.initialized = true;
		}
	}

}
