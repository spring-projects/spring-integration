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

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.aop.support.AopUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.ThreadStatePropagationChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The {@link ExecutorChannelInterceptor} implementation responsible for
 * the {@link SecurityContext} propagation from one message flow's thread to another
 * through the {@link MessageChannel}s involved in the flow.
 * <p>
 *
 * @author Artem Bilan
 * @see ThreadStatePropagationChannelInterceptor
 * @since 4.2
 */
public class SecurityContextPropagationChannelInterceptor
		extends ThreadStatePropagationChannelInterceptor<Authentication> {

	static final ThreadLocal<Deque<SecurityContext>> ORIGINAL_CONTEXT = new ThreadLocal<Deque<SecurityContext>>();

	private final SecurityContextCleanupChannelInterceptor securityContextCleanupChannelInterceptor;

	public SecurityContextPropagationChannelInterceptor() {
		this(false);
	}

	public SecurityContextPropagationChannelInterceptor(boolean cleanupContext) {
		this.securityContextCleanupChannelInterceptor =
				cleanupContext
						? new SecurityContextCleanupChannelInterceptor()
						: null;
	}

	@Override
	public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
		if (this.securityContextCleanupChannelInterceptor != null) {
			this.securityContextCleanupChannelInterceptor.afterSendCompletion(message, channel, sent, ex);
		}
	}

	@Override
	public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
		if (this.securityContextCleanupChannelInterceptor != null) {
			this.securityContextCleanupChannelInterceptor.afterMessageHandled(message, channel, handler, ex);
		}
	}

	@Override
	protected Authentication obtainPropagatingContext(Message<?> message, MessageChannel channel) {
		if (!DirectChannel.class.isAssignableFrom(AopUtils.getTargetClass(channel))) {
			return SecurityContextHolder.getContext().getAuthentication();
		}
		return null;
	}

	@Override
	protected void populatePropagatedContext(Authentication authentication, Message<?> message,
											 MessageChannel channel) {
		if (authentication != null) {
			SecurityContext currentContext = SecurityContextHolder.getContext();

			Deque<SecurityContext> contextStack = ORIGINAL_CONTEXT.get();
			if (contextStack == null) {
				contextStack = new ArrayDeque<SecurityContext>();
				ORIGINAL_CONTEXT.set(contextStack);
			}
			contextStack.push(currentContext);

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
		}
	}

}
