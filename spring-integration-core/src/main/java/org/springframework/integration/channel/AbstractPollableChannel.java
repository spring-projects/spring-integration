/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;

import org.springframework.integration.support.management.PollableChannelManagement;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.util.CollectionUtils;

import io.micrometer.core.instrument.Counter;

/**
 * Base class for all pollable channels.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractPollableChannel extends AbstractMessageChannel
		implements PollableChannel, PollableChannelManagement, ExecutorChannelInterceptorAware {

	private volatile int executorInterceptorsSize;

	private Counter receiveCounter;

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
	public Message<?> receive() {
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
	public Message<?> receive(long timeout) {
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
			if (countsEnabled && message != null) {
				if (getMeterRegistry() != null) {
					incrementReceiveCounter();
				}
				getMetrics().afterReceive();
				counted = true;
			}
			if (message != null && logger.isDebugEnabled()) {
				logger.debug("postReceive on channel '" + this + "', message: " + message);
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("postReceive on channel '" + this + "', message is null");
			}
			if (!CollectionUtils.isEmpty(interceptorStack)) {
				message = interceptorList.postReceive(message, this);
				interceptorList.afterReceiveCompletion(message, this, null, interceptorStack);
			}
			return message;
		}
		catch (RuntimeException e) {
			if (countsEnabled && !counted) {
				if (getMeterRegistry() != null) {
					Counter.builder(RECEIVE_COUNTER_NAME)
							.tag("name", getComponentName() == null ? "unknown" : getComponentName())
							.tag("type", "channel")
							.tag("result", "failure")
							.tag("exception", e.getClass().getSimpleName())
							.description("Messages received")
							.register(getMeterRegistry())
							.increment();
				}
				getMetrics().afterError();
			}
			if (!CollectionUtils.isEmpty(interceptorStack)) {
				interceptorList.afterReceiveCompletion(null, this, e, interceptorStack);
			}
			throw e;
		}
	}

	private void incrementReceiveCounter() {
		if (this.receiveCounter == null) {
			this.receiveCounter = Counter.builder(RECEIVE_COUNTER_NAME)
					.tag("name", getComponentName())
					.tag("type", "channel")
					.tag("result", "success")
					.tag("exception", "none")
					.description("Messages received")
					.register(getMeterRegistry());
		}
		this.receiveCounter.increment();
	}

	@Override
	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		super.setInterceptors(interceptors);
		for (ChannelInterceptor interceptor : interceptors) {
			if (interceptor instanceof ExecutorChannelInterceptor) {
				this.executorInterceptorsSize++;
			}
		}
	}

	@Override
	public void addInterceptor(ChannelInterceptor interceptor) {
		super.addInterceptor(interceptor);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize++;
		}
	}

	@Override
	public void addInterceptor(int index, ChannelInterceptor interceptor) {
		super.addInterceptor(index, interceptor);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize++;
		}
	}

	@Override
	public boolean removeInterceptor(ChannelInterceptor interceptor) {
		boolean removed = super.removeInterceptor(interceptor);
		if (removed && interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize--;
		}
		return removed;
	}

	@Override
	public ChannelInterceptor removeInterceptor(int index) {
		ChannelInterceptor interceptor = super.removeInterceptor(index);
		if (interceptor instanceof ExecutorChannelInterceptor) {
			this.executorInterceptorsSize--;
		}
		return interceptor;
	}

	@Override
	public boolean hasExecutorInterceptors() {
		return this.executorInterceptorsSize > 0;
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
