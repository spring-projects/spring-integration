/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessageProducer;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 */
public abstract class AbstractSimpleMessageHandlerFactoryBean<H extends MessageHandler> implements FactoryBean<MessageHandler>, BeanFactoryAware {

	private volatile H handler;

	private volatile MessageChannel outputChannel;

	private volatile Integer order;

	private BeanFactory beanFactory;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractSimpleMessageHandlerFactoryBean() {
		super();
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	public H getObject() throws Exception {
		if (this.handler == null) {
			this.handler = this.createHandlerInternal();
			Assert.notNull(this.handler, "failed to create MessageHandler");
			if (this.handler instanceof MessageProducer && this.outputChannel != null) {
				((MessageProducer) this.handler).setOutputChannel(this.outputChannel);
			}
			if (this.handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.handler).setBeanFactory(beanFactory);
			}
			if (this.handler instanceof Orderable && this.order != null) {
				((Orderable) this.handler).setOrder(this.order.intValue());
			}
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

	public Class<? extends MessageHandler> getObjectType() {
		if (this.handler != null) {
			return this.handler.getClass();
		}
		return MessageHandler.class;
	}

	public boolean isSingleton() {
		return true;
	}

}