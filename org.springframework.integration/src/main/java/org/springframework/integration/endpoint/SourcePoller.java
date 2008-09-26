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

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class SourcePoller extends AbstractPoller {

	private final MessageSource<?> source;

	private final MessageChannel channel;

	private volatile long receiveTimeout = 1000;


	public SourcePoller(MessageSource<?> source, MessageChannel channel, Trigger trigger) {
		super(trigger);
		Assert.notNull(source, "source must not be null");
		Assert.notNull(channel, "channel must not be null");
		this.source = source;
		this.channel = channel;
	}


	/**
	 * Specify the timeout to use when receiving from the source (in milliseconds).
	 * This value will only apply if the source is a {@link BlockingSource}.
	 * <p>
	 * A negative value indicates that receive calls should block indefinitely.
	 * The default value is 1000 (1 second).
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = (this.receiveTimeout >= 0 && this.source instanceof BlockingSource)
				? ((BlockingSource<?>) this.source).receive(this.receiveTimeout)
				: this.source.receive();
		if (message == null) {
			return false;
		}
		try {
			boolean sent = this.channel.send(message); 
			if (sent && this.source instanceof MessageDeliveryAware) {
				((MessageDeliveryAware) this.source).onSend(message);
			}
			return sent;
		}
		catch (Exception e) {
			if (this.source instanceof MessageDeliveryAware) {
				((MessageDeliveryAware) this.source).onFailure(message, e);
			}
			throw (e instanceof MessagingException) ? (MessagingException) e
					: new MessageDeliveryException(message, "source poller failed to send message to channel", e);
		}
	}

}
