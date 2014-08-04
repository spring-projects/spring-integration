/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;

/**
 * Base class for MessageHandlers that set outputchannel.
 *
 * @author David Liu
 * since 4.1
 */
public abstract class AbstractMessageProducingMessageHandler extends AbstractMessageHandler
		implements MessageProducer {

	private MessageChannel outputChannel;

	private String outputChannelName;


	private volatile boolean requiresReply = false;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	public boolean isRequiresReply() {
		return requiresReply;
	}

	public MessagingTemplate getMessagingTemplate() {
		return messagingTemplate;
	}

	/**
	 * Set the timeout for sending reply Messages.
	 *
	 * @param sendTimeout The send timeout.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Flag whether a reply is required. If true an incoming message MUST result in a reply message being sent.
	 * If false an incoming message MAY result in a reply message being sent. Default is false.
	 *
	 * @param requiresReply true if a reply is required.
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public MessageChannel getOutputChannel() {
		return outputChannel;
	}

	public String getOutputChannelName() {
		return outputChannelName;
	}

}
