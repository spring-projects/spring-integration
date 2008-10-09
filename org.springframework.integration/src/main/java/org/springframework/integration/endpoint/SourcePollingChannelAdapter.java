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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageSource;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a
 * {@link org.springframework.integration.message.MessageSource}
 * to a {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class SourcePollingChannelAdapter extends AbstractPollingEndpoint implements BeanNameAware {

	private volatile String name;

	private volatile MessageSource<?> source;

	private volatile MessageChannel outputChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	public void setBeanName(String beanName) {
		this.name = beanName;
	}

	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.channelTemplate.setSendTimeout(sendTimeout);
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.source, "source must not be null");
		Assert.notNull(this.outputChannel, "outputChannel must not be null");
		super.afterPropertiesSet();
		if (this.maxMessagesPerPoll < 0) {
			// the default is 1 since a source might return
			// a non-null value every time it is invoked
			this.setMaxMessagesPerPoll(1);
		}
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = this.source.receive();
		if (message != null) {
			return this.channelTemplate.send(message, this.outputChannel);
		}
		return false;
	}

	public String toString() {
		return this.name;
	}

}
