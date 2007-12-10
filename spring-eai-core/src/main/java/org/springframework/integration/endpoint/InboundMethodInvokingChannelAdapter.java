/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.util.Assert;

/**
 * An inbound channel adapter for invoking a no-argument method and receiving
 * its return value.
 * 
 * @author Mark Fisher
 */
public class InboundMethodInvokingChannelAdapter<T> extends AbstractInboundChannelAdapter {

	private T object;

	private String method;

	private SimpleMethodInvoker<T> invoker;


	public void setObject(T object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(String method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
	}

	@Override
	public void initialize() {
		this.invoker = new SimpleMethodInvoker<T>(this.object, this.method);
		this.invoker.setMethodValidator(new MessageReceivingMethodValidator());
	}

	@Override
	protected Object doReceiveObject() {
		return this.invoker.invokeMethod(new Object[] {});
	}


	public static class MessageReceivingMethodValidator implements MethodValidator {

		public void validate(Method method) {
			if (method.getReturnType().equals(void.class)) {
				throw new MessagingConfigurationException(
						"Inbound channel adapter requires a non-void returning method.");
			}
		}
	}

}
