/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.support;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Default observation convention for MQTT 5 publishes.
 *
 * @author Hyunah Oh
 *
 * @since 7.2
 */
public class DefaultMqtt5PublishObservationConvention implements ObservationConvention<Mqtt5PublishSenderContext> {

	/**
	 * A shared instance of this convention.
	 */
	public static final DefaultMqtt5PublishObservationConvention INSTANCE =
			new DefaultMqtt5PublishObservationConvention();

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof Mqtt5PublishSenderContext;
	}

	@Override
	public String getName() {
		return "spring.integration.mqtt.publish";
	}

	@Override
	public String getContextualName(Mqtt5PublishSenderContext context) {
		return context.getProducerName() + " publish";
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(Mqtt5PublishSenderContext context) {
		return KeyValues.of("spring.integration.name", context.getProducerName())
				.and("spring.integration.type", "producer");
	}

}
