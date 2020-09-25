/*
 * Copyright 2014-2020 the original author or authors.
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

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.integration.amqp.channel.PollableAmqpChannel;

/**
 * A {@link AmqpMessageChannelSpec} for
 * {@link org.springframework.integration.amqp.channel.PublishSubscribeAmqpChannel}s.
 *
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public class AmqpPublishSubscribeMessageChannelSpec
		extends AmqpMessageChannelSpec<AmqpPublishSubscribeMessageChannelSpec, PollableAmqpChannel> {

	protected AmqpPublishSubscribeMessageChannelSpec(ConnectionFactory connectionFactory) {
		super(connectionFactory);
		this.amqpChannelFactoryBean.setPubSub(true);
	}

	/**
	 * @param exchange the exchange.
	 * @return the spec.
	 * @see org.springframework.integration.amqp.config.AmqpChannelFactoryBean#setExchange(FanoutExchange)
	 */
	public AmqpPublishSubscribeMessageChannelSpec exchange(FanoutExchange exchange) {
		this.amqpChannelFactoryBean.setExchange(exchange);
		return _this();
	}

}
