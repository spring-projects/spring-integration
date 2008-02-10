/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;

/**
 * A {@link ChannelInterceptor} with no-op method implementations so that
 * subclasses do not have to implement all of the interface's methods.
 * 
 * @author Mark Fisher
 */
public class ChannelInterceptorAdapter implements ChannelInterceptor {

	public boolean preSend(Message<?> message, MessageChannel channel) {
		return true;
	}

	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
	}

	public boolean preReceive(MessageChannel channel) {
		return true;
	}

	public void postReceive(Message<?> message, MessageChannel channel) {
	}

}
