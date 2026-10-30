/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.outbound.Mqtt3MessageHandler;

/**
 * A spec for an MQTT 3 outbound message handler.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public class Mqtt3MessageHandlerSpec extends MqttMessageHandlerSpec<Mqtt3MessageHandlerSpec, Mqtt3MessageHandler> {

	protected Mqtt3MessageHandlerSpec(ClientManager<Mqtt3Client> clientManager) {
		super(new Mqtt3MessageHandler(clientManager));
	}

}
