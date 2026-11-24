/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dsl;

import org.springframework.messaging.MessageHandler;

/**
 * A {@link ConsumerEndpointSpec} for a {@link MessageHandler} implementations.
 *
 * @param <H> the {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class GenericEndpointSpec<H extends MessageHandler>
		extends ConsumerEndpointSpec<GenericEndpointSpec<H>, H> {

	protected GenericEndpointSpec(H messageHandler) {
		super(messageHandler);
	}

}
