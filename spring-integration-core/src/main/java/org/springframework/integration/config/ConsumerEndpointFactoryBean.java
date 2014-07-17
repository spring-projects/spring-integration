/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.List;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Josh Long
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ConsumerEndpointFactoryBean
		implements FactoryBean<AbstractEndpoint>, BeanFactoryAware, BeanNameAware, BeanClassLoaderAware,
		InitializingBean, SmartLifecycle {

	private volatile MessageHandler handler;

	private volatile String beanName;

	private volatile String inputChannelName;

	private volatile PollerMetadata pollerMetadata;

	private volatile boolean autoStartup = true;

	private volatile int phase = 0;

	private volatile boolean isPhaseSet;

	private volatile MessageChannel inputChannel;

	private volatile ConfigurableBeanFactory beanFactory;

	private volatile ClassLoader beanClassLoader;

	private volatile AbstractEndpoint endpoint;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private final Object handlerMonitor = new Object();

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile List<Advice> adviceChain;

	public void setHandler(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		synchronized (this.handlerMonitor) {
			Assert.isNull(this.handler, "handler cannot be overridden");
			this.handler = handler;
		}
	}

	public void setInputChannel(MessageChannel inputChannel) {
		this.inputChannel = inputChannel;
	}

	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
		this.isPhaseSet = true;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory, "a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		Assert.notNull(adviceChain, "adviceChain must not be null");
		this.adviceChain = adviceChain;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			if (!this.beanName.startsWith("org.springframework")) {
				MessageHandler targetHandler = this.handler;
				if (AopUtils.isAopProxy(targetHandler)) {
					Object target = ((Advised) targetHandler).getTargetSource().getTarget();
					if (target instanceof MessageHandler) {
						targetHandler = (MessageHandler) target;
					}
				}
				if (targetHandler instanceof IntegrationObjectSupport) {
					((IntegrationObjectSupport) targetHandler).setComponentName(this.beanName);
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not set component name for handler "
						+ this.handler + " for " + this.beanName + " :" + e.getMessage());
			}
		}
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			/*
			 *  ARPMHs advise the handleRequesMessage method internally and already have the advice chain injected.
			 *  So we only advise handlers that are not reply-producing. If the handler is already advised,
			 *  add the configured advices to its chain, otherwise create a proxy.
			 */
			if (!(this.handler instanceof AbstractReplyProducingMessageHandler)) {
				if (AopUtils.isAopProxy(this.handler) && this.handler instanceof Advised) {
					Class<?> targetClass = AopUtils.getTargetClass(this.handler);
					for (Advice advice : this.adviceChain) {
						NameMatchMethodPointcutAdvisor handlerAdvice = new NameMatchMethodPointcutAdvisor(advice);
						handlerAdvice.addMethodName("handleMessage");
						if (AopUtils.canApply(handlerAdvice.getPointcut(), targetClass)) {
							((Advised) this.handler).addAdvice(advice);
						}
					}
				}
				else {
					ProxyFactory proxyFactory = new ProxyFactory(this.handler);
					for (Advice advice : this.adviceChain) {
						proxyFactory.addAdvice(advice);
					}
					this.handler = (MessageHandler) proxyFactory.getProxy(this.beanClassLoader);
				}
			}
		}
		this.initializeEndpoint();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public AbstractEndpoint getObject() throws Exception {
		if (!this.initialized) {
			this.initializeEndpoint();
		}
		return this.endpoint;
	}

	@Override
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
			MessageChannel channel = null;
			if (StringUtils.hasText(this.inputChannelName)) {
				Assert.isTrue(this.beanFactory.containsBean(this.inputChannelName), "no such input channel '"
						+ this.inputChannelName + "' for endpoint '" + this.beanName + "'");
				channel = this.beanFactory.getBean(this.inputChannelName, MessageChannel.class);
			}
			if (this.inputChannel != null) {
				channel = this.inputChannel;
			}
			Assert.state(channel != null, "one of inputChannelName or inputChannel is required");
			if (channel instanceof SubscribableChannel) {
				Assert.isNull(this.pollerMetadata, "A poller should not be specified for endpoint '" + this.beanName
						+ "', since '" + channel + "' is a SubscribableChannel (not pollable).");
				this.endpoint = new EventDrivenConsumer((SubscribableChannel) channel, this.handler);
				if (logger.isWarnEnabled() && !this.autoStartup && channel instanceof FixedSubscriberChannel) {
					logger.warn("'autoStartup=\"false\"' has no effect when using a FixedSubscriberChannel");
				}
			}
			else if (channel instanceof PollableChannel) {
				PollingConsumer pollingConsumer = new PollingConsumer((PollableChannel) channel, this.handler);
				if (this.pollerMetadata == null) {
					this.pollerMetadata = PollerMetadata.getDefaultPollerMetadata(this.beanFactory);
					Assert.notNull(this.pollerMetadata, "No poller has been defined for endpoint '" + this.beanName
							+ "', and no default poller is available within the context.");
				}
				pollingConsumer.setTaskExecutor(this.pollerMetadata.getTaskExecutor());
				pollingConsumer.setTrigger(this.pollerMetadata.getTrigger());
				pollingConsumer.setAdviceChain(this.pollerMetadata.getAdviceChain());
				pollingConsumer.setMaxMessagesPerPoll(this.pollerMetadata.getMaxMessagesPerPoll());

				pollingConsumer.setErrorHandler(this.pollerMetadata.getErrorHandler());

				pollingConsumer.setReceiveTimeout(this.pollerMetadata.getReceiveTimeout());
				pollingConsumer.setTransactionSynchronizationFactory(
						this.pollerMetadata.getTransactionSynchronizationFactory());
				pollingConsumer.setBeanClassLoader(beanClassLoader);
				pollingConsumer.setBeanFactory(beanFactory);
				this.endpoint = pollingConsumer;
			}
			else {
				throw new IllegalArgumentException("unsupported channel type: [" + channel.getClass() + "]");
			}
			this.endpoint.setBeanName(this.beanName);
			this.endpoint.setBeanFactory(this.beanFactory);
			this.endpoint.setAutoStartup(this.autoStartup);
			int phase = this.phase;
			if (!this.isPhaseSet && this.endpoint instanceof PollingConsumer) {
				phase = Integer.MAX_VALUE / 2;
			}
			this.endpoint.setPhase(phase);
			this.endpoint.afterPropertiesSet();
			this.initialized = true;
		}
	}


	/*
	 * SmartLifecycle implementation (delegates to the created endpoint)
	 */

	@Override
	public boolean isAutoStartup() {
		return (this.endpoint == null) || this.endpoint.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return (this.endpoint != null) ? this.endpoint.getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return (this.endpoint != null) && this.endpoint.isRunning();
	}

	@Override
	public void start() {
		if (this.endpoint != null) {
			this.endpoint.start();
		}
	}

	@Override
	public void stop() {
		if (this.endpoint != null) {
			this.endpoint.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.endpoint != null) {
			this.endpoint.stop(callback);
		}
	}

}
