/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;

import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.inbound.Mqtt5MessageDrivenChannelAdapter;

/**
 * A spec for an MQTT 5 message-driven channel adapter.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public class Mqtt5MessageDrivenChannelAdapterSpec
		extends MqttMessageDrivenChannelAdapterSpec<Mqtt5MessageDrivenChannelAdapterSpec, Mqtt5MessageDrivenChannelAdapter> {

	Mqtt5MessageDrivenChannelAdapterSpec(ClientManager<Mqtt5Client> clientManager,
			String... topics) {

		super(new Mqtt5MessageDrivenChannelAdapter(clientManager, topics));
	}

	Mqtt5MessageDrivenChannelAdapterSpec(ClientManager<Mqtt5Client> clientManager,
			Mqtt5Subscription... subscriptions) {

		super(new Mqtt5MessageDrivenChannelAdapter(clientManager, subscriptions));
	}

	/**
	 * Set the mapper for incoming MQTT 5 publish properties.
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageDrivenChannelAdapterSpec headerMapper(HeaderMapper<Mqtt5Publish> headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	/**
	 * Set whether topic-derived subscriptions exclude this client's publishes.
	 * Explicit subscriptions retain their own options.
	 * @param noLocal the noLocal.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageDrivenChannelAdapterSpec noLocal(boolean noLocal) {
		this.target.setNoLocal(noLocal);
		return this;
	}

	/**
	 * Set retained-message handling for topic-derived subscriptions.
	 * Explicit subscriptions retain their own options.
	 * @param retainHandling the retainHandling.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageDrivenChannelAdapterSpec retainHandling(Mqtt5RetainHandling retainHandling) {
		this.target.setRetainHandling(retainHandling);
		return this;
	}

	/**
	 * Set whether topic-derived subscriptions preserve the original retain flag.
	 * Explicit subscriptions retain their own options.
	 * @param retainAsPublished the retainAsPublished.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageDrivenChannelAdapterSpec retainAsPublished(boolean retainAsPublished) {
		this.target.setRetainAsPublished(retainAsPublished);
		return this;
	}

}
