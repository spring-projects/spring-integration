/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
