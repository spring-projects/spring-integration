/*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;

/**
 * Base class for {@link MessageHandler} advice classes.
 * @author Gary Russell
 * @since 2.2
 *
 */
public abstract class AbstractRequestHandlerAdvice implements MethodInterceptor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	public final Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		boolean isMessageMethod = (method.getName().equals("handleRequestMessage") || method.getName().equals("handleMessage"))
				&& (invocation.getArguments().length == 1 && invocation.getArguments()[0] instanceof Message);

		if (!isMessageMethod) {
			return invocation.proceed();
		}

		return doInvoke(invocation);
	}

	protected abstract Object doInvoke(MethodInvocation invocation) throws Throwable;
}