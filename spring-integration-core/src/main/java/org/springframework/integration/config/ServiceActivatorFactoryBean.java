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

import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating {@link ServiceActivatingHandler} instances.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ServiceActivatorFactoryBean extends AbstractMessageHandlerFactoryBean {

	private volatile Long sendTimeout;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		ServiceActivatingHandler handler = (StringUtils.hasText(targetMethodName))
				? new ServiceActivatingHandler(targetObject, targetMethodName)
				: new ServiceActivatingHandler(targetObject);
		return this.configureHandler(handler);
	}

	@Override
	MessageHandler createExpressionEvaluatingHandler(String expression) {
		Class<?> expectedType = null;
		return this.configureHandler(new ServiceActivatingHandler(expression, expectedType));
	}

	private ServiceActivatingHandler configureHandler(ServiceActivatingHandler handler) {
		if (this.sendTimeout != null) {
			handler.setSendTimeout(sendTimeout);
		}
		return handler;
	}

}
