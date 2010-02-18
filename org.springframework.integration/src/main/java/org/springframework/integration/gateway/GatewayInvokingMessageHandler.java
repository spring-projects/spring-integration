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

package org.springframework.integration.gateway;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHistoryEvent;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;

/**
 * Will decorate 'gateway' as {@link MessageHandler} so it could be included in the chain.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GatewayInvokingMessageHandler extends AbstractReplyProducingMessageHandler {

	private static final String COMPONENT_TYPE_LABEL = "gateway";


	private GenericSendAndRecieveGateway gateway;

	/**
	 * @param gateway
	 */
	public GatewayInvokingMessageHandler(GenericSendAndRecieveGateway gateway) {
		Assert.notNull(gateway, "gateway must not be null");
		this.gateway = gateway;
	}

	/**
	 * Will simply delegate to the original gateway
	 */
	protected Object handleRequestMessage(Message<?> requestMessage) {
		return gateway.sendAndRecieve(requestMessage);
	}

	@Override
	protected void postProcessHistoryEvent(MessageHistoryEvent event) {
		event.setComponentType(COMPONENT_TYPE_LABEL);
	}

}
