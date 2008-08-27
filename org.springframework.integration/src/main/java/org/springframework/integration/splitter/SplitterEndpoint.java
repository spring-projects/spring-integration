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

package org.springframework.integration.splitter;

import java.util.List;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class SplitterEndpoint extends AbstractEndpoint {

	private final Splitter splitter;


	public SplitterEndpoint(Splitter splitter) {
		Assert.notNull(splitter, "splitter must not be null");
		this.splitter = splitter;
	}


	// TODO: move to superclass
	private MessageTarget resolveReplyTarget(Object returnAddress) {
		MessageTarget replyTarget = this.getTarget();
		if (replyTarget == null && returnAddress != null) {
			if (returnAddress instanceof MessageTarget) {
				replyTarget = (MessageTarget) returnAddress;
			}
			else if (returnAddress instanceof String) {
				ChannelRegistry channelRegistry = this.getChannelRegistry();
				if (channelRegistry != null) {
					replyTarget = channelRegistry.lookupChannel((String) returnAddress);
				}
			}
		}
		if (replyTarget == null) {
			throw new MessagingException("unable to resolve reply target");
		}
		return replyTarget;
	}

	@Override
	protected boolean sendInternal(Message<?> message) {
		List<Message<?>> results = this.splitter.split(message);
		if (results != null) {
			for (Message<?> splitMessage : results) {
				MessageTarget replyTarget = this.resolveReplyTarget(message.getHeaders().getReturnAddress());
				this.getMessageExchangeTemplate().send(splitMessage, replyTarget);
			}
			return true;
		}
		return false;
	}


	// TODO: remove these methods after refactoring

	private volatile String inputChannelName;

	public String getInputChannelName() {
		return this.inputChannelName;
	}

	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public String getOutputChannelName() {
		if (this.getTarget() instanceof MessageChannel) {
			return ((MessageChannel) this.getTarget()).getName();
		}
		return null;
	}

}
