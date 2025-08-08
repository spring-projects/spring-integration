/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.management.ManageableLifecycle;

/**
 * A {@link ReleaseStrategy} that invokes a method on a plain old Java object.
 *
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Artme Bilan
 */
public class MethodInvokingReleaseStrategy implements ReleaseStrategy, BeanFactoryAware, ManageableLifecycle {

	private final MethodInvokingMessageListProcessor<Boolean> adapter;

	public MethodInvokingReleaseStrategy(Object object, Method method) {
		this.adapter = new MethodInvokingMessageListProcessor<Boolean>(object, method, Boolean.class);
	}

	public MethodInvokingReleaseStrategy(Object object, String methodName) {
		this.adapter = new MethodInvokingMessageListProcessor<Boolean>(object, methodName, Boolean.class);
	}

	public void setConversionService(ConversionService conversionService) {
		this.adapter.setConversionService(conversionService);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.adapter.setBeanFactory(beanFactory);
	}

	@Override
	public boolean canRelease(MessageGroup messages) {
		return this.adapter.process(messages.getMessages(), null);
	}

	@Override
	public void start() {
		this.adapter.start();
	}

	@Override
	public void stop() {
		this.adapter.stop();
	}

	@Override
	public boolean isRunning() {
		return this.adapter.isRunning();
	}

}
