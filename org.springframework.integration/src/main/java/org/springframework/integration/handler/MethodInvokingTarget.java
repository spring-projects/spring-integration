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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Target;

/**
 * A messaging target that invokes the specified method on the provided object.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingTarget extends AbstractMessageHandlerAdapter implements Target {

	public boolean send(Message<?> message) {
		this.handle(message);
		return true;
	}

	@Override
	protected Message<?> handleReturnValue(Object returnValue, Message<?> originalMessage) {
		if (returnValue != null) {
			throw new MessagingException(originalMessage, "The target method returned a non-null Object. " +
					"MethodInvokingTarget should only be used for methods that return no value (preferably void).");
		}
		return null;
	}

}
