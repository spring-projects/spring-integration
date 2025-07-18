/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Factory bean to create and configure a {@link MessageHandler}.
 *
 * @param <H> the target message handler type.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 * @author Christian Tzolov
 * @author Ngoc Nhan
 */
public abstract class AbstractSimpleMessageHandlerFactoryBean<H extends MessageHandler>
		implements FactoryBean<MessageHandler>, ApplicationContextAware, BeanFactoryAware, BeanNameAware,
		ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(getClass()); //NOSONAR protected with final

	private final Lock initializationMonitor = new ReentrantLock();

	@SuppressWarnings("NullAway.Init")
	private BeanFactory beanFactory;

	@SuppressWarnings("NullAway.Init")
	private H handler;

	private @Nullable MessageChannel outputChannel;

	private @Nullable String outputChannelName;

	private @Nullable Integer order;

	private @Nullable List<Advice> adviceChain;

	private @Nullable String componentName;

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	@SuppressWarnings("NullAway.Init")
	private String beanName;

	private @Nullable ApplicationEventPublisher applicationEventPublisher;

	private @Nullable DestinationResolver<MessageChannel> channelResolver;

	private @Nullable Boolean async;

	private boolean initialized;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Set the handler's channel resolver.
	 * @param channelResolver the channel resolver to set.
	 */
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		this.channelResolver = channelResolver;
	}

	/**
	 * Set the handler's output channel.
	 * @param outputChannel the output channel to set.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Set the handler's output channel name.
	 * @param outputChannelName the output channel bean name to set.
	 * @since 5.1.4
	 */
	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	/**
	 * Set the order in which the handler will be subscribed to its channel
	 * (when subscribable).
	 * @param order the order to set.
	 */
	public void setOrder(Integer order) {
		this.order = order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Set the advice chain to be configured within an
	 * {@link AbstractReplyProducingMessageHandler} to advise just this local endpoint.
	 * For other handlers, the advice chain is applied around the handler itself.
	 * @param adviceChain the adviceChain to set.
	 */
	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	/**
	 * Currently only exposed on the service activator
	 * namespace. It's not clear that other endpoints would benefit from async support,
	 * but any subclass of {@link AbstractReplyProducingMessageHandler} can potentially
	 * return a {@code ListenableFuture<?>}.
	 * @param async the async to set.
	 * @since 4.3
	 */
	public void setAsync(Boolean async) {
		this.async = async;
	}

	/**
	 * Sets the name of the handler component.
	 *
	 * @param componentName The component name.
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	@Override
	public H getObject() {
		if (this.handler == null) {
			this.handler = createHandlerInternal();
		}
		return this.handler;
	}

	protected final H createHandlerInternal() {
		this.initializationMonitor.lock();
		try {
			Assert.state(!this.initialized, "FactoryBean already initialized");
			this.handler = createHandler();
			JavaUtils.INSTANCE
					.acceptIfCondition(this.handler instanceof ApplicationContextAware && this.applicationContext != null,
							this.applicationContext,
							context -> ((ApplicationContextAware) Objects.requireNonNull(this.handler)).setApplicationContext(this.applicationContext))
					.acceptIfCondition(this.handler instanceof BeanFactoryAware && getBeanFactory() != null,
							getBeanFactory(),
							factory -> ((BeanFactoryAware) Objects.requireNonNull(this.handler)).setBeanFactory(factory))
					.acceptIfCondition(this.handler instanceof BeanNameAware && this.beanName != null, this.beanName,
							name -> ((BeanNameAware) Objects.requireNonNull(this.handler)).setBeanName(this.beanName))
					.acceptIfCondition(this.handler instanceof ApplicationEventPublisherAware
									&& this.applicationEventPublisher != null,
							this.applicationEventPublisher,
							publisher -> ((ApplicationEventPublisherAware) Objects.requireNonNull(Objects.requireNonNull(this.handler)))
									.setApplicationEventPublisher(publisher));
			configureOutputChannelIfAny();
			Object actualHandler = extractTarget(this.handler);
			if (actualHandler == null) {
				actualHandler = this.handler;
			}
			final Object handlerToConfigure = actualHandler; // must be final for lambdas
			integrationObjectSupport(actualHandler, handlerToConfigure);
			adviceChain(actualHandler);
			JavaUtils.INSTANCE
					.acceptIfCondition(this.async != null && actualHandler instanceof AbstractMessageProducingHandler,
							this.async,
							asyncValue -> ((AbstractMessageProducingHandler) handlerToConfigure).setAsync(asyncValue))
					.acceptIfCondition(this.handler instanceof Orderable && this.order != null,
							this.order, theOrder -> ((Orderable) Objects.requireNonNull(this.handler)).setOrder(theOrder));
			this.initialized = true;
		}
		finally {
			this.initializationMonitor.unlock();
		}
		initializingBean();
		return this.handler;
	}

	private void integrationObjectSupport(Object actualHandler, final Object handlerToConfigure) {
		if (actualHandler instanceof IntegrationObjectSupport) {
			JavaUtils.INSTANCE
					.acceptIfNotNull(this.componentName,
							name -> ((IntegrationObjectSupport) handlerToConfigure).setComponentName(name))
					.acceptIfNotNull(this.channelResolver,
							resolver -> ((IntegrationObjectSupport) handlerToConfigure).setChannelResolver(resolver));
		}
	}

	private void adviceChain(Object actualHandler) {
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			if (actualHandler instanceof AbstractReplyProducingMessageHandler abstractReplyProducingMessageHandler) {
				abstractReplyProducingMessageHandler.setAdviceChain(this.adviceChain);
			}
			else if (this.logger.isDebugEnabled()) {
				String name = this.componentName;
				if (name == null && actualHandler instanceof NamedComponent namedComponent) {
					name = namedComponent.getBeanName();
				}
				this.logger.debug("adviceChain can only be set on an AbstractReplyProducingMessageHandler"
						+ (name == null ? "" : (", " + name)) + ".");
			}
		}
	}

	private void initializingBean() {
		if (this.handler instanceof InitializingBean initializingBean) {
			try {
				initializingBean.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new BeanInitializationException("failed to initialize MessageHandler", e);
			}
		}
	}

	private void configureOutputChannelIfAny() {
		if (this.handler instanceof MessageProducer messageProducer) {
			if (this.outputChannel != null) {
				messageProducer.setOutputChannel(this.outputChannel);
			}
			else if (this.outputChannelName != null) {
				messageProducer.setOutputChannelName(this.outputChannelName);
			}
		}
	}

	protected abstract H createHandler();

	@Override
	public Class<? extends MessageHandler> getObjectType() {
		if (this.handler != null) {
			return this.handler.getClass();
		}
		return getPreCreationHandlerType();
	}

	/**
	 * Subclasses can override this to return a more specific type before handler creation.
	 * After handler creation, the actual type is used.
	 * @return the type.
	 */
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return MessageHandler.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private static Object extractTarget(Object object) {
		if (!(object instanceof Advised)) {
			return object;
		}
		else {
//			return extractTarget(Objects.requireNonNull(AopProxyUtils.getSingletonTarget(object)));
			return Objects.requireNonNullElse(AopProxyUtils.getSingletonTarget(object), object);

		}
	}

}
