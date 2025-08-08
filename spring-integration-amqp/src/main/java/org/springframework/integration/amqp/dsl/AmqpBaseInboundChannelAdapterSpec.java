/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;

/**
 * The base {@link MessageProducerSpec} implementation for a {@link AmqpInboundChannelAdapter}.
 *
 * @param <S> the target {@link AmqpBaseInboundChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class AmqpBaseInboundChannelAdapterSpec<S extends AmqpBaseInboundChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, AmqpInboundChannelAdapter> {

	protected final DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper(); // NOSONAR

	protected AmqpBaseInboundChannelAdapterSpec(AmqpInboundChannelAdapter producer) {
		super(producer);
		this.target.setHeaderMapper(this.headerMapper);
	}

	/**
	 * Configure the adapter's {@link MessageConverter};
	 * defaults to {@link org.springframework.amqp.support.converter.SimpleMessageConverter}.
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see AmqpInboundChannelAdapter#setMessageConverter
	 */
	public S messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Configure the adapter's {@link AmqpHeaderMapper};
	 * defaults to {@link DefaultAmqpHeaderMapper}.
	 * @param headerMapper the headerMapper.
	 * @return the spec.
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
	 * Set a {@link RetryTemplate} to use for retrying a message delivery within the
	 * adapter.
	 * @param retryTemplate the template.
	 * @return the spec.
	 * @since 5.0.2
	 * @see AmqpInboundChannelAdapter#setRetryTemplate(RetryTemplate)
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
	 * @see AmqpInboundChannelAdapter#setRecoveryCallback(RecoveryCallback)
	 */
	public S recoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.target.setRecoveryCallback(recoveryCallback);
		return _this();
	}

	/**
	 * Set a {@link MessageRecoverer} when using retry within the adapter.
	 * @param messageRecoverer the callback.
	 * @return the spec.
	 * @since 5.5
	 * @see AmqpInboundChannelAdapter#setMessageRecoverer(MessageRecoverer)
	 */
	public S messageRecoverer(MessageRecoverer messageRecoverer) {
		this.target.setMessageRecoverer(messageRecoverer);
		return _this();
	}

}
