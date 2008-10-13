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

package org.springframework.integration.endpoint;

import java.lang.reflect.Method;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.integration.util.DefaultMethodResolver;
import org.springframework.integration.util.MethodInvoker;
import org.springframework.integration.util.MethodResolver;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class ServiceActivatorEndpoint extends AbstractReplyProducingMessageConsumer implements InitializingBean {

	private final MethodResolver methodResolver = new DefaultMethodResolver(ServiceActivator.class);

	private final MethodInvoker invoker;


	public ServiceActivatorEndpoint(MethodInvoker invoker) {
		Assert.notNull(invoker, "invoker must not be null");
		this.invoker = invoker;
	}

	public ServiceActivatorEndpoint(final Object object) {
		Assert.notNull(object, "object must not be null");
		Method method = this.methodResolver.findMethod(object.getClass()); 
		Assert.notNull(method, "unable to resolve ServiceActivator method on target class ["
				+ object.getClass() + "]");
		this.invoker = new MessageMappingMethodInvoker(object, method);
	}

	public ServiceActivatorEndpoint(Object object, String methodName) {
		this(new MessageMappingMethodInvoker(object, methodName));
	}


	public void afterPropertiesSet() throws Exception {
		if (this.invoker instanceof InitializingBean) {
			((InitializingBean) this.invoker).afterPropertiesSet();
		}
	}

	@Override
	protected void onMessage(Message<?> message, ReplyMessageHolder replyHolder) {
		try {
			Object result = this.invoker.invokeMethod(message);
			if (result != null) {
				replyHolder.set(result);
			}
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new MessageHandlingException(message, "failure occurred in endpoint '" + this + "'", e);
		}
	}

}
