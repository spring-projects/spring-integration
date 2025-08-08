/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.support;

import org.springframework.messaging.Message;

/**
 * The {@link Message} decoration contract.
 * An implementation may decide to return any {@link Message} instance
 * and even a different {@link Message} implementation. Usually used to
 * wrap a message in another.
 *
 * @author Artem Bilan
 * @since 4.2.9
 */
@FunctionalInterface
public interface MessageDecorator {

	Message<?> decorateMessage(Message<?> message);

}
