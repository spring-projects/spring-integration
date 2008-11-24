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

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.config.PollerMetadata;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageSource;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a
 * {@link MessageSource} to a {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class SourcePollingChannelAdapter extends AbstractPollingEndpoint {

	private volatile MessageSource<?> source;

	private volatile MessageChannel outputChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	/**
	 * Specify the source to be polled for Messages.
	 */
	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	/**
	 * Specify the {@link MessageChannel} where Messages should be sent.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Specify the maximum time to wait for a Message to be sent to the
	 * output channel.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.channelTemplate.setSendTimeout(sendTimeout);
	}

	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.setTrigger(pollerMetadata.getTrigger());
		this.setMaxMessagesPerPoll(pollerMetadata.getMaxMessagesPerPoll());
		this.setTaskExecutor(pollerMetadata.getTaskExecutor());
		this.setTransactionDefinition(pollerMetadata.getTransactionDefinition());
		this.setTransactionManager(pollerMetadata.getTransactionManager());
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.source, "source must not be null");
		Assert.notNull(this.outputChannel, "outputChannel must not be null");
		if (this.maxMessagesPerPoll < 0) {
			// the default is 1 since a source might return
			// a non-null value every time it is invoked
			this.setMaxMessagesPerPoll(1);
		}
		super.onInit();
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = this.source.receive();
		if (message != null) {
			return this.channelTemplate.send(message, this.outputChannel);
		}
		return false;
	}

}
