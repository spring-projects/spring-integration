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

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageSource;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves messages from a {@link MessageSource}
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class SourceEndpoint extends AbstractEndpoint {

	private final MessageSource<?> source;

	private volatile long receiveTimeout = 5000;

	private volatile long sendTimeout = -1;


	public SourceEndpoint(MessageSource<?> source) {
		Assert.notNull(source, "source must not be null");
		this.source = source;
	}


	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	public void initialize() {
		if (this.getOutputChannelName() == null && this.getOutputChannel() == null) {
			throw new ConfigurationException(
					"no output channel has been configured for source endpoint '" + this.getName() + "'");
		}
	}

	@Override
	protected final boolean supports(Message<?> message) {
		return false;
	}

	@Override
	protected final boolean handleMessage(Message<?> message) {
		return false;
	}

	public boolean poll() {
		Message<?> message = (this.source instanceof BlockingSource && this.receiveTimeout >= 0) ?
				((BlockingSource<?>) this.source).receive(this.receiveTimeout) : this.source.receive();
		if (message == null) {
			return false;
		}
		boolean sent = this.getOutputChannel().send(message, this.sendTimeout);
		if (this.source instanceof MessageDeliveryAware) {
			if (sent) {
				((MessageDeliveryAware) this.source).onSend(message);
			}
			else {
				((MessageDeliveryAware) this.source).onFailure(new MessageDeliveryException(message, "failed to send message"));
			}
		}
		return sent;
	}

}
