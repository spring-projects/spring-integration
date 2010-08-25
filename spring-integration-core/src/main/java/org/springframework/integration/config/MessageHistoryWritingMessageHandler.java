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

package org.springframework.integration.config;

import org.springframework.core.Ordered;
import org.springframework.integration.Message;
import org.springframework.integration.context.MessageHistoryWriter;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

/**
 * Wrapper class to be used when a particular MessageHandler needs to be tracked in MessageHistory.
 * Note, any MessageHandler that is wrapped by this class will be tracked in MessageHistory
 * only when a MessageHistoryWriter is present in the ApplicationContext.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
class MessageHistoryWritingMessageHandler implements NamedComponent, MessageHandler, Ordered {

	private final MessageHandler targetHandler;

	private final MessageHistoryWriter historyWriter;

	private final String componentName;


	/**
	 * @param historyWriter
	 * @param endpointName
	 * @param targetHandler
	 */
	public MessageHistoryWritingMessageHandler(MessageHandler targetHandler, MessageHistoryWriter historyWriter,  String endpointName) {
		Assert.notNull(targetHandler, "targetHandler must not be null");
		Assert.notNull(historyWriter, "historyWriter must not be null");
		this.targetHandler = targetHandler;
		this.historyWriter = historyWriter;
		this.componentName = endpointName;
	}


	public String getComponentName() {
		return this.componentName;
	}

	public String getComponentType() {
		return (targetHandler instanceof NamedComponent) ? ((NamedComponent) targetHandler).getComponentType() : null;
	}

	public int getOrder() {
		return (targetHandler instanceof Ordered) ? ((Ordered) targetHandler).getOrder() : Ordered.LOWEST_PRECEDENCE;
	}


	/**
	 * Writes the MessageHistory event and then invokes the target handler.
	 */
	public void handleMessage(Message<?> message) {
		if (message != null) {
			message = this.historyWriter.writeHistory(this, message);
		}
		this.targetHandler.handleMessage(message);
	}

}
