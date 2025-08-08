/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.common.KeyValues;

/**
 * A default {@link MessageReceiverObservationConvention} implementation.
 * Provides low cardinalities as a {@link IntegrationObservation.HandlerTags} values.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class DefaultMessageReceiverObservationConvention implements MessageReceiverObservationConvention {

	/**
	 * A shared singleton instance for {@link DefaultMessageReceiverObservationConvention}.
	 */
	public static final DefaultMessageReceiverObservationConvention INSTANCE =
			new DefaultMessageReceiverObservationConvention();

	@Override
	public KeyValues getLowCardinalityKeyValues(MessageReceiverContext context) {
		return KeyValues
				// See IntegrationObservation.HandlerTags.COMPONENT_NAME - to avoid class tangle
				.of("spring.integration.name", context.getHandlerName())
				// See IntegrationObservation.HandlerTags.COMPONENT_TYPE - to avoid class tangle
				.and("spring.integration.type", "handler");
	}

}
