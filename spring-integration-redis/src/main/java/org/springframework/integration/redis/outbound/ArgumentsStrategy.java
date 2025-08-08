/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.outbound;

import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@FunctionalInterface
public interface ArgumentsStrategy {

	Object[] resolve(String command, Message<?> message);

}
