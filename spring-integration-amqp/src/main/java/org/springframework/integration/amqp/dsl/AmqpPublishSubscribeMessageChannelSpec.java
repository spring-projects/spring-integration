/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
