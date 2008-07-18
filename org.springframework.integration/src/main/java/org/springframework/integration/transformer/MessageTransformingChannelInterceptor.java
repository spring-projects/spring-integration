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
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;

/**
 * A {@link ChannelInterceptor} which invokes a {@link MessageHandler}
 * when either sending-to or receiving-from a channel.
 * 
 * @author Jonas Partner
 */
public class MessageTransformingChannelInterceptor extends ChannelInterceptorAdapter {

	private final MessageHandler transfomer;

	private volatile boolean transformOnSend = true;


	public MessageTransformingChannelInterceptor(MessageHandler transformer) {
		this.transfomer = transformer;
	}


	public boolean getTransformOnSend() {
		return this.transformOnSend;
	}

	public void setTransformOnSend(boolean transformOnSend) {
		this.transformOnSend = transformOnSend;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (this.transformOnSend) {
			message = this.transfomer.handle(message);
		}
		return message;
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		if (!this.transformOnSend) {
			message = this.transfomer.handle(message);
		}
		return message;
	}

}
