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
