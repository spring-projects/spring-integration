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

package org.springframework.integration.handler;

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.util.MethodValidator;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;

/**
 * A simple wrapper for {@link MethodInvoker}.
 * 
 * @author Mark Fisher
 */
public class HandlerMethodInvoker<T> {

	private T object;

	private String method;

	private MethodValidator methodValidator;


	public HandlerMethodInvoker(T object, String method) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(method, "'method' must not be null");
		this.object = object;
		this.method = method;
	}

	public void setMethodValidator(MethodValidator methodValidator) {
		this.methodValidator = methodValidator;
	}

	public Object invokeMethod(Object ... args) {
		try {
			MethodInvoker methodInvoker = new ArgumentConvertingMethodInvoker();
			methodInvoker.setTargetObject(this.object);
			methodInvoker.setTargetMethod(this.method);
			methodInvoker.setArguments(args);
			methodInvoker.prepare();
			methodInvoker.getPreparedMethod().setAccessible(true);
			if (this.methodValidator != null) {
				this.methodValidator.validate(methodInvoker.getPreparedMethod());
			}
			return methodInvoker.invoke();
		}
		catch (InvocationTargetException e) {
			throw new MessagingException(
					"Method '" + this.method + "' threw an Exception.", e.getTargetException());
		}
		catch (Throwable e) {
			throw new MessagingException("Failed to invoke method '" + this.method +
					"' with arguments: " + ObjectUtils.nullSafeToString(args), e);
		}
	}

}
