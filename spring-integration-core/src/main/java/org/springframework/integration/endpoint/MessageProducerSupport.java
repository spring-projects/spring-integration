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

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.util.Assert;

/**
 * A support class for producer endpoints that provides a setter for the
 * output channel and a convenience method for sending Messages.
 * 
 * @author Mark Fisher
 */
public abstract class MessageProducerSupport extends AbstractEndpoint implements MessageProducer {

	private volatile MessageChannel outputChannel;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.outputChannel, "outputChannel is required");
	}

	protected void sendMessage(Message<?> message) {
		if (message != null) {
			message.getHeaders().getHistory().addEvent(this);
		}
		this.messagingTemplate.send(this.outputChannel, message);
	}

}
