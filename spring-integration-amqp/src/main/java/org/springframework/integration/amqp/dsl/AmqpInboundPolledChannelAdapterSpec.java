/*
 * Copyright 2018-2020 the original author or authors.
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

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.inbound.AmqpMessageSource;
import org.springframework.integration.amqp.inbound.AmqpMessageSource.AmqpAckCallbackFactory;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dsl.MessageSourceSpec;

/**
 * Spec for a polled AMQP inbound channel adapter.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
public class AmqpInboundPolledChannelAdapterSpec
		extends MessageSourceSpec<AmqpInboundPolledChannelAdapterSpec, AmqpMessageSource> {

	protected AmqpInboundPolledChannelAdapterSpec(ConnectionFactory connectionFactory, String queue) {
		this.target = new AmqpMessageSource(connectionFactory, queue);
	}

	protected AmqpInboundPolledChannelAdapterSpec(ConnectionFactory connectionFactory,
			AmqpAckCallbackFactory ackCallbackFactory, String queue) {

		this.target = new AmqpMessageSource(connectionFactory, ackCallbackFactory, queue);
	}

	public AmqpInboundPolledChannelAdapterSpec transacted(boolean transacted) {
		this.target.setTransacted(transacted);
		return this;
	}

	public AmqpInboundPolledChannelAdapterSpec propertiesConverter(MessagePropertiesConverter propertiesConverter) {
		this.target.setPropertiesConverter(propertiesConverter);
		return this;
	}

	public AmqpInboundPolledChannelAdapterSpec headerMapper(AmqpHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	public AmqpInboundPolledChannelAdapterSpec messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	public AmqpInboundPolledChannelAdapterSpec rawMessageHeader(boolean rawMessageHeader) {
		this.target.setRawMessageHeader(rawMessageHeader);
		return this;
	}

}
