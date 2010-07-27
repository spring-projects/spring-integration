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
package org.springframework.integration.history;

import org.springframework.core.Ordered;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;

/**
 * Wrapper class to be used when a particular MessageHandler needs to be tracked in MessageHistory.
 * Note, any MessageHandler that is wrapped by this class will be tracked in MessageHistory
 * only when MessageHistoryWriter is present.Ê
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MessageHistoryAwareMessageHandler implements NamedComponent, MessageHandler, Ordered {
	private MessageHandler parentHandler;
	private String componentName;
	private MessageHistoryWriter historyWriter;
	private int order;
	/**
	 * 
	 * @param historyWriter
	 * @param endpointName
	 * @param parentHandler
	 */
	public MessageHistoryAwareMessageHandler(MessageHistoryWriter historyWriter,  String endpointName, MessageHandler parentHandler){
		this.historyWriter = historyWriter;
		this.componentName = endpointName;
		this.parentHandler = parentHandler;
		if (parentHandler instanceof Ordered){
			this.order = ((Ordered)parentHandler).getOrder();
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.message.MessageHandler#handleMessage(org.springframework.integration.core.Message)
	 */
	public void handleMessage(Message<?> message)
			throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		historyWriter.writeHistory(this, message);
		parentHandler.handleMessage(message);
	}

	public String getComponentType() {
		return ((NamedComponent)parentHandler).getComponentType();
	}

	public int getOrder() {
		return this.order;
	}

	public String getComponentName() {
		return this.componentName;
	}
}
