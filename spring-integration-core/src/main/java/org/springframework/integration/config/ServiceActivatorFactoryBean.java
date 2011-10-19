/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating {@link ServiceActivatingHandler} instances.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ServiceActivatorFactoryBean extends AbstractStandardMessageHandlerFactoryBean<ServiceActivatingHandler> {

	private volatile Long sendTimeout;

	private volatile Boolean requiresReply;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setRequiresReply(Boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	@Override
	ServiceActivatingHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		ServiceActivatingHandler handler = (StringUtils.hasText(targetMethodName))
				? new ServiceActivatingHandler(targetObject, targetMethodName)
				: new ServiceActivatingHandler(targetObject);
		return this.configureHandler(handler);
	}

	@Override
	ServiceActivatingHandler createExpressionEvaluatingHandler(Expression expression) {
		ExpressionEvaluatingMessageProcessor<Object> processor = new ExpressionEvaluatingMessageProcessor<Object>(expression);
		processor.setBeanFactory(this.getBeanFactory());
		return this.configureHandler(new ServiceActivatingHandler(processor));
	}

	@Override
	<T> ServiceActivatingHandler createMessageProcessingHandler(MessageProcessor<T> processor) {
		return this.configureHandler(new ServiceActivatingHandler(processor));
	}

	private ServiceActivatingHandler configureHandler(ServiceActivatingHandler handler) {
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		if (this.requiresReply != null) {
			handler.setRequiresReply(this.requiresReply);
		}
		return handler;
	}

}
