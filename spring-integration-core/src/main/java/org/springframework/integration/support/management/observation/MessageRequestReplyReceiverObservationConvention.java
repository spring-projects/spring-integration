/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * A {@link MessageRequestReplyReceiverContext}-based {@link ObservationConvention} contract.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public interface MessageRequestReplyReceiverObservationConvention
		extends ObservationConvention<MessageRequestReplyReceiverContext> {

	@Override
	default String getName() {
		return "spring.integration.gateway";
	}

	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof MessageRequestReplyReceiverContext;
	}

	@Override
	default String getContextualName(MessageRequestReplyReceiverContext context) {
		return context.getGatewayName() + " process";
	}

}
