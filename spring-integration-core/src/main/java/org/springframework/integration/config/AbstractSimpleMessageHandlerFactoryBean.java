/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractSimpleMessageHandlerFactoryBean<H extends MessageHandler>
		implements FactoryBean<MessageHandler>, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile H handler;

	private volatile MessageChannel outputChannel;

	private volatile Integer order;

	private BeanFactory beanFactory;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private volatile List<Advice> adviceChain;

	private volatile String componentName;

	public AbstractSimpleMessageHandlerFactoryBean() {
		super();
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

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

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
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
			handler = createHandler();
			if (handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) handler).setBeanFactory(getBeanFactory());
			}
			if (this.handler instanceof MessageProducer && this.outputChannel != null) {
				((MessageProducer) this.handler).setOutputChannel(this.outputChannel);
			}
			Object actualHandler = extractTarget(this.handler);
			if (actualHandler == null) {
				actualHandler = this.handler;
			}
			if (actualHandler instanceof IntegrationObjectSupport && this.componentName != null) {
				((IntegrationObjectSupport) actualHandler).setComponentName(this.componentName);
			}
			if (!CollectionUtils.isEmpty(this.adviceChain)) {
				if (actualHandler instanceof AbstractReplyProducingMessageHandler) {
					((AbstractReplyProducingMessageHandler) actualHandler).setAdviceChain(this.adviceChain);
				}
				else if (logger.isDebugEnabled()) {
					String name = this.componentName;
					if (name == null && actualHandler instanceof NamedComponent) {
						name = ((NamedComponent) actualHandler).getComponentName();
					}
					logger.debug("adviceChain can only be set on an AbstractReplyProducingMessageHandler"
						+ (name == null ? "" : (", " + name)) + ".");
				}
			}
			if (this.handler instanceof Orderable && this.order != null) {
				((Orderable) this.handler).setOrder(this.order);
			}
			this.initialized = true;
		}
		if (handler instanceof InitializingBean) {
			try {
				((InitializingBean) handler).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new BeanInitializationException("failed to initialize MessageHandler", e);
			}
		}
		return handler;
	}

	protected abstract H createHandler();

	@Override
	public Class<? extends MessageHandler> getObjectType() {
		if (this.handler != null) {
			return this.handler.getClass();
		}
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
			logger.error("Could not extract target", e);
			return null;
		}
	}

}
