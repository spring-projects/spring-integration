/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.support;

import java.util.ArrayList;

import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder;
import io.micrometer.observation.transport.SenderContext;

/**
 * A sender context that propagates tracing headers through MQTT 5 user properties.
 * The original publish is preserved; the carrier builds a new publish with the injected headers.
 *
 * @author Hyunah Oh
 *
 * @since 7.3
 */
public class Mqtt5PublishSenderContext extends SenderContext<Mqtt5PublishBuilder.Complete> {

	private final Mqtt5PublishBuilder.Complete carrier;

	private final String producerName;

	public Mqtt5PublishSenderContext(Mqtt5Publish publish, String producerName) {
		super((carrier, key, value) -> {
			if (carrier != null) {
				var properties = new ArrayList<Mqtt5UserProperty>(carrier.build().getUserProperties().asList());
				properties.removeIf(property -> property.getName().toString().equals(key));
				properties.add(Mqtt5UserProperty.of(key, value));
				carrier.userProperties(Mqtt5UserProperties.of(properties));
			}
		});
		this.carrier = publish.extend();
		this.producerName = producerName;
	}

	@Override
	public Mqtt5PublishBuilder.Complete getCarrier() {
		return this.carrier;
	}

	public String getProducerName() {
		return this.producerName;
	}

}
