/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} that invokes the specified
 * method on the provided object.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class MethodInvokingMessageHandler extends AbstractMessageHandler implements ManageableLifecycle {

	private final MethodInvokingMessageProcessor<Object> processor;

	private String componentType;

	public MethodInvokingMessageHandler(Object object, Method method) {
		Assert.isTrue(method.getReturnType().equals(void.class),
				"MethodInvokingMessageHandler requires a void-returning method");
		this.processor = new MethodInvokingMessageProcessor<>(object, method);
	}

	public MethodInvokingMessageHandler(Object object, String methodName) {
		this.processor = new MethodInvokingMessageProcessor<>(object, methodName);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.processor.setBeanFactory(beanFactory);
	}

	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	@Override
	public String getComponentType() {
		return this.componentType;
	}

	@Override
	public void start() {
		this.processor.start();
	}

	@Override
	public void stop() {
		this.processor.stop();
	}

	@Override
	public boolean isRunning() {
		return this.processor.isRunning();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object result = this.processor.processMessage(message);
		if (result != null) {
			throw new MessagingException(message, "the MethodInvokingMessageHandler method must "
					+ "have a void return, but '" + this + "' received a value: [" + result + "]");
		}
	}

}
