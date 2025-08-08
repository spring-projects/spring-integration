/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * A {@link MessageSenderContext}-based {@link ObservationConvention} contract.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public interface MessageSenderObservationConvention extends ObservationConvention<MessageSenderContext> {

	@Override
	default String getName() {
		return "spring.integration.producer";
	}

	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof MessageSenderContext;
	}

	@Override
	default String getContextualName(MessageSenderContext context) {
		return context.getProducerName() + " send";
	}

}
