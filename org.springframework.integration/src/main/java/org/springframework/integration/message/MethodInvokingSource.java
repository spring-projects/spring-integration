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

package org.springframework.integration.message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.Message;
import org.springframework.integration.util.DefaultMethodInvoker;
import org.springframework.integration.util.MethodInvoker;
import org.springframework.integration.util.MethodValidator;
import org.springframework.integration.util.NameResolvingMethodInvoker;
import org.springframework.util.Assert;

/**
 * A pollable source that invokes a no-argument method so that its return value
 * may be sent to a channel.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingSource implements MessageSource<Object>, InitializingBean {

	private volatile Object object;

	private volatile Method method;

	private volatile String methodName;

	private volatile MethodInvoker invoker;


	public void setObject(Object object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(Method method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
		this.methodName = method.getName();
	}

	public void setMethodName(String methodName) {
		Assert.notNull(methodName, "'methodName' must not be null");
		this.methodName = methodName;
	}

	public void afterPropertiesSet() {
		if (this.method != null) {
			this.invoker = new DefaultMethodInvoker(this.object, this.method);
		}
		else if (this.methodName != null) {
			NameResolvingMethodInvoker nrmi = new NameResolvingMethodInvoker(this.object, this.methodName);
			nrmi.setMethodValidator(new MessageReceivingMethodValidator());
			this.invoker = nrmi;
		}
		else {
			throw new IllegalArgumentException("either 'method' or 'methodName' is required");
		}
	}

	public Message<Object> receive() {
		if (this.invoker == null) {
			this.afterPropertiesSet();
		}
		try {
			Object result = this.invoker.invokeMethod(new Object[] {});
			if (result == null) {
				return null;
			}
			if (result instanceof Message) {
				return (Message) result;
			}
			return new GenericMessage<Object>(result);
		}
		catch (InvocationTargetException e) {
			throw new MessagingException(
					"Source method '" + this.methodName + "' threw an Exception.", e.getTargetException());
		}
		catch (Throwable e) {
			throw new MessagingException("Failed to invoke source method '" + this.methodName + "'.");
		}
	}


	private static class MessageReceivingMethodValidator implements MethodValidator {

		public void validate(Method method) {
			Assert.isTrue(!method.getReturnType().equals(void.class),
					"MethodInvokingSource requires a non-void returning method.");
		}
	}

}
