/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
