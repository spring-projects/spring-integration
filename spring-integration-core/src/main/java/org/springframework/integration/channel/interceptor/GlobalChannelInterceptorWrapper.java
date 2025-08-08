/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.interceptor;

import java.util.Arrays;

import org.springframework.core.Ordered;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class GlobalChannelInterceptorWrapper implements Ordered {

	private final ChannelInterceptor channelInterceptor;

	private volatile String[] patterns = {"*"}; // default

	private volatile int order = 0;

	public GlobalChannelInterceptorWrapper(ChannelInterceptor channelInterceptor) {
		Assert.notNull(channelInterceptor, "channelInterceptor must not be null");
		this.channelInterceptor = channelInterceptor;
		// will set initial order for this interceptor wrapper to be the same as
		// the underlying interceptor. Could be overridden with setOrder() method
		if (channelInterceptor instanceof Ordered) {
			this.order = ((Ordered) channelInterceptor).getOrder();
		}
	}

	public ChannelInterceptor getChannelInterceptor() {
		return this.channelInterceptor;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public final int getOrder() {
		return this.order;
	}

	public void setPatterns(String[] patterns) {
		this.patterns = Arrays.copyOf(patterns, patterns.length);
	}

	public String[] getPatterns() {
		return this.patterns; // NOSONAR - expose internals
	}

	@Override
	public String toString() {
		return this.channelInterceptor.toString();
	}

}
