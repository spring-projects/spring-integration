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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.util.Assert;

/**
 * Adapts a {@link RequestReplyExchanger} to the {@link MessageHandler} interface.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
class RequestReplyMessageHandlerAdapter extends AbstractReplyProducingMessageHandler {

	private RequestReplyExchanger exchanger;

	/**
	 * @param exchanger
	 */
	public RequestReplyMessageHandlerAdapter(RequestReplyExchanger exchanger) {
		Assert.notNull(exchanger, "exchanger must not be null");
		this.exchanger = exchanger;
	}

	/**
	 * Delegates to the exchanger.
	 */
	protected Object handleRequestMessage(Message<?> requestMessage) {
		return exchanger.exchange(requestMessage);
	}

}
