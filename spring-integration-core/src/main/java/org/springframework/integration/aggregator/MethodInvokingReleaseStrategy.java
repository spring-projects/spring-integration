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
