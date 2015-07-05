/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.integration.channel.management.PollableChannelManagement;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * Base class for all pollable channels.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class AbstractPollableChannel extends AbstractMessageChannel implements PollableChannel,
		PollableChannelManagement {


	@Override
	public int getReceiveCount() {
		return getMetrics().getReceiveCount();
	}

	@Override
	public long getReceiveCountLong() {
		return getMetrics().getReceiveCountLong();
	}

	@Override
	public int getReceiveErrorCount() {
		return getMetrics().getReceiveErrorCount();
	}

	@Override
	public long getReceiveErrorCountLong() {
		return getMetrics().getReceiveErrorCountLong();
	}

	/**
	 * Receive the first available message from this channel. If the channel
	 * contains no messages, this method will block.
	 *
	 * @return the first available message or <code>null</code> if the
	 * receiving thread is interrupted.
	 */
	@Override
	public final Message<?> receive() {
		return receive(-1);
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
		ChannelInterceptorList interceptorList = getInterceptors();
		Deque<ChannelInterceptor> interceptorStack = null;
		boolean counted = false;
		boolean countsEnabled = isCountsEnabled();
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("preReceive on channel '" + this + "'");
			}
			if (interceptorList.getSize() > 0) {
				interceptorStack = new ArrayDeque<ChannelInterceptor>();

				if (!interceptorList.preReceive(this, interceptorStack)) {
					return null;
				}
			}
			Message<?> message = this.doReceive(timeout);
			if (countsEnabled) {
				getMetrics().afterReceive();
				counted = true;
			}
			if (message != null && logger.isDebugEnabled()) {
				logger.debug("postReceive on channel '" + this + "', message: " + message);
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("postReceive on channel '" + this + "', message is null");
			}
			if (interceptorStack != null) {
				message = interceptorList.postReceive(message, this);
				interceptorList.afterReceiveCompletion(message, this, null, interceptorStack);
			}
			return message;
		}
		catch (RuntimeException e) {
			if (countsEnabled && !counted) {
				getMetrics().afterError();
			}
			if (interceptorStack != null) {
				interceptorList.afterReceiveCompletion(null, this, e, interceptorStack);
			}
			throw e;
		}
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
