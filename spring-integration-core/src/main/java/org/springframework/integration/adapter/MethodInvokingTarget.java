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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.handler.HandlerMethodInvoker;
import org.springframework.util.Assert;

/**
 * A messaging target that invokes the specified method on the provided object.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingTarget<T> implements Target<Object>, InitializingBean {

	private Log logger = LogFactory.getLog(this.getClass());

	private T object;

	private String method;

	private HandlerMethodInvoker<T> invoker;

	private ArgumentListPreparer argumentListPreparer;


	public void setObject(T object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(String method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
	}

	public void setArgumentListPreparer(ArgumentListPreparer argumentListPreparer) {
		this.argumentListPreparer = argumentListPreparer;
	}

	public void afterPropertiesSet() {
		this.invoker = new HandlerMethodInvoker<T>(this.object, this.method);
	}

	public boolean send(Object object) {
		Object args[] = null;
		if (this.argumentListPreparer != null) {
			args = this.argumentListPreparer.prepare(object);
		}
		else {
			args = new Object[] { object };
		}
		Object result = this.invoker.invokeMethod(args);
		if (result != null && logger.isWarnEnabled()) {
			logger.warn("ignoring outbound channel adapter's return value");
		}
		return true;
	}

}
