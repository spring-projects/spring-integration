/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.endpoint.MethodValidator;
import org.springframework.integration.endpoint.SimpleMethodInvoker;
import org.springframework.util.Assert;

/**
 * A pollable source that invokes a no-argument method so that its return value
 * may be sent to a channel.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingSource<T> implements PollableSource<Object>, InitializingBean {

	private T object;

	private String method;

	private SimpleMethodInvoker<T> invoker;


	public void setObject(T object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(String method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
	}

	public void afterPropertiesSet() {
		this.invoker = new SimpleMethodInvoker<T>(this.object, this.method);
		this.invoker.setMethodValidator(new MessageReceivingMethodValidator());
	}

	public Collection<Object> poll(int limit) {
		if (this.invoker == null) {
			this.afterPropertiesSet();
		}
		return Arrays.asList(this.invoker.invokeMethod(new Object[] {}));
	}


	private static class MessageReceivingMethodValidator implements MethodValidator {

		public void validate(Method method) {
			if (method.getReturnType().equals(void.class)) {
				throw new MessagingConfigurationException("MethodInvokingSource requires a non-void returning method.");
			}
		}
	}

}
