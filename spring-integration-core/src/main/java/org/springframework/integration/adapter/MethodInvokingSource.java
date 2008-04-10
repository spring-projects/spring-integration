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

package org.springframework.integration.adapter;

import java.lang.reflect.Method;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.HandlerMethodInvoker;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.util.MethodValidator;
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

	private HandlerMethodInvoker<T> invoker;


	public void setObject(T object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(String method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
	}

	public void afterPropertiesSet() {
		this.invoker = new HandlerMethodInvoker<T>(this.object, this.method);
		this.invoker.setMethodValidator(new MessageReceivingMethodValidator());
	}

	public Message<Object> poll() {
		if (this.invoker == null) {
			this.afterPropertiesSet();
		}
		return new GenericMessage<Object>(this.invoker.invokeMethod(new Object[] {}));
	}


	private static class MessageReceivingMethodValidator implements MethodValidator {

		public void validate(Method method) {
			if (method.getReturnType().equals(void.class)) {
				throw new ConfigurationException("MethodInvokingSource requires a non-void returning method.");
			}
		}
	}

}
