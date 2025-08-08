/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.common.KeyValues;

/**
 * A default {@link MessageSenderObservationConvention} implementation.
 * Provides low cardinalities as a {@link IntegrationObservation.ProducerTags} values.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class DefaultMessageSenderObservationConvention implements MessageSenderObservationConvention {

	/**
	 * A shared singleton instance for {@link DefaultMessageSenderObservationConvention}.
	 */
	public static final DefaultMessageSenderObservationConvention INSTANCE =
			new DefaultMessageSenderObservationConvention();

	@Override
	public KeyValues getLowCardinalityKeyValues(MessageSenderContext context) {
		return KeyValues
				// See IntegrationObservation.ProducerTags.COMPONENT_NAME - to avoid class tangle
				.of("spring.integration.name", context.getProducerName())
				// See IntegrationObservation.ProducerTags.COMPONENT_TYPE - to avoid class tangle
				.and("spring.integration.type", "producer");
	}

}
