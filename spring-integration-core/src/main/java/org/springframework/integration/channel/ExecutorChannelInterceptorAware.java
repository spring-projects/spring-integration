/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.channel;

import org.springframework.messaging.support.InterceptableChannel;

/**
 * The {@link InterceptableChannel} extension for the cases when
 * the {@link org.springframework.messaging.support.ExecutorChannelInterceptor}s
 * may have reason (e.g. {@link ExecutorChannel} or {@link QueueChannel})
 * and the implementors require to know if they should make the
 * {@link org.springframework.messaging.support.ExecutorChannelInterceptor}
 * or not.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 */
public interface ExecutorChannelInterceptorAware extends InterceptableChannel {

	boolean hasExecutorInterceptors();

}
