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
import org.springframework.integration.message.MessageCreator;

/**
 * An implementation of {@link MessageHandler} that invokes the specified method and target object.
 * It will use the provided implementation of the {@link MessageCreator} strategy interface to convert
 * the method invocation's return value to a reply Message.
 * 
 * @author Mark Fisher
 */
public class DefaultMessageHandlerAdapter extends AbstractMessageHandlerAdapter {

	protected Message<?> handleReturnValue(Object returnValue, Message<?> originalMessage) {
		return this.createReplyMessage(returnValue, originalMessage);
	}

}
