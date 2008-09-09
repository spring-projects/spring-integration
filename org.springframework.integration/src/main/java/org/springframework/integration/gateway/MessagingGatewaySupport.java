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

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.MessageChannelTemplate;

/**
 * A convenient base class providing access to a {@link MessageChannelTemplate} and exposing setter methods for
 * configuring request and reply {@link MessageChannel MessageChannels}. May be used as a base class for framework
 * components so that the details of messaging are well-encapsulated and hidden from application code. For example,
 * see {@link SimpleMessagingGateway}.
 * 
 * @author Mark Fisher
 */
public abstract class MessagingGatewaySupport {

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	/**
	 * Set the timeout value for sending request messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param requestTimeout the timeout value in milliseconds
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.channelTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.channelTemplate.setReceiveTimeout(replyTimeout);
	}

	/**
	 * Retrieve the {@link MessageChannelTemplate} for performing
	 * send and receive operations across channels.
	 */
	protected final MessageChannelTemplate getChannelTemplate() {
		return this.channelTemplate;
	}

}
