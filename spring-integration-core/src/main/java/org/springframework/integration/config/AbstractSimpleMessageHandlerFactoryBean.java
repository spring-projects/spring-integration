/*
 * Copyright 2002-2016 the original author or authors.
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
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 */
public abstract class AbstractSimpleMessageHandlerFactoryBean<H extends MessageHandler>
		implements FactoryBean<MessageHandler>, ApplicationContextAware, BeanFactoryAware, BeanNameAware,
		ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile H handler;

	private volatile MessageChannel outputChannel;

	private volatile Integer order;

	private BeanFactory beanFactory;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private volatile List<Advice> adviceChain;

	private volatile String componentName;

	private ApplicationContext applicationContext;

	private String beanName;

	private ApplicationEventPublisher applicationEventPublisher;

	private DestinationResolver<MessageChannel> channelResolver;

	private Boolean async;

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
	public H getObject() throws Exception {
		if (this.handler == null) {
			this.handler = this.createHandlerInternal();
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
			if (this.handler instanceof ApplicationContextAware && this.applicationContext != null) {
				((ApplicationContextAware) this.handler).setApplicationContext(this.applicationContext);
			}
			if (this.handler instanceof BeanFactoryAware && getBeanFactory() != null) {
				((BeanFactoryAware) this.handler).setBeanFactory(getBeanFactory());
			}
			if (this.handler instanceof BeanNameAware && this.beanName != null) {
				((BeanNameAware) this.handler).setBeanName(this.beanName);
			}
			if (this.handler instanceof ApplicationEventPublisherAware && this.applicationEventPublisher != null) {
				((ApplicationEventPublisherAware) this.handler)
						.setApplicationEventPublisher(this.applicationEventPublisher);
			}
			if (this.handler instanceof MessageProducer && this.outputChannel != null) {
				((MessageProducer) this.handler).setOutputChannel(this.outputChannel);
			}
			Object actualHandler = extractTarget(this.handler);
			if (actualHandler == null) {
				actualHandler = this.handler;
			}
			if (actualHandler instanceof IntegrationObjectSupport) {
				if (this.componentName != null) {
					((IntegrationObjectSupport) actualHandler).setComponentName(this.componentName);
				}
				if (this.channelResolver != null) {
					((IntegrationObjectSupport) actualHandler).setChannelResolver(this.channelResolver);
				}
			}
			if (!CollectionUtils.isEmpty(this.adviceChain)) {
				if (actualHandler instanceof AbstractReplyProducingMessageHandler) {
					((AbstractReplyProducingMessageHandler) actualHandler).setAdviceChain(this.adviceChain);
				}
				else if (this.logger.isDebugEnabled()) {
					String name = this.componentName;
					if (name == null && actualHandler instanceof NamedComponent) {
						name = ((NamedComponent) actualHandler).getComponentName();
					}
					this.logger.debug("adviceChain can only be set on an AbstractReplyProducingMessageHandler"
						+ (name == null ? "" : (", " + name)) + ".");
				}
			}
			if (this.async != null) {
				if (actualHandler instanceof AbstractReplyProducingMessageHandler) {
					((AbstractReplyProducingMessageHandler) actualHandler)
							.setAsync(this.async);
				}
			}
			if (this.handler instanceof Orderable && this.order != null) {
				((Orderable) this.handler).setOrder(this.order);
			}
			this.initialized = true;
		}
		if (this.handler instanceof InitializingBean) {
			try {
				((InitializingBean) this.handler).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new BeanInitializationException("failed to initialize MessageHandler", e);
			}
		}
		return this.handler;
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

	private Object extractTarget(Object object) {
		if (!(object instanceof Advised)) {
			return object;
		}
		Advised advised = (Advised) object;
		if (advised.getTargetSource() == null) {
			return null;
		}
		try {
			return extractTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			this.logger.error("Could not extract target", e);
			return null;
		}
	}

}
