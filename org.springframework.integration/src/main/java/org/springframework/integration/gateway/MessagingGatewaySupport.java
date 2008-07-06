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

package org.springframework.integration.gateway;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAware;
import org.springframework.integration.channel.MessageChannel;

/**
 * A convenient base class providing access to a {@link RequestReplyTemplate} and exposing setter methods for
 * configuring request and reply {@link MessageChannel MessageChannels}. May be used as a base class for framework
 * components so that the details of messaging are well-encapsulated and hidden from application code. For example,
 * see {@link SimpleMessagingGateway}.
 * 
 * @author Mark Fisher
 */
public abstract class MessagingGatewaySupport implements MessageBusAware {

	private final RequestReplyTemplate requestReplyTemplate = new RequestReplyTemplate();


	public MessagingGatewaySupport(MessageChannel requestChannel) {
		this.requestReplyTemplate.setRequestChannel(requestChannel);
	}

	public MessagingGatewaySupport() {
		super();
	}


	public void setMessageBus(MessageBus messageBus) {
		this.requestReplyTemplate.setMessageBus(messageBus);
	}

	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestReplyTemplate.setRequestChannel(requestChannel);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.requestReplyTemplate.setReplyChannel(replyChannel);
	}

	public void setRequestTimeout(long requestTimeout) {
		this.requestReplyTemplate.setRequestTimeout(requestTimeout);
	}

	public void setReplyTimeout(long replyTimeout) {
		this.requestReplyTemplate.setReplyTimeout(replyTimeout);
	}

	protected final RequestReplyTemplate getRequestReplyTemplate() {
		return this.requestReplyTemplate;
	}

}
