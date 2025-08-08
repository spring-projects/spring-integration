/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.messaging.MessageHandler;

/**
 * An {@link IntegrationComponentSpec} for {@link MessageHandler}s.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageHandlerSpec<S extends MessageHandlerSpec<S, H>, H extends MessageHandler>
		extends IntegrationComponentSpec<S, H> {

}
