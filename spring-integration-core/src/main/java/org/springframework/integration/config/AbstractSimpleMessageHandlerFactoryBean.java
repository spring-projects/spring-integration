/*
 * Copyright 2002-2020 the original author or authors.
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

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.util.JavaUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Factory bean to create and configure a {@link MessageHandler}.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 */
public abstract class AbstractSimpleMessageHandlerFactoryBean<H extends MessageHandler>
		implements FactoryBean<MessageHandler>, ApplicationContextAware, BeanFactoryAware, BeanNameAware,
		ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(getClass()); //NOSONAR protected with final

	private final Object initializationMonitor = new Object();

	private BeanFactory beanFactory;

	private H handler;

	private MessageChannel outputChannel;

	private String outputChannelName;

	private Integer order;

	private List<Advice> adviceChain;

	private String componentName;

	private ApplicationContext applicationContext;

	private String beanName;

	private ApplicationEventPublisher applicationEventPublisher;

	private DestinationResolver<MessageChannel> channelResolver;

	private Boolean async;

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
			Assert.notNull(this.handler, "failed to create MessageHandler");
		}
		return this.handler;
	}

	protected final H createHandlerInternal() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				// There was a problem when this method was called already
				return null;
			}
			this.handler = createHandler();
			JavaUtils.INSTANCE
				.acceptIfCondition(this.handler instanceof ApplicationContextAware && this.applicationContext != null,
					this.applicationContext,
					context -> ((ApplicationContextAware) this.handler).setApplicationContext(this.applicationContext))
				.acceptIfCondition(this.handler instanceof BeanFactoryAware && getBeanFactory() != null,
					getBeanFactory(),
					factory -> ((BeanFactoryAware) this.handler).setBeanFactory(factory))
				.acceptIfCondition(this.handler instanceof BeanNameAware && this.beanName != null, this.beanName,
					name -> ((BeanNameAware) this.handler).setBeanName(this.beanName))
				.acceptIfCondition(this.handler instanceof ApplicationEventPublisherAware
										&& this.applicationEventPublisher != null,
					this.applicationEventPublisher,
					publisher -> ((ApplicationEventPublisherAware) this.handler)
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
					this.order, theOrder -> ((Orderable) this.handler).setOrder(theOrder));
			this.initialized = true;
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
			if (actualHandler instanceof AbstractReplyProducingMessageHandler) {
				((AbstractReplyProducingMessageHandler) actualHandler).setAdviceChain(this.adviceChain);
			}
			else if (this.logger.isDebugEnabled()) {
				String name = this.componentName;
				if (name == null && actualHandler instanceof NamedComponent) {
					name = ((NamedComponent) actualHandler).getBeanName();
				}
				this.logger.debug("adviceChain can only be set on an AbstractReplyProducingMessageHandler"
						+ (name == null ? "" : (", " + name)) + ".");
			}
		}
	}

	private void initializingBean() {
		if (this.handler instanceof InitializingBean) {
			try {
				((InitializingBean) this.handler).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new BeanInitializationException("failed to initialize MessageHandler", e);
			}
		}
	}

	private void configureOutputChannelIfAny() {
		if (this.handler instanceof MessageProducer) {
			MessageProducer messageProducer = (MessageProducer) this.handler;
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
			return extractTarget(AopProxyUtils.getSingletonTarget(object));
		}
	}

}
