/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel.interceptor;

import org.springframework.core.Ordered;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GlobalChannelInterceptorWrapper implements ChannelInterceptor, Ordered {

	private final ChannelInterceptor channelInterceptor;

	private volatile String[] patterns;

	private volatile int order;


	public GlobalChannelInterceptorWrapper(ChannelInterceptor channelInterceptor) {
		Assert.notNull(channelInterceptor, "channelInterceptor must not be null");
		this.channelInterceptor = channelInterceptor;
		// will set initial order for this interceptor wrapper to be the same as
		// the underlying interceptor. Could be overridden with setOrder() method
		if (channelInterceptor instanceof Ordered) {
			order = ((Ordered) channelInterceptor).getOrder();
		}
	}


	public ChannelInterceptor getChannelInterceptor() {
		return this.channelInterceptor;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	public void setPatterns(String[] patterns) {
		this.patterns = patterns;
	}

	public String[] getPatterns() {
		return this.patterns;
	}

	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		return this.channelInterceptor.preSend(message, channel);
	}	

	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		this.channelInterceptor.postSend(message, channel, sent);
	}

	public boolean preReceive(MessageChannel channel) {
		return this.channelInterceptor.preReceive(channel);
	}

	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return this.channelInterceptor.postReceive(message, channel);
	}

	public String toString() {
		return this.channelInterceptor.toString();
	}

}
