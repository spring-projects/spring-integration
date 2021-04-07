/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.kafka.channel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.integration.channel.ExecutorChannelInterceptorAware;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.util.Assert;

/**
 * Pollable channel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public class PollableKafkaChannel extends AbstractKafkaChannel
		implements PollableChannel, ExecutorChannelInterceptorAware {

	private final KafkaMessageSource<?, ?> source;

	private CounterFacade receiveCounter;

	private volatile int executorInterceptorsSize;

	/**
	 * Construct an instance with the provided parameters.
	 * @param template the template for sending.
	 * @param source the source for receiving.
	 */
	public PollableKafkaChannel(KafkaOperations<?, ?> template, KafkaMessageSource<?, ?> source) {
		super(template, topic(source));
		this.source = source;
		if (source.getConsumerProperties().getGroupId() == null) {
			String groupId = getGroupId();
			source.getConsumerProperties().setGroupId(groupId != null ? groupId : getBeanName());
		}
	}

	@Override
	@Nullable
	public Message<?> receive() {
		return doReceive();
	}

	@Override
	@Nullable
	public Message<?> receive(long timeout) {
		return doReceive();
	}

	@Nullable
	protected Message<?> doReceive() {
		ChannelInterceptorList interceptorList = getIChannelInterceptorList();
		Deque<ChannelInterceptor> interceptorStack = null;
		boolean counted = false;
		try {
			if (isLoggingEnabled()) {
				logger.trace(() -> "preReceive on channel '" + this + "'");
			}
			if (interceptorList.getInterceptors().size() > 0) {
				interceptorStack = new ArrayDeque<>();
				if (!interceptorList.preReceive(this, interceptorStack)) {
					return null;
				}
			}
			Message<?> message = this.source.receive();
			if (message != null) {
				incrementReceiveCounter();
				counted = true;
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

	private static String topic(KafkaMessageSource<?, ?> source) {
		Assert.notNull(source, "'source' cannot be null");
		String[] topics = source.getConsumerProperties().getTopics();
		Assert.isTrue(topics != null && topics.length == 1, "Only one topic is allowed");
		return topics[0];
	}

}
