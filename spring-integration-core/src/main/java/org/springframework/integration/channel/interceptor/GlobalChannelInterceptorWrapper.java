/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
