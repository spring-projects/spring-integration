/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.context.Lifecycle;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;

/**
 * The standard <a href="https://www.enterpriseintegrationpatterns.com/patterns/messaging/MessagingAdapter.html">Service Activator pattern</a> implementation.
 * An extension of {@link AbstractReplyProducingMessageHandler}.
 *
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
		return (this.processor instanceof IntegrationPattern integrationPattern)
				? integrationPattern.getIntegrationPatternType()
				: IntegrationPatternType.service_activator;
	}

	@Override
	protected void doInit() {
		setupMessageProcessor(this.processor);
	}

	@Override
	public void start() {
		if (this.processor instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	public void stop() {
		if (this.processor instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.processor instanceof Lifecycle lifecycle) || lifecycle.isRunning();
	}

	@Override
	@Nullable
	protected Object handleRequestMessage(Message<?> message) {
		return this.processor.processMessage(message);
	}

	@Override
	public String toString() {
		return "ServiceActivator for [" + this.processor + "]"
				+ " (" + getComponentName() + ")";
	}

}
