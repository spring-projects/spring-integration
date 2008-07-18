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

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.integration.ConfigurationException;

/**
 * A base class for adapters that invoke a specified method and target object.
 * Either a {@link Method} reference or a 'methodName' may be provided, but both
 * are not necessary. In fact, while preference is given to a {@link Method}
 * reference if available, an Exception will be thrown if a non-matching
 * 'methodName' has also been provided. Therefore, to avoid such ambiguity,
 * it is recommended to provide just one or the other.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMethodInvokingAdapter implements MethodInvoker, InitializingBean, Ordered {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile Object object;

	private volatile Method method;

	private volatile String methodName;

	private volatile int order;

	private volatile MethodInvoker invoker;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setObject(Object object) {
		this.object = object;
	}

	protected Object getObject() {
		return this.object;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	protected Method getMethod() {
		return this.method;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	protected String getMethodName() {
		return this.methodName;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	protected boolean isInitialized() {
		return this.initialized;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.object == null) {
				throw new ConfigurationException("The target 'object' must not be null.");
			}
			if (this.method == null && this.methodName == null) {
				throw new ConfigurationException("Either a 'method' or 'methodName' is required.");
			}
			if (this.method != null) {
				if (this.methodName != null && !this.methodName.equals(this.method.getName())) {
					throw new ConfigurationException("An ambiguity exists between the 'method' and 'methodName' properties. " +
							"Note that only one of them is required, but if both are provided they must match.");
				}
				this.methodName = this.method.getName();
				this.invoker = new DefaultMethodInvoker(this.object, this.method);
			}
			else {
				this.invoker = new NameResolvingMethodInvoker(this.object, this.methodName);
			}
			this.initialized = true;
		}
		this.initialize();
	}

	/**
	 * Subclasses may override this method for custom initialization requirements.
	 */
	protected void initialize() {
	}

	public Object invokeMethod(Object ... args) throws Exception {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		return this.invoker.invokeMethod(args);
	}

}
