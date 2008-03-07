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

package org.springframework.integration.adapter.rmi;

import java.io.Serializable;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/**
 * A target channel adapter for RMI-based remoting.
 * 
 * @author Mark Fisher
 */
public class RmiTargetAdapter implements MessageHandler {

	private final MessageHandler handlerProxy;


	public RmiTargetAdapter(String url) {
		RmiProxyFactoryBean proxyFactory = new RmiProxyFactoryBean();
		proxyFactory.setServiceInterface(MessageHandler.class);
		proxyFactory.setServiceUrl(url);
		proxyFactory.setLookupStubOnStartup(false);
		proxyFactory.setRefreshStubOnConnectFailure(true);
		proxyFactory.afterPropertiesSet();
		this.handlerProxy = (MessageHandler) proxyFactory.getObject();
	}


	public Message<?> handle(Message<?> message) {
		this.verifySerializability(message);
		try {
			return this.handlerProxy.handle(message);
		}
		catch (RemoteAccessException e) {
			throw new MessageHandlingException("unable to handle message remotely", e);
		}
	}

	private void verifySerializability(Message<?> message) {
		if (!(message.getPayload() instanceof Serializable)) {
			throw new MessageHandlingException(message,
					"RmiTargetAdapter expects a Serializable payload type " +
					"but encountered '" + message.getPayload().getClass().getName() + "'");
		}
		for (String attributeName : message.getHeader().getAttributeNames()) {
			Object attribute = message.getHeader().getAttribute(attributeName);
			if (!(attribute instanceof Serializable)) {
				throw new MessageHandlingException(message,
						"RmiTargetAdapter expects Serializable attribute types " +
						"but encountered '" + attribute.getClass().getName() + "' for the attribute '" +
						attributeName + "'");				
			}
		}
	}

}
