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

import java.io.Serializable;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.remoting.RemoteAccessException;

/**
 * A base class for remoting {@link MessageHandler} adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRemotingHandler implements MessageHandler {

	private final MessageHandler handlerProxy;


	public AbstractRemotingHandler(String url) {
		this.handlerProxy = this.createHandlerProxy(url);
	}


	/**
	 * Subclasses must implement this method. It will be invoked from the constructor.
	 */
	protected abstract MessageHandler createHandlerProxy(String url);


	public final Message<?> handle(Message<?> message) {
		this.verifySerializability(message);
		try {
			return this.handlerProxy.handle(message);
		}
		catch (RemoteAccessException e) {
			throw new MessageHandlingException(message, "unable to handle message remotely", e);
		}
	}

	private void verifySerializability(Message<?> message) {
		if (!(message.getPayload() instanceof Serializable)) {
			throw new MessageHandlingException(message,
					this.getClass().getName() + " expects a Serializable payload type " +
					"but encountered '" + message.getPayload().getClass().getName() + "'");
		}
		for (String headerName : message.getHeaders().keySet()) {
			Object attribute = message.getHeaders().get(headerName);
			if (!(attribute instanceof Serializable)) {
				throw new MessageHandlingException(message,
						this.getClass().getName() + " expects Serializable attribute types " +
						"but encountered '" + attribute.getClass().getName() + "' for the attribute '" +
						headerName + "'");				
			}
		}
	}

}
