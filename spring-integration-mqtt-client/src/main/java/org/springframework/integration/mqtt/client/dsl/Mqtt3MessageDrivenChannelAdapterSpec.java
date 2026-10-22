/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;

import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.inbound.Mqtt3MessageDrivenChannelAdapter;

/**
 * A spec for an MQTT 3 message-driven channel adapter.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public class Mqtt3MessageDrivenChannelAdapterSpec
		extends MqttMessageDrivenChannelAdapterSpec<Mqtt3MessageDrivenChannelAdapterSpec, Mqtt3MessageDrivenChannelAdapter> {

	protected Mqtt3MessageDrivenChannelAdapterSpec(ClientManager<Mqtt3Client> clientManager,
			String... topics) {

		super(new Mqtt3MessageDrivenChannelAdapter(clientManager, topics));
	}

	protected Mqtt3MessageDrivenChannelAdapterSpec(ClientManager<Mqtt3Client> clientManager,
			Mqtt3Subscription... subscriptions) {

		super(new Mqtt3MessageDrivenChannelAdapter(clientManager, subscriptions));
	}

}
