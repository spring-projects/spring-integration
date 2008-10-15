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

package org.springframework.integration.filter;

import java.lang.reflect.Method;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * A method-invoking implementation of {@link MessageSelector}.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingSelector implements MessageSelector {

	private final MessageMappingMethodInvoker invoker;


	public MethodInvokingSelector(Object object, Method method) {
		this.invoker = new MessageMappingMethodInvoker(object, method);
		Class<?> returnType = method.getReturnType();
		Assert.isTrue(boolean.class.isAssignableFrom(returnType)
				|| Boolean.class.isAssignableFrom(returnType),
				"MethodInvokingSelector method must return a boolean result.");
	}

	public MethodInvokingSelector(Object object, String methodName) {
		this.invoker = new MessageMappingMethodInvoker(object, methodName);
	}


	public boolean accept(Message<?> message) {
		Object result = this.invoker.invokeMethod(message);
		Assert.notNull(result, "result must not be null");
		Assert.isAssignable(Boolean.class, result.getClass(), "a boolean result is required");
		return (Boolean) result;
	}

}
