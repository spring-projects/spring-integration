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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.handler.HandlerMethodInvoker;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} that invokes the specified method on the provided object.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingHandler implements MessageHandler, InitializingBean {

	private volatile Object object;

	private volatile String methodName;

	private volatile MessageMapper messageMapper;

	private volatile MessageCreator messageCreator;

	protected HandlerMethodInvoker<?> invoker;


	public void setObject(Object object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethodName(String methodName) {
		Assert.notNull(methodName, "'methodName' must not be null");
		this.methodName = methodName;
	}

	public void setMessageMapper(MessageMapper messageMapper) {
		this.messageMapper = messageMapper;
	}

	public void setMessageCreator(MessageCreator messageCreator) {
		this.messageCreator = messageCreator;
	}

	public void afterPropertiesSet() {
		this.invoker = new HandlerMethodInvoker(this.object, this.methodName);
	}

	public Message<?> handle(Message<?> message) {
		Object args = (this.messageMapper != null) ? this.messageMapper.mapMessage(message) : message.getPayload();
		Object result = this.invoker.invokeMethod(args);
		if (result == null) {
			return null;
		}
		return (this.messageCreator != null) ? this.messageCreator.createMessage(result) : new GenericMessage<Object>(result);
	}

}
