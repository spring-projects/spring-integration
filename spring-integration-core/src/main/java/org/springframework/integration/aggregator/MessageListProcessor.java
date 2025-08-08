/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.util.Collection;

import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 *
 */
@FunctionalInterface
public interface MessageListProcessor {

	Object process(Collection<? extends Message<?>> messages);

}
