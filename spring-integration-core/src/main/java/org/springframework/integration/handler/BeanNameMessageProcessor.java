/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * An "artificial" {@link MessageProcessor} for lazy-load of target bean by its name.
 * For internal use only.
 *
 * @param <T> the expected {@link #processMessage} result type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class BeanNameMessageProcessor<T> implements MessageProcessor<T>, BeanFactoryAware {

	private final String beanName;

	private final String methodName;

	private MessageProcessor<T> delegate;

	private BeanFactory beanFactory;

	public BeanNameMessageProcessor(String object, String methodName) {
		this.beanName = object;
		this.methodName = methodName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	@Nullable
	public T processMessage(Message<?> message) {
		if (this.delegate == null) {
			Object target = this.beanFactory.getBean(this.beanName);
			MethodInvokingMessageProcessor<T> methodInvokingMessageProcessor =
					new MethodInvokingMessageProcessor<>(target, this.methodName);
			methodInvokingMessageProcessor.setBeanFactory(this.beanFactory);
			this.delegate = methodInvokingMessageProcessor;
		}
		return this.delegate.processMessage(message);
	}

}
