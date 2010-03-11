/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for FactoryBeans that create MessageHandler instances.
 * 
 * @author Mark Fisher
 * @author Alexander Peters
 */
abstract class AbstractMessageHandlerFactoryBean implements FactoryBean<MessageHandler>, BeanFactoryAware {

	private volatile MessageHandler handler;

	private volatile Object targetObject;

	private volatile String targetMethodName;

	private volatile String expression;

	private volatile MessageChannel outputChannel;

	private volatile Integer order;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private BeanFactory beanFactory;


	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	public void setTargetMethodName(String targetMethodName) {
		this.targetMethodName = targetMethodName;
	}

	public void setExpression(String expression) {
		this.expression = expression;
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

	public MessageHandler getObject() throws Exception {
		if (this.handler == null) {
			this.initializeHandler();
			Assert.notNull(this.handler, "failed to create MessageHandler");
			if (this.handler instanceof AbstractReplyProducingMessageHandler && this.outputChannel != null) {
				((AbstractReplyProducingMessageHandler) this.handler).setOutputChannel(this.outputChannel);
			}
			if (this.handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.handler).setBeanFactory(beanFactory);
			}
			if (this.handler instanceof AbstractMessageHandler && this.order != null) {
				((AbstractMessageHandler) this.handler).setOrder(this.order.intValue());
			}
		}
		return this.handler;
	}

	public Class<? extends MessageHandler> getObjectType() {
		if (this.handler != null) {
			return this.handler.getClass();
		}
		return MessageHandler.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initializeHandler() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.targetObject == null) {
				Assert.isTrue(!StringUtils.hasText(this.targetMethodName),
						"The target method is only allowed when a target object (ref or inner bean) is also provided.");
			}
			if (this.targetObject != null) {
				Assert.state(this.expression == null,
						"The 'targetObject' and 'expression' properties are mutually exclusive.");
				this.handler = this.createMethodInvokingHandler(this.targetObject, this.targetMethodName);
			}
			else if (this.expression != null) {
				this.handler = this.createExpressionEvaluatingHandler(this.expression);
			}
			else {
				this.handler = this.createDefaultHandler();
			}
			if (this.handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.handler).setBeanFactory(beanFactory);
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
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 */
	abstract MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName);

	MessageHandler createExpressionEvaluatingHandler(String expression) {
		throw new UnsupportedOperationException(this.getClass().getName() + " does not support expressions.");
	}

	MessageHandler createDefaultHandler() {
		throw new IllegalArgumentException(
			"Exactly one of the 'targetObject' or 'expression' property is required.");
	}

}
