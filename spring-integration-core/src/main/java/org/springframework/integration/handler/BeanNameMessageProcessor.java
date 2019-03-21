/*
 * Copyright 2016-2019 the original author or authors.
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
