/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.security.channel;

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
 * In addition this interceptor cleans up (restores) the {@link SecurityContext}
 * in the containers Threads for channels like
 * {@link org.springframework.integration.channel.ExecutorChannel}
 * and {@link org.springframework.integration.channel.QueueChannel}.
 * @author Artem Bilan
 * @see ThreadStatePropagationChannelInterceptor
 * @since 4.2
 */
public class SecurityContextPropagationChannelInterceptor
		extends ThreadStatePropagationChannelInterceptor<Authentication> {

	private final static SecurityContext EMPTY_CONTEXT = SecurityContextHolder.createEmptyContext();

	private final static ThreadLocal<SecurityContext> ORIGINAL_CONTEXT = new ThreadLocal<SecurityContext>();

	@Override
	public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
		cleanup();
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

			ORIGINAL_CONTEXT.set(currentContext);

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
		}
	}

	private void cleanup() {
		SecurityContext originalContext = ORIGINAL_CONTEXT.get();
		try {
			if (originalContext == null || EMPTY_CONTEXT.equals(originalContext)) {
				SecurityContextHolder.clearContext();
				ORIGINAL_CONTEXT.remove();
			}
			else {
				SecurityContextHolder.setContext(originalContext);
			}
		}
		catch (Throwable t) { //NOSONAR
			SecurityContextHolder.clearContext();
		}
	}

}
