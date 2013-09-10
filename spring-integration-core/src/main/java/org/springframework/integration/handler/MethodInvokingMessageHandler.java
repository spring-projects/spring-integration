/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} that invokes the specified method on the provided object.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MethodInvokingMessageHandler extends AbstractMessageHandler {

	private volatile MethodInvokingMessageProcessor<Object> processor;
	
	private volatile String componentType;

	public MethodInvokingMessageHandler(Object object, Method method) {
		Assert.isTrue(method.getReturnType().equals(void.class),
				"MethodInvokingMessageHandler requires a void-returning method");
		processor = new MethodInvokingMessageProcessor<Object>(object, method);
	}

	public MethodInvokingMessageHandler(Object object, String methodName) {
		processor = new MethodInvokingMessageProcessor<Object>(object, methodName);
	}
	
	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	@Override
	public String getComponentType() {
		return this.componentType;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object result = processor.processMessage(message);
		if (result != null) {
			throw new MessagingException(message, "the MethodInvokingMessageHandler method must "
					+ "have a void return, but '" + this + "' received a value: [" + result + "]");			
		}
	}

}
