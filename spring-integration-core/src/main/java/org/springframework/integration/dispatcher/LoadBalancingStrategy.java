/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Strategy for determining the iteration order of a MessageHandler list.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
@FunctionalInterface
public interface LoadBalancingStrategy {

	Iterator<MessageHandler> getHandlerIterator(Message<?> message, Collection<MessageHandler> handlers);

}
