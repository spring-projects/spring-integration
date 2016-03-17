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

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class ServiceActivatingHandler extends AbstractReplyProducingMessageHandler implements Lifecycle {

	private final MessageProcessor<?> processor;


	public ServiceActivatingHandler(final Object object) {
		this(new MethodInvokingMessageProcessor<Object>(object, ServiceActivator.class));
	}

	public ServiceActivatingHandler(Object object, Method method) {
		this(new MethodInvokingMessageProcessor<Object>(object, method));
	}

	public ServiceActivatingHandler(Object object, String methodName) {
		this(new MethodInvokingMessageProcessor<Object>(object, methodName));
	}

	public <T> ServiceActivatingHandler(MessageProcessor<T> processor) {
		this.processor = processor;
	}


	@Override
	public String getComponentType() {
		return "service-activator";
	}

	@Override
	protected void doInit() {
		if (this.processor instanceof AbstractMessageProcessor) {
			((AbstractMessageProcessor<?>) this.processor).setConversionService(this.getConversionService());
		}
		if (this.processor instanceof BeanFactoryAware && this.getBeanFactory() != null) {
			((BeanFactoryAware) this.processor).setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	public void start() {
		if (this.processor instanceof Lifecycle) {
			((Lifecycle) this.processor).start();
		}
	}

	@Override
	public void stop() {
		if (this.processor instanceof Lifecycle) {
			((Lifecycle) this.processor).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.processor instanceof Lifecycle) || ((Lifecycle) this.processor).isRunning();
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		return this.processor.processMessage(message);
	}

	@Override
	public String toString() {
		return "ServiceActivator for [" + this.processor + "]"
				+ (this.getComponentName() == null ? "" : " (" + this.getComponentName() + ")");
	}

}
