/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
