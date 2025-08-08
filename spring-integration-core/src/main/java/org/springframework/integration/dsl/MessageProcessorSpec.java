/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.handler.MessageProcessor;

/**
 * The {@link IntegrationComponentSpec} specific base class
 * for {@link MessageProcessor}s.
 *
 * @param <S> the target {@link MessageProcessorSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageProcessorSpec<S extends MessageProcessorSpec<S>>
		extends IntegrationComponentSpec<S, MessageProcessor<?>> {

}
