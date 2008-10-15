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

package org.springframework.integration.splitter;

import java.lang.reflect.Method;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.integration.util.DefaultMethodResolver;
import org.springframework.integration.util.MethodResolver;
import org.springframework.util.Assert;

/**
 * A Message Splitter implementation that invokes the specified method
 * on the given object. The method's return value will be split if it
 * is a Collection or Array. If the return value is not a Collection or
 * Array, then the single Object will be returned as the payload of a
 * single reply Message.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingSplitter extends AbstractMessageSplitter implements InitializingBean {

	private final MethodResolver methodResolver = new DefaultMethodResolver(Splitter.class);

	private final MessageMappingMethodInvoker invoker;


	public MethodInvokingSplitter(Object object, Method method) {
		this.invoker = new MessageMappingMethodInvoker(object, method);
	}

	public MethodInvokingSplitter(Object object, String methodName) {
		this.invoker = new MessageMappingMethodInvoker(object, methodName);
	}

	public MethodInvokingSplitter(Object object) {
		Assert.notNull(object, "object must not be null");
		Method method = this.methodResolver.findMethod(object); 
		Assert.notNull(method, "unable to resolve Splitter method on target class ["
				+ object.getClass() + "]");
		this.invoker = new MessageMappingMethodInvoker(object, method);
	}


	public void afterPropertiesSet() throws Exception {
		this.invoker.afterPropertiesSet();
	}

	@Override
	protected Object splitMessage(Message<?> message) {
		return this.invoker.invokeMethod(message);
	}

}
