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
import org.springframework.integration.handler.ExpressionPayloadMessageProcessor;
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

	private final ExpressionPayloadMessageProcessor processor;


	public ExpressionControlBusFactoryBean(ExpressionPayloadMessageProcessor processor) {
		this.processor = processor;	
	}


	public void setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	protected MessageHandler createHandler() {
		if (beanResolver != null) {
			processor.setBeanResolver(beanResolver);
		}
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}

}
