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

package org.springframework.integration.jms;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.integration.channel.ExecutorChannelInterceptorAware;
import org.springframework.integration.support.management.PollableChannelManagement;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class PollableJmsChannel extends AbstractJmsChannel
		implements PollableChannel, PollableChannelManagement, ExecutorChannelInterceptorAware {

	private volatile String messageSelector;

	private volatile int executorInterceptorsSize;

	public PollableJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

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

	@Override
	public Message<?> receive() {
		ChannelInterceptorList interceptorList = getInterceptors();
		Deque<ChannelInterceptor> interceptorStack = null;
		boolean counted = false;
		boolean countsEnabled = isCountsEnabled();
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("preReceive on channel '" + this + "'");
			}
			if (interceptorList.getInterceptors().size() > 0) {
				interceptorStack = new ArrayDeque<ChannelInterceptor>();

				if (!interceptorList.preReceive(this, interceptorStack)) {
					return null;
				}
			}
			Object object;
			if (this.messageSelector == null) {
				object = getJmsTemplate().receiveAndConvert();
			}
			else {
				object = getJmsTemplate().receiveSelectedAndConvert(this.messageSelector);
			}

			if (object == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("postReceive on channel '" + this + "', message is null");
				}
				return null;
			}
			if (countsEnabled) {
				getMetrics().afterReceive();
				counted = true;
			}
			Message<?> message = null;
			if (object instanceof Message<?>) {
				message = (Message<?>) object;
			}
			else {
				message = getMessageBuilderFactory().withPayload(object).build();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("postReceive on channel '" + this + "', message: " + message);
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

	@Override
	public Message<?> receive(long timeout) {
		try {
			DynamicJmsTemplateProperties.setReceiveTimeout(timeout);
			return this.receive();
		}
		finally {
			DynamicJmsTemplateProperties.clearReceiveTimeout();
		}
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

}
