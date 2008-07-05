/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.message.Message;

/**
 * a {@link ChannelInterceptor} which allows the application of a {@link MessageTransformer} on either send or receive to a channel
 * @author Jonas Partner
 * 
 */
public class MessageTransformingChannelInterceptor extends ChannelInterceptorAdapter {

	private boolean convertOnSend = true;

	private final MessageTransformer transfomer;

	public MessageTransformingChannelInterceptor(MessageTransformer transformer) {
		this.transfomer = transformer;
	}

	public boolean getConvertOnSend() {
		return convertOnSend;
	}

	public void setConvertOnSend(boolean convertOnSend) {
		this.convertOnSend = convertOnSend;
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		if(convertOnSend){
			transfomer.transform(message);
		}
	}

	@Override
	public void postReceive(Message<?> message, MessageChannel channel) {
		if(!convertOnSend){
			transfomer.transform(message);
		}
	}

}
