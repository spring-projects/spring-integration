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

package org.springframework.integration.channel;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;

/**
 * Interface for interceptors that are able to view and/or modify the
 * {@link Message Messages} being sent-to and/or received-from a
 * {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public interface ChannelInterceptor {

	Message<?> preSend(Message<?> message, MessageChannel channel);

	void postSend(Message<?> message, MessageChannel channel, boolean sent);

	boolean preReceive(MessageChannel channel);

	Message<?> postReceive(Message<?> message, MessageChannel channel);

}
