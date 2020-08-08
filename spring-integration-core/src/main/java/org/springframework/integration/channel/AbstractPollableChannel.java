/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

/**
 * Base class for all pollable channels.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractPollableChannel extends AbstractMessageChannel
		implements PollableChannel, ExecutorChannelInterceptorAware {

	private int executorInterceptorsSize;

	private CounterFacade receiveCounter;

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.pollable_channel;
	}

	/**
	 * Receive the first available message from this channel. If the channel
	 * contains no messages, this method will block.
	 * @return the first available message or <code>null</code> if the
	 * receiving thread is interrupted.
	 */
	@Override
	@Nullable
	public Message<?> receive() {
		return receive(-1);
	}

	/**
	 * Receive the first available message from this channel. If the channel
	 * contains no messages, this method will block until the allotted timeout
	 * elapses. If the specified timeout is 0, the method will return
	 * immediately. If less than zero, it will block indefinitely (see
	 * {@link #receive()}).
	 * @param timeout the timeout in milliseconds
	 * @return the first available message or <code>null</code> if no message
	 * is available within the allotted time or the receiving thread is
	 * interrupted.
	 */
	@Override // NOSONAR complexity
	@Nullable
	public Message<?> receive(long timeout) {
		ChannelInterceptorList interceptorList = getIChannelInterceptorList();
		Deque<ChannelInterceptor> interceptorStack = null;
		boolean counted = false;
		boolean traceEnabled = isLoggingEnabled() && logger.isTraceEnabled();
		try {
			if (traceEnabled) {
				logger.trace("preReceive on channel '" + this + "'");
			}
			if (interceptorList.getSize() > 0) {
				interceptorStack = new ArrayDeque<>();

				if (!interceptorList.preReceive(this, interceptorStack)) {
					return null;
				}
			}
			Message<?> message = doReceive(timeout);
			if (message == null) {
				if (traceEnabled) {
					logger.trace("postReceive on channel '" + this + "', message is null");
				}
			}
			else {
				incrementReceiveCounter();
				counted = true;

				if (logger.isDebugEnabled()) {
					logger.debug("postReceive on channel '" + this + "', message: " + message);
				}
			}

			if (interceptorStack != null && message != null) {
				message = interceptorList.postReceive(message, this);
			}
			interceptorList.afterReceiveCompletion(message, this, null, interceptorStack);
			return message;
		}
		catch (RuntimeException ex) {
			if (!counted) {
				incrementReceiveErrorCounter(ex);
			}
			interceptorList.afterReceiveCompletion(null, this, ex, interceptorStack);
			throw ex;
		}
	}

	private void incrementReceiveCounter() {
		MetricsCaptor metricsCaptor = getMetricsCaptor();
		if (metricsCaptor != null) {
			if (this.receiveCounter == null) {
				this.receiveCounter = buildReceiveCounter(metricsCaptor, null);
			}
			this.receiveCounter.increment();
		}
	}

	private void incrementReceiveErrorCounter(Exception ex) {
		MetricsCaptor metricsCaptor = getMetricsCaptor();
		if (metricsCaptor != null) {
			buildReceiveCounter(metricsCaptor, ex).increment();
		}
	}

	private CounterFacade buildReceiveCounter(MetricsCaptor metricsCaptor, @Nullable Exception ex) {
		CounterFacade counterFacade = metricsCaptor
				.counterBuilder(RECEIVE_COUNTER_NAME)
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("type", "channel")
				.tag("result", ex == null ? "success" : "failure")
				.tag("exception", ex == null ? "none" : ex.getClass().getSimpleName())
				.description("Messages received")
				.build();
		this.meters.add(counterFacade);
		return counterFacade;
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
	@Nullable
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
	 * @param timeout The timeout.
	 * @return The message, or null.
	 */
	@Nullable
	protected abstract Message<?> doReceive(long timeout);

}
