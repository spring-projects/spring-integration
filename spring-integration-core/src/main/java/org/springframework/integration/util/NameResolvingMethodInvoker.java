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

package org.springframework.integration.util;

import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.Assert;

/**
 * Implementation of {@link MethodInvoker} to be used when only the method name is known.
 * 
 * @author Mark Fisher
 */
public class NameResolvingMethodInvoker implements MethodInvoker {

	private final Object object;

	private final String methodName;

	private volatile MethodValidator methodValidator;


	public NameResolvingMethodInvoker(Object object, String methodName) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(methodName, "'methodName' must not be null");
		this.object = object;
		this.methodName = methodName;
	}


	public void setMethodValidator(MethodValidator methodValidator) {
		this.methodValidator = methodValidator;
	}

	public Object invokeMethod(Object ... args) throws Exception {
		ArgumentConvertingMethodInvoker invoker = new ArgumentConvertingMethodInvoker();
		invoker.setTargetObject(this.object);
		invoker.setTargetMethod(this.methodName);
		invoker.setArguments(args);
		invoker.prepare();
		invoker.getPreparedMethod().setAccessible(true);
		if (this.methodValidator != null) {
			this.methodValidator.validate(invoker.getPreparedMethod());
		}
		return invoker.invoke();
	}

}
