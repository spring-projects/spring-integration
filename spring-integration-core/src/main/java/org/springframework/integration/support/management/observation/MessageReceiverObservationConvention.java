/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
