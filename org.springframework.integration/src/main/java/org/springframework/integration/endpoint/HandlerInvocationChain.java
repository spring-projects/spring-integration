/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.core.Ordered;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

/**
 * A {@link MessageHandler} implementation that delegates to a target
 * handler but also adds history information.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
 // TODO: add support for interceptors.
class HandlerInvocationChain implements MessageHandler, Ordered {

	private final MessageHandler handler;

	private final String endpointName;

	private final String handlerType;


	public HandlerInvocationChain(MessageHandler handler, String endpointName) {
		this.handler = handler;
		this.endpointName = endpointName;
		this.handlerType = (handler instanceof IntegrationObjectSupport) ?
				((IntegrationObjectSupport) this.handler).getComponentType() : null;
	}


	public int getOrder() {
		return (this.handler instanceof Ordered) ?
				((Ordered) this.handler).getOrder() : Ordered.LOWEST_PRECEDENCE;
	}

	public void handleMessage(Message<?> message) {
		if (message != null && this.endpointName != null) {
			message.getHeaders().getHistory().addEvent(this.endpointName, this.handlerType);
		}
		this.handler.handleMessage(message);
	}

}
