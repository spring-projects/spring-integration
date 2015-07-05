/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.security.context;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.core.context.SecurityContext;

/**
 * The {@link ExecutorChannelInterceptor} implementation responsible for
 * the {@link SecurityContext} cleanup.
 * <p>
 * Designed to work in tandem with {@link SecurityContextPropagationChannelInterceptor}.
 * However the {@link MessageChannel} can be marked for {@link SecurityContext} cleanup
 * with this interceptor, meanwhile not for {@link SecurityContext} propagation
 * with {@link SecurityContextPropagationChannelInterceptor}.
 *
 * @author Artem Bilan
 * @since 4.2
 *
 * @see SecurityContextPropagationChannelInterceptor
 * @see SecurityContextCleanupAdvice
 */
public class SecurityContextCleanupChannelInterceptor extends ChannelInterceptorAdapter
		implements ExecutorChannelInterceptor {

	@Override
	public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
		return message;
	}

	@Override
	public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
		SecurityContextCleanupAdvice.cleanup();
	}

	@Override
	public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
		SecurityContextCleanupAdvice.cleanup();
	}

}

