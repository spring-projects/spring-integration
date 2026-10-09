/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.routingslip;

import org.jspecify.annotations.Nullable;

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
	@Nullable
	Object getNextPath(Message<?> requestMessage, Object reply);

}
