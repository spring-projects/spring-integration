/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import org.springframework.messaging.Message;
import org.springframework.retry.RetryState;

/**
 * Strategy interface for generating a {@link RetryState} instance
 * based on a message.
 * @author Gary Russell
 * @since 2.2
 *
 */
@FunctionalInterface
public interface RetryStateGenerator {

	RetryState determineRetryState(Message<?> message);

}
