/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.channel;

import org.reactivestreams.Publisher;

import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public interface ReactiveStreamsSubscribableChannel extends IntegrationPattern {

	void subscribeTo(Publisher<? extends Message<?>> publisher);

	@Override
	default IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.reactive_channel;
	}

}
