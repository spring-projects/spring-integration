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

import java.lang.reflect.Method;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link MessageConsumer} that invokes the specified method on the provided object.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingConsumer extends MessageMappingMethodInvoker implements MessageConsumer {

	public MethodInvokingConsumer(Object object, Method method) {
		super(object, method);
		Assert.isTrue(method.getReturnType().equals(void.class),
				"MethodInvokingConsumer requires a void-returning method");
	}

	public MethodInvokingConsumer(Object object, String methodName) {
		super(object, methodName);
	}


	public void onMessage(Message<?> message) {
		Object result = this.invokeMethod(message);
		if (result != null) {
			throw new MessagingException(message, "the MethodInvokingConsumer method must have a void return, "
					+ "but '" + this + "' received a value: [" + result + "]");			
		}
	}

}
