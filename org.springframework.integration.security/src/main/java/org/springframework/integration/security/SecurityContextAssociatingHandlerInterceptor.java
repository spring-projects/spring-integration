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

package org.springframework.integration.security;

import org.springframework.integration.handler.InterceptingMessageHandler;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * Associates the {@link SecurityContext} propagated in the message header with
 * the thread executing the handle call to a {@link MessageHandler}.
 * 
 * @author Jonas Partner
 */
public class SecurityContextAssociatingHandlerInterceptor extends InterceptingMessageHandler {

	/**
	 * One time only set the strategy to be stack based to allow use of direct
	 * channels where push and pop is required rather than set and clear
	 */
	static {
		SecurityContextHolder.setStrategyName(StackBasedSecurityContextHolderStrategy.class.getName());
	}

	public SecurityContextAssociatingHandlerInterceptor(MessageHandler target) {
		super(target);
	}

	@Override
	public Message<?> handle(Message<?> message, MessageHandler target) {
		if (message.getHeader().getAttributeNames().contains(SecurityContextUtils.SECURITY_CONTEXT_HEADER_ATTRIBUTE)) {
			return handleInSecurityContext(message, target);
		}
		return target.handle(message);
	}

	private Message<?> handleInSecurityContext(Message<?> message, MessageHandler target) {
		SecurityContext context = SecurityContextUtils.getSecurityContextFromHeader(message);
		SecurityContextHolder.setContext(context);
		try {
			return target.handle(message);
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

}
