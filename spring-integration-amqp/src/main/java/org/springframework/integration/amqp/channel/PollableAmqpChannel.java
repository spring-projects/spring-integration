/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.amqp.channel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.channel.ExecutorChannelInterceptorAware;
import org.springframework.integration.support.management.PollableChannelManagement;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link PollableChannel} implementation that is backed by an AMQP Queue.
 * Messages will be sent to the default (no-name) exchange with that Queue's
 * name as the routing key.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 */
public class PollableAmqpChannel extends AbstractAmqpChannel
		implements PollableChannel, PollableChannelManagement, ExecutorChannelInterceptorAware {

	private final String channelName;

	private volatile String queueName;

	private volatile AmqpAdmin amqpAdmin;

	private volatile int executorInterceptorsSize;

	/**
	 * Construct an instance with the supplied name, template and default header mappers
	 * used if the template is a {@link RabbitTemplate} and the message is mapped.
	 * @param channelName the channel name.
	 * @param amqpTemplate the template.
	 * @see #setExtractPayload(boolean)
	 */
	public PollableAmqpChannel(String channelName, AmqpTemplate amqpTemplate) {
		super(amqpTemplate);
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
	}

	/**
	 * Construct an instance with the supplied name, template and header mappers.
	 * @param channelName the channel name.
	 * @param amqpTemplate the template.
	 * @param outboundMapper the outbound mapper.
	 * @param inboundMapper the inbound mapper.
	 * @see #setExtractPayload(boolean)
	 * @since 4.3
	 */
	public PollableAmqpChannel(String channelName, AmqpTemplate amqpTemplate, AmqpHeaderMapper outboundMapper,
			AmqpHeaderMapper inboundMapper) {
		super(amqpTemplate, outboundMapper, inboundMapper);
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
	}


	/**
	 * Provide an explicitly configured queue name. If this is not provided, then a Queue will be created
	 * implicitly with the channelName as its name. The implicit creation will require that either an AmqpAdmin
	 * instance has been provided or that the configured AmqpTemplate is an instance of RabbitTemplate.
	 *
	 * @param queueName The queue name.
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * Provide an instance of AmqpAdmin for implicitly declaring Queues if the queueName is not provided.
	 * When providing a RabbitTemplate implementation, this is not strictly necessary since a RabbitAdmin
	 * instance can be created from the template's ConnectionFactory reference.
	 *
	 * @param amqpAdmin The amqp admin.
	 */
	public void setAmqpAdmin(AmqpAdmin amqpAdmin) {
		this.amqpAdmin = amqpAdmin;
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
	protected String getRoutingKey() {
		return this.queueName;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		AmqpTemplate amqpTemplate = this.getAmqpTemplate();
		if (this.queueName == null) {
			if (this.amqpAdmin == null && amqpTemplate instanceof RabbitTemplate) {
				this.amqpAdmin = new RabbitAdmin(((RabbitTemplate) amqpTemplate).getConnectionFactory());
			}
			Assert.notNull(this.amqpAdmin,
					"If no queueName is configured explicitly, an AmqpAdmin instance must be provided, " +
					"or the AmqpTemplate must be a RabbitTemplate since the Queue needs to be declared.");
			this.queueName = this.channelName;
			this.amqpAdmin.declareQueue(new Queue(this.queueName));
		}
	}

	@Override
	public Message<?> receive() {
		ChannelInterceptorList interceptorList = getInterceptors();
		Deque<ChannelInterceptor> interceptorStack = null;
		boolean counted = false;
		boolean countsEnabled = isCountsEnabled();
		try {
			if (isLoggingEnabled() && logger.isTraceEnabled()) {
				logger.trace("preReceive on channel '" + this + "'");
			}
			if (interceptorList.getInterceptors().size() > 0) {
				interceptorStack = new ArrayDeque<ChannelInterceptor>();

				if (!interceptorList.preReceive(this, interceptorStack)) {
					 return null;
				}
			}
			Object object = doReceive();
			if (object == null) {
				if (isLoggingEnabled() && logger.isTraceEnabled()) {
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
			if (isLoggingEnabled() && logger.isDebugEnabled()) {
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


	protected Object doReceive() {
		if (!isExtractPayload()) {
			return getAmqpTemplate().receiveAndConvert(this.queueName);
		}
		else {
			RabbitTemplate rabbitTemplate = getRabbitTemplate();
			org.springframework.amqp.core.Message message = rabbitTemplate.receive(this.queueName);
			if (message != null) {
				Object payload = rabbitTemplate.getMessageConverter().fromMessage(message);
				Map<String, Object> headers = getInboundHeaderMapper()
						.toHeadersFromRequest(message.getMessageProperties());
				return getMessageBuilderFactory()
						.withPayload(payload)
						.copyHeaders(headers)
						.build();
			}
			else {
				return null;
			}
		}
	}

	@Override
	public Message<?> receive(long timeout) {
		if (isLoggingEnabled() && logger.isInfoEnabled()) {
			logger.info("Calling receive with a timeout value on PollableAmqpChannel. " +
					"The timeout will be ignored since no receive timeout is supported.");
		}
		return this.receive();
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
