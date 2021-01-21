/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.rabbit.batch.BatchingStrategy;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;

/**
 * A base {@link MessagingGatewaySpec} implementation for {@link AmqpInboundGateway} endpoint options.
 * Doesn't allow to specify {@code listenerContainer} options.
 *
 * @param <S> the target {@link AmqpBaseInboundGatewaySpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see AmqpInboundGateway
 */
public class AmqpBaseInboundGatewaySpec<S extends AmqpBaseInboundGatewaySpec<S>>
		extends MessagingGatewaySpec<S, AmqpInboundGateway> {

	protected final DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper(); // NOSONAR

	protected AmqpBaseInboundGatewaySpec(AmqpInboundGateway gateway) {
		super(gateway);
		this.target.setHeaderMapper(this.headerMapper);
	}

	/**
	 * Configure the gateway's {@link MessageConverter};
	 * defaults to {@link org.springframework.amqp.support.converter.SimpleMessageConverter}.
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see AmqpInboundGateway#setMessageConverter
	 */
	public S messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Configure the gateway's {@link AmqpHeaderMapper}; defaults to
	 * {@link org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper}.
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 * @see AmqpInboundGateway#setHeaderMapper(AmqpHeaderMapper)
	 */
	public S headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * Only applies if the default header mapper is used.
	 * @param headers the headers.
	 * @return the spec.
	 * @see DefaultAmqpHeaderMapper#setRequestHeaderNames(String[])
	 */
	public S mappedRequestHeaders(String... headers) {
		this.headerMapper.setRequestHeaderNames(headers);
		return _this();
	}

	/**
	 * Only applies if the default header mapper is used.
	 * @param headers the headers.
	 * @return the spec.
	 * @see DefaultAmqpHeaderMapper#setReplyHeaderNames(String[])
	 */
	public S mappedReplyHeaders(String... headers) {
		this.headerMapper.setReplyHeaderNames(headers);
		return _this();
	}

	/**
	 * The {@code defaultReplyTo} address with the form
	 * <pre class="code">
	 * (exchange)/(routingKey)
	 * </pre>
	 * or
	 * <pre class="code">
	 * (queueName)
	 * </pre>
	 * if the request message doesn't have a {@code replyTo} property.
	 * The second form uses the default exchange ("") and the queue name as
	 * the routing key.
	 * @param defaultReplyTo the default {@code replyTo} address to use.
	 * @return the spec.
	 * @see AmqpInboundGateway#setDefaultReplyTo
	 */
	public S defaultReplyTo(String defaultReplyTo) {
		this.target.setDefaultReplyTo(defaultReplyTo);
		return _this();
	}

	/**
	 * Set a {@link RetryTemplate} to use for retrying a message delivery within the
	 * adapter.
	 * @param retryTemplate the template.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AmqpInboundGateway#setRetryTemplate(RetryTemplate)
	 */
	public S retryTemplate(RetryTemplate retryTemplate) {
		this.target.setRetryTemplate(retryTemplate);
		return _this();
	}

	/**
	 * Set a {@link RecoveryCallback} when using retry within the adapter.
	 * @param recoveryCallback the callback.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AmqpInboundGateway#setRecoveryCallback(RecoveryCallback)
	 */
	public S recoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.target.setRecoveryCallback(recoveryCallback);
		return _this();
	}

	/**
	 * Set a batching strategy to use when de-batching messages.
	 * @param batchingStrategy the strategy to use.
	 * @return the spec.
	 * @since 5.2.1
	 * @see AmqpInboundGateway#setBatchingStrategy(BatchingStrategy)
	 */
	public S batchingStrategy(BatchingStrategy batchingStrategy) {
		this.target.setBatchingStrategy(batchingStrategy);
		return _this();
	}

	/**
	 * Set to true to bind the source message in the headers.
	 * @param bindSourceMessage true to bind.
	 * @return the spec.
	 * @since 5.1.9
	 * @see AmqpInboundGateway#setBindSourceMessage(boolean)
	 */
	public S bindSourceMessage(boolean bindSourceMessage) {
		this.target.setBindSourceMessage(bindSourceMessage);
		return _this();
	}

	/**
	 * When mapping headers for the outbound (reply) message, determine whether the headers are
	 * mapped before the message is converted, or afterwards.
	 * @param replyHeadersMappedLast true if reply headers are mapped after conversion.
	 * @return the spec.
	 * @since 5.1.9
	 * @see AmqpInboundGateway#setReplyHeadersMappedLast(boolean)
	 */
	public S replyHeadersMappedLast(boolean replyHeadersMappedLast) {
		this.target.setReplyHeadersMappedLast(replyHeadersMappedLast);
		return _this();
	}

	/**
	 * Set a {@link MessageRecoverer} when using retry within the adapter.
	 * @param messageRecoverer the callback.
	 * @return the spec.
	 * @since 5.5
	 * @see org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter#setMessageRecoverer(MessageRecoverer)
	 */
	public S messageRecoverer(MessageRecoverer messageRecoverer) {
		this.target.setMessageRecoverer(messageRecoverer);
		return _this();
	}

}
