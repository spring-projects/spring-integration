/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.dispatcher;

import org.springframework.messaging.support.MessageHandlingRunnable;

/**
 * The strategy to decorate {@link MessageHandlingRunnable} tasks
 * to be used with the {@link java.util.concurrent.Executor}.
 *
 * @author Artem Bilan
 * @since 4.2
 * @see UnicastingDispatcher
 * @see BroadcastingDispatcher
 */
@FunctionalInterface
public interface MessageHandlingTaskDecorator {

	Runnable decorate(MessageHandlingRunnable task);

}
