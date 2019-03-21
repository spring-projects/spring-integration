/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.integration.routingslip;

import org.springframework.messaging.Message;

/**
 * The {@code RoutingSlip} strategy to determine the next {@code replyChannel}.
 * <p>
 * This strategy is called repeatedly until null or an empty String is returned.
 *
 * @author Artem Bilan
 * @since 4.1
 * @see org.springframework.integration.handler.AbstractMessageProducingHandler
 */
@FunctionalInterface
public interface RoutingSlipRouteStrategy {

	/**
	 * Get the next path for this routing slip.
	 * @param requestMessage the request message.
	 * @param reply the reply - depending on context, this may be a user-level domain
	 * object, a {@link Message} or a {@code AbstractIntegrationMessageBuilder}.
	 * @return a channel name or another {@link RoutingSlipRouteStrategy}.
	 */
	Object getNextPath(Message<?> requestMessage, Object reply);

}
