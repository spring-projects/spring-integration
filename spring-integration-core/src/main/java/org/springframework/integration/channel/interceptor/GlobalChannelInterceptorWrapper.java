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
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.core.MessageChannel;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GlobalChannelInterceptorWrapper implements ChannelInterceptor, Ordered{
	private ChannelInterceptor channelInterceptor;
	private String[] patterns;
	private int order;
	
    public GlobalChannelInterceptorWrapper(ChannelInterceptor channelInterceptor){
		this.channelInterceptor = channelInterceptor;
		// will set initial order for this interceptor wrapper to be the same as the 
		// underlying interceptor. Could be overridden with setOrder() method
		if (channelInterceptor instanceof Ordered){
			order = ((Ordered)channelInterceptor).getOrder();
		}
	}

	public int getOrder() {
		return order;
	}

	/**
	 * 
	 * @param order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public ChannelInterceptor getChannelInterceptor() {
		return channelInterceptor;
	}

	public String[] getPatterns() {
		return patterns;
	}

	public void setPatterns(String[] patterns) {
		this.patterns = patterns;
	}

	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return channelInterceptor.postReceive(message, channel);
	}

	public void postSend(Message<?> message, MessageChannel channel,
			boolean sent) {
		channelInterceptor.postSend(message, channel, sent);
	}

	public boolean preReceive(MessageChannel channel) {
		return channelInterceptor.preReceive(channel);
	}

	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		return channelInterceptor.preSend(message, channel);
	}
	
	public String toString(){
		return channelInterceptor.toString();
	}
}
