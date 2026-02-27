/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jms.channel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.channel.ExecutorChannelInterceptorAware;
import org.springframework.integration.jms.DynamicJmsTemplateProperties;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

/**
 * A JMS-backed channel from which messages can be received through polling.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 7.0
 */
public class PollableJmsChannel extends AbstractJmsChannel
		implements PollableChannel, ExecutorChannelInterceptorAware {

	private @Nullable String messageSelector;

	private @Nullable CounterFacade receiveCounter;

	private volatile int executorInterceptorsSize;

	public PollableJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	@Override
	public @Nullable Message<?> receive(long timeout) {
		try {
			DynamicJmsTemplateProperties.setReceiveTimeout(timeout);
			return receive();
		}
		finally {
			DynamicJmsTemplateProperties.clearReceiveTimeout();
		}
	}

	@Override
	public @Nullable Message<?> receive() {
		ChannelInterceptorList interceptorList = getIChannelInterceptorList();
		Deque<ChannelInterceptor> interceptorStack = null;
		boolean counted = false;
		try {
			logger.trace(() -> "preReceive on channel '" + this + "'");
			if (!interceptorList.getInterceptors().isEmpty()) {
				interceptorStack = new ArrayDeque<>();

				if (!interceptorList.preReceive(this, interceptorStack)) {
					return null;
				}
			}

			Message<?> message = receiveAndConvertToMessage();
			if (message != null) {
				incrementReceiveCounter();
				counted = true;
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

	private @Nullable Message<?> receiveAndConvertToMessage() {
		jakarta.jms.Message jmsMessage;
		if (this.messageSelector == null) {
			jmsMessage = this.jmsTemplate.receive();
		}
		else {
			jmsMessage = this.jmsTemplate.receiveSelected(this.messageSelector);
		}

		if (jmsMessage == null) {
			logger.trace(() -> "postReceive on channel '" + this + "', message is null");
			return null;
		}

		Message<?> message = fromJmsMessage(jmsMessage);

		logger.debug(() -> "postReceive on channel '" + this + "', message: " + message);

		return message;
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
				.tag("name", getComponentName())
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
