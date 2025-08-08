/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
