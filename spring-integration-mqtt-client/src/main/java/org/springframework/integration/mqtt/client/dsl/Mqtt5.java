/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;

import org.springframework.integration.mqtt.client.core.ClientManager;

/**
 * Factory methods for MQTT 5 Java DSL components.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public final class Mqtt5 {

	/**
	 * Create an inbound adapter with the provided topic filters.
	 * The client manager must be managed independently as a Spring bean.
	 * @param clientManager the client manager.
	 * @param topics the topic filters.
	 * @return the spec.
	 */
	public static Mqtt5MessageDrivenChannelAdapterSpec inboundAdapter(ClientManager<Mqtt5Client> clientManager,
			String... topics) {

		return new Mqtt5MessageDrivenChannelAdapterSpec(clientManager, topics);
	}

	/**
	 * Create an inbound adapter with the provided native subscriptions.
	 * The client manager must be managed independently as a Spring bean.
	 * @param clientManager the client manager.
	 * @param subscriptions the native subscriptions.
	 * @return the spec.
	 */
	public static Mqtt5MessageDrivenChannelAdapterSpec inboundAdapter(ClientManager<Mqtt5Client> clientManager,
			Mqtt5Subscription... subscriptions) {

		return new Mqtt5MessageDrivenChannelAdapterSpec(clientManager, subscriptions);
	}

	/**
	 * Create an outbound adapter.
	 * The client manager must be managed independently as a Spring bean.
	 * @param clientManager the client manager.
	 * @return the spec.
	 */
	public static Mqtt5MessageHandlerSpec outboundAdapter(ClientManager<Mqtt5Client> clientManager) {
		return new Mqtt5MessageHandlerSpec(clientManager);
	}

	private Mqtt5() {
	}

}
