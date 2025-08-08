/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class ServiceActivatingHandler extends AbstractReplyProducingMessageHandler implements ManageableLifecycle {

	private final MessageProcessor<?> processor;

	public ServiceActivatingHandler(final Object object) {
		this(new MethodInvokingMessageProcessor<>(object, ServiceActivator.class));
	}

	public ServiceActivatingHandler(Object object, Method method) {
		this(new MethodInvokingMessageProcessor<>(object, method));
	}

	public ServiceActivatingHandler(Object object, String methodName) {
		this(new MethodInvokingMessageProcessor<>(object, methodName));
	}

	public <T> ServiceActivatingHandler(MessageProcessor<T> processor) {
		this.processor = processor;
	}

	@Override
	public String getComponentType() {
		return "service-activator";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return (this.processor instanceof IntegrationPattern)
				? ((IntegrationPattern) this.processor).getIntegrationPatternType()
				: IntegrationPatternType.service_activator;
	}

	@Override
	protected void doInit() {
		setupMessageProcessor(this.processor);
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
	@Nullable
	protected Object handleRequestMessage(Message<?> message) {
		return this.processor.processMessage(message);
	}

	@Override
	public String toString() {
		return "ServiceActivator for [" + this.processor + "]"
				+ (getComponentName() == null ? "" : " (" + getComponentName() + ")");
	}

}
