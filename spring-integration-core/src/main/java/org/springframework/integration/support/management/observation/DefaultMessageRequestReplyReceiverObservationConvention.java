/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.common.KeyValues;

/**
 * A default {@link MessageRequestReplyReceiverObservationConvention} implementation.
 * Provides low cardinalities as a {@link IntegrationObservation.GatewayTags} values.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class DefaultMessageRequestReplyReceiverObservationConvention
		implements MessageRequestReplyReceiverObservationConvention {

	/**
	 * A shared singleton instance for {@link DefaultMessageRequestReplyReceiverObservationConvention}.
	 */
	public static final DefaultMessageRequestReplyReceiverObservationConvention INSTANCE =
			new DefaultMessageRequestReplyReceiverObservationConvention();

	@Override
	public KeyValues getLowCardinalityKeyValues(MessageRequestReplyReceiverContext context) {
		return KeyValues
				// See IntegrationObservation.GatewayTags.COMPONENT_NAME - to avoid class tangle
				.of("spring.integration.name", context.getGatewayName())
				// See IntegrationObservation.GatewayTags.COMPONENT_TYPE - to avoid class tangle
				.and("spring.integration.type", "gateway")
				// See IntegrationObservation.GatewayTags.OUTCOME - to avoid class tangle
				.and("spring.integration.outcome", context.getError() != null ? "INTERNAL_ERROR" : "SUCCESS");
	}

}
