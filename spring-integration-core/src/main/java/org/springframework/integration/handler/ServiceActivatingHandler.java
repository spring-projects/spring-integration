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

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * @author Mark Fisher
 */
public class ServiceActivatingHandler extends AbstractReplyProducingMessageHandler {

	private final AbstractMessageProcessor processor;


	public ServiceActivatingHandler(final Object object) {
		this.processor = new MethodInvokingMessageProcessor(object, ServiceActivator.class);
	}

	public ServiceActivatingHandler(Object object, Method method) {
		this.processor = new MethodInvokingMessageProcessor(object, method);
	}

	public ServiceActivatingHandler(Object object, String methodName) {
		this.processor = new MethodInvokingMessageProcessor(object, methodName);
	}

	public ServiceActivatingHandler(String expression, Class<?> expectedType) {
		ExpressionEvaluatingMessageProcessor eemp = new ExpressionEvaluatingMessageProcessor(expression);
		eemp.setExpectedType(expectedType);
		this.processor = eemp;
	}


	@Override
	public String getComponentType() {
		return "service-activator";
	}

	@Override
	public final void onInit() {
		this.processor.setConversionService(this.getConversionService());
		if (this.processor instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.processor).setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		try {
			return this.processor.processMessage(message);
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new MessageHandlingException(message, "failure occurred in Service Activator '" + this + "'", e);
		}
	}

	public String toString() {
		return "ServiceActivator for [" + this.processor + "]";
	}

}
