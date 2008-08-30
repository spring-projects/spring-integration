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

package org.springframework.integration.endpoint;

import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * @author Mark Fisher
 */
public abstract class AbstractRequestReplyEndpoint extends AbstractEndpoint {

	private volatile boolean requiresReply = false;


	/**
	 * Specify whether this endpoint should throw an Exception when
	 * it returns an invalid reply Message after handling the request.
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}


	protected boolean sendInternal(Message<?> requestMessage) {
		Message<?> replyMessage = this.handleRequestMessage(requestMessage);
		if (!this.isValidReplyMessage(replyMessage)) {
			if (this.requiresReply) {
				throw new MessageHandlingException(requestMessage,
						"endpoint requires reply but none was received");
			}
		}
		else if (replyMessage instanceof CompositeMessage) {
			for (Message<?> nextReply : (CompositeMessage) replyMessage) {
				this.sendReplyMessage(nextReply, requestMessage);
			}
		}
		else {
			this.sendReplyMessage(replyMessage, requestMessage);
		}
		return true;
	}

	protected abstract Message<?> handleRequestMessage(Message<?> requestMessage);

	protected abstract boolean isValidReplyMessage(Message<?> replyMessage);

	protected abstract void sendReplyMessage(Message<?> replyMessage, Message<?> requestMessage);

}
