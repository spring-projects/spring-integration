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

package org.springframework.integration.channel;

import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * Base class for all pollable channels.
 *
 * @author Mark Fisher
 */
public abstract class AbstractPollableChannel extends AbstractMessageChannel implements PollableChannel {

	/**
	 * Receive the first available message from this channel. If the channel
	 * contains no messages, this method will block.
	 *
	 * @return the first available message or <code>null</code> if the
	 * receiving thread is interrupted.
	 */
	@Override
	public final Message<?> receive() {
		return this.receive(-1);
	}

	/**
	 * Receive the first available message from this channel. If the channel
	 * contains no messages, this method will block until the allotted timeout
	 * elapses. If the specified timeout is 0, the method will return
	 * immediately. If less than zero, it will block indefinitely (see
	 * {@link #receive()}).
	 *
	 * @param timeout the timeout in milliseconds
	 *
	 * @return the first available message or <code>null</code> if no message
	 * is available within the allotted time or the receiving thread is
	 * interrupted.
	 */
	@Override
	public final Message<?> receive(long timeout) {
		if (!this.getInterceptors().preReceive(this)) {
			return null;
		}
		Message<?> message = this.doReceive(timeout);
		message = this.getInterceptors().postReceive(message, this);
		return message;
	}

	/**
	 * Subclasses must implement this method. A non-negative timeout indicates
	 * how long to wait if the channel is empty (if the value is 0, it must
	 * return immediately with or without success). A negative timeout value
	 * indicates that the method should block until either a message is
	 * available or the blocking thread is interrupted.
	 *
	 * @param timeout The timeout.
	 * @return The message, or null.
	 */
	protected abstract Message<?> doReceive(long timeout);

}
