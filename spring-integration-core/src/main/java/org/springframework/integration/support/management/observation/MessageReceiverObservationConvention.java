/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * A {@link MessageReceiverContext}-based {@link ObservationConvention} contract.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public interface MessageReceiverObservationConvention extends ObservationConvention<MessageReceiverContext> {

	@Override
	default String getName() {
		return "spring.integration.handler";
	}

	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof MessageReceiverContext;
	}

	@Override
	default String getContextualName(MessageReceiverContext context) {
		return context.getHandlerName() + " receive";
	}

}
