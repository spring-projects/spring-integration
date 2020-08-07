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
