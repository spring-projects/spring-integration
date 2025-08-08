/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.channel.interceptor;

import org.springframework.messaging.support.InterceptableChannel;

/**
 * {@link org.springframework.messaging.support.ChannelInterceptor}s implementing this
 * interface can veto global interception of a particular channel.
 * Could be used, for example, when an interceptor itself writes to an output channel
 * (which should not be intercepted with this interceptor).
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public interface VetoCapableInterceptor {

	/**
	 * @param beanName The channel name.
	 * @param channel The channel that is about to be intercepted.
	 * @return false if the intercept wishes to veto the interception.
	 */
	boolean shouldIntercept(String beanName, InterceptableChannel channel);

}
