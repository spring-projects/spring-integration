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

package org.springframework.integration.transformer;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.util.MethodInvoker;
import org.springframework.integration.util.NameResolvingMethodInvoker;

/**
 * @author Mark Fisher
 */
public class MethodInvokingPayloadTransformer implements PayloadTransformer<Object, Object>, InitializingBean {

	private volatile Object object;

	private volatile String methodName;

	private volatile MethodInvoker invoker;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setObject(Object object) {
		this.object = object;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.object == null || this.methodName == null) {
				throw new ConfigurationException("the 'object' and 'methodName' are required");
			}
			this.invoker = new NameResolvingMethodInvoker(this.object, this.methodName);
			this.initialized = true;
		}
	}

	public Object transform(Object payload) throws Exception {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		return this.invoker.invokeMethod(payload); 
	}

}
