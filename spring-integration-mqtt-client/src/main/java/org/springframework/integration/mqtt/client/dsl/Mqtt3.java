/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;

import org.springframework.integration.mqtt.client.core.ClientManager;

/**
 * Factory methods for MQTT 3 Java DSL components.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public final class Mqtt3 {

	/**
	 * Create an inbound adapter with the provided topic filters.
	 * The client manager must be managed independently as a Spring bean.
	 * @param clientManager the client manager.
	 * @param topics the topic filters.
	 * @return the spec.
	 * @since 7.3
	 */
	public static Mqtt3MessageDrivenChannelAdapterSpec inboundAdapter(ClientManager<Mqtt3Client> clientManager,
			String... topics) {

		return new Mqtt3MessageDrivenChannelAdapterSpec(clientManager, topics);
	}

	/**
	 * Create an inbound adapter with the provided native subscriptions.
	 * The client manager must be managed independently as a Spring bean.
	 * @param clientManager the client manager.
	 * @param subscriptions the native subscriptions.
	 * @return the spec.
	 * @since 7.3
	 */
	public static Mqtt3MessageDrivenChannelAdapterSpec inboundAdapter(ClientManager<Mqtt3Client> clientManager,
			Mqtt3Subscription... subscriptions) {

		return new Mqtt3MessageDrivenChannelAdapterSpec(clientManager, subscriptions);
	}

	/**
	 * Create an outbound adapter.
	 * The client manager must be managed independently as a Spring bean.
	 * @param clientManager the client manager.
	 * @return the spec.
	 * @since 7.3
	 */
	public static Mqtt3MessageHandlerSpec outboundAdapter(ClientManager<Mqtt3Client> clientManager) {
		return new Mqtt3MessageHandlerSpec(clientManager);
	}

	private Mqtt3() {
	}

}
