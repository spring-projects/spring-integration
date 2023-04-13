/*
 * Copyright 2022-2023 the original author or authors.
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

import org.springframework.integration.amqp.outbound.RabbitStreamMessageHandler;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.messaging.MessageChannel;
import org.springframework.rabbit.stream.producer.RabbitStreamOperations;

/**
 * The base {@link MessageHandlerSpec} for {@link RabbitStreamMessageHandler}s.
 *
 * @author Gary Russell
 *
 * @since 6.0
 */
public class RabbitStreamMessageHandlerSpec
		extends MessageHandlerSpec<RabbitStreamMessageHandlerSpec, RabbitStreamMessageHandler> {

	private final DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();

	RabbitStreamMessageHandlerSpec(RabbitStreamOperations operations) {
		this.target = new RabbitStreamMessageHandler(operations);
	}

	/**
	 * Set a custom {@link AmqpHeaderMapper} for mapping request and reply headers.
	 * @param headerMapper the {@link AmqpHeaderMapper} to use.
	 * @return this spec.
	 */
	public RabbitStreamMessageHandlerSpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	/**
	 * Provide the header names that should be mapped from a request to a
	 * {@link org.springframework.messaging.MessageHeaders}.
	 * @param headers The request header names.
	 * @return this spec.
	 */
	public RabbitStreamMessageHandlerSpec mappedRequestHeaders(String... headers) {
		this.headerMapper.setRequestHeaderNames(headers);
		return this;
	}

	/**
	 * Determine whether the headers are
	 * mapped before the message is converted, or afterwards.
	 * @param headersLast true to map headers last.
	 * @return this spec.
	 * @see RabbitStreamMessageHandler#setHeadersMappedLast(boolean)
	 */
	public RabbitStreamMessageHandlerSpec headersMappedLast(boolean headersLast) {
		this.target.setHeadersMappedLast(headersLast);
		return this;
	}

	/**
	 * Set the success channel.
	 * @param channel the channel.
	 * @return this spec.
	 */
	public RabbitStreamMessageHandlerSpec sendSuccessChannel(MessageChannel channel) {
		this.target.setSendSuccessChannel(channel);
		return this;
	}

	/**
	 * Set the failure channel. After a send failure, an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent
	 * to this channel with a payload of the exception with the
	 * failed message.
	 * @param channel the channel.
	 * @return this spec.
	 */
	public RabbitStreamMessageHandlerSpec sendFailureChannel(MessageChannel channel) {
		this.target.setSendFailureChannel(channel);
		return this;
	}

	/**
	 * Set the success channel.
	 * @param channel the channel.
	 * @return this spec.
	 */
	public RabbitStreamMessageHandlerSpec sendSuccessChannel(String channel) {
		this.target.setSendSuccessChannelName(channel);
		return this;
	}

	/**
	 * Set the failure channel. After a send failure, an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent
	 * to this channel with a payload of the exception with the
	 * failed message.
	 * @param channel the channel.
	 * @return this spec.
	 */
	public RabbitStreamMessageHandlerSpec sendFailureChannel(String channel) {
		this.target.setSendFailureChannelName(channel);
		return this;
	}

	/**
	 * Set to true to wait for a confirmation.
	 * @param sync true to wait.
	 * @return this spec.
	 * @see #confirmTimeout(long)
	 */
	public RabbitStreamMessageHandlerSpec sync(boolean sync) {
		this.target.setSync(sync);
		return this;
	}

	/**
	 * Set a timeout for the confirm result.
	 * @param timeout the approximate timeout.
	 * @return this spec.
	 * @see #sync(boolean)
	 */
	public RabbitStreamMessageHandlerSpec confirmTimeout(long timeout) {
		this.target.setConfirmTimeout(timeout);
		return this;
	}

}
