/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.expression.BeanResolver;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.ExpressionCommandMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a SpEL expression.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class ExpressionControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean {

	private volatile Long sendTimeout;

	private volatile BeanResolver beanResolver;

	private final ExpressionCommandMessageProcessor processor = new ExpressionCommandMessageProcessor();


	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	protected MessageHandler createHandler() {
		this.processor.setBeanFactory(this.getBeanFactory());
		if (this.beanResolver != null) {
			this.processor.setBeanResolver(this.beanResolver);
		}
		ServiceActivatingHandler handler = new ServiceActivatingHandler(this.processor);
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}

}
