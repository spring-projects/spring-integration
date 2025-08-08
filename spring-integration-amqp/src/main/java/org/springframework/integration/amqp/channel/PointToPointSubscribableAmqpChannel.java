/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.amqp.channel;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;

/**
 * The {@link AbstractSubscribableAmqpChannel} implementation for one-to-one subscription
 * over AMQP queue.
 * <p>
 * If queue name is not provided, the channel bean name is used internally to declare
 * a queue via provided {@link AmqpAdmin} (if any).
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class PointToPointSubscribableAmqpChannel extends AbstractSubscribableAmqpChannel {

	private volatile Queue queue;

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @see #setExtractPayload(boolean)
	 */
	public PointToPointSubscribableAmqpChannel(String channelName, AbstractMessageListenerContainer container,
			AmqpTemplate amqpTemplate) {

		super(channelName, container, amqpTemplate);
	}

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @param outboundMapper the outbound mapper.
	 * @param inboundMapper the inbound mapper.
	 * @since 4.3
	 * @see #setExtractPayload(boolean)
	 */
	public PointToPointSubscribableAmqpChannel(String channelName, AbstractMessageListenerContainer container,
			AmqpTemplate amqpTemplate, AmqpHeaderMapper outboundMapper, AmqpHeaderMapper inboundMapper) {

		super(channelName, container, amqpTemplate, outboundMapper, inboundMapper);
	}

	/**
	 * Provide a Queue name to be used. If this is not provided,
	 * the Queue's name will be the same as the channel name.
	 * @param queueName The queue name.
	 */
	public void setQueueName(String queueName) {
		this.queue = new Queue(queueName);
	}

	@Override
	protected String obtainQueueName(String channelName) {
		if (this.queue == null) {
			this.queue = new Queue(channelName);
		}

		return this.queue.getName();
	}

	@Override
	protected AbstractDispatcher createDispatcher() {
		UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
		unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
		return unicastingDispatcher;
	}

	@Override
	protected String getRoutingKey() {
		return this.queue != null ? this.queue.getName() : super.getRoutingKey();
	}

	@Override
	protected void doDeclares() {
		AmqpAdmin admin = getAdmin();
		if (admin != null && this.queue != null && admin.getQueueProperties(this.queue.getName()) == null) {
			admin.declareQueue(this.queue);
		}
	}

}
