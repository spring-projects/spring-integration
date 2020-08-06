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

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;

/**
 * MessageGroupProcessor that serves as an adapter for the invocation of a POJO method.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Dave Syer
 * @author Gary Russell
 * @author Artme Bilan
 *
 * @since 2.0
 */
public class MethodInvokingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor
		implements ManageableLifecycle {

	private final MethodInvokingMessageListProcessor<Object> processor;

	/**
	 * Creates a wrapper around the object passed in. This constructor will look for a method that can process
	 * a list of messages.
	 *
	 * @param target the object to wrap
	 */
	public MethodInvokingMessageGroupProcessor(Object target) {
		this.processor = new MethodInvokingMessageListProcessor<Object>(target, Aggregator.class);
	}

	/**
	 * Creates a wrapper around the object passed in. This constructor will look for a named method specifically and
	 * fail when it cannot find a method with the given name.
	 *
	 * @param target the object to wrap
	 * @param methodName the name of the method to invoke
	 */
	public MethodInvokingMessageGroupProcessor(Object target, String methodName) {
		this.processor = new MethodInvokingMessageListProcessor<Object>(target, methodName);
	}

	/**
	 * Creates a wrapper around the object passed in.
	 *
	 * @param target the object to wrap
	 * @param method the method to invoke
	 */
	public MethodInvokingMessageGroupProcessor(Object target, Method method) {
		this.processor = new MethodInvokingMessageListProcessor<Object>(target, method);
	}

	public void setConversionService(ConversionService conversionService) {
		this.processor.setConversionService(conversionService);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.processor.setBeanFactory(beanFactory);
	}

	@Override
	protected final Object aggregatePayloads(MessageGroup group, Map<String, Object> headers) {
		final Collection<Message<?>> messagesUpForProcessing = group.getMessages();
		return this.processor.process(messagesUpForProcessing, headers);
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
