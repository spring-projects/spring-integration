/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.dsl.MessageProducerSpec;

/**
 * The base {@link MessageProducerSpec} implementation for a {@link AmqpInboundChannelAdapter}.
 *
 * @param <S> the target {@link AmqpBaseInboundChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class AmqpBaseInboundChannelAdapterSpec<S extends AmqpBaseInboundChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, AmqpInboundChannelAdapter> {

	private final DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	AmqpBaseInboundChannelAdapterSpec(AmqpInboundChannelAdapter producer) {
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

}
