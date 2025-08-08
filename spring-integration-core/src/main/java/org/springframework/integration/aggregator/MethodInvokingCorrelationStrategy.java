/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link CorrelationStrategy} implementation that works as an adapter to another bean.
 *
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
public class MethodInvokingCorrelationStrategy implements CorrelationStrategy, BeanFactoryAware, ManageableLifecycle {

	private final MethodInvokingMessageProcessor<?> processor;

	public MethodInvokingCorrelationStrategy(Object object, String methodName) {
		this.processor = new MethodInvokingMessageProcessor<Object>(object, methodName);
	}

	public MethodInvokingCorrelationStrategy(Object object, Method method) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(method, "'method' must not be null");
		Assert.isTrue(!Void.TYPE.equals(method.getReturnType()), "Method return type must not be void");
		this.processor = new MethodInvokingMessageProcessor<Object>(object, method);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory != null) {
			this.processor.setBeanFactory(beanFactory);
		}
	}

	@Override
	public Object getCorrelationKey(Message<?> message) {
		return this.processor.processMessage(message);
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

}
