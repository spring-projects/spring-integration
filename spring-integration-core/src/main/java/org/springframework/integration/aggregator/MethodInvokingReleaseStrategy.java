/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link ReleaseStrategy} that invokes a method on a plain old Java object. 
 * 
 * @author Marius Bogoevici
 * @author Dave Syer
 */
public class MethodInvokingReleaseStrategy implements ReleaseStrategy {

	private final MethodInvokingMessageListProcessor adapter;

	public MethodInvokingReleaseStrategy(Object object, Method method) {
		adapter = new MethodInvokingMessageListProcessor(object, method, Boolean.class);
	}

	public MethodInvokingReleaseStrategy(Object object, String methodName) {
		adapter = new MethodInvokingMessageListProcessor(object, methodName, Boolean.class);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		adapter.setBeanFactory(beanFactory);
	}

	public void setConversionService(ConversionService conversionService) {
		adapter.setConversionService(conversionService);
	}

	public boolean canRelease(MessageGroup messages) {
		return (Boolean) adapter.process(messages.getUnmarked(), null);
	}

}
