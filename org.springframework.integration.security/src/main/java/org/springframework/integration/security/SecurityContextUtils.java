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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.security.context.SecurityContext;

/**
 * @author Jonas Partner
 * 
 */
public class SecurityContextUtils {

	public static final String SECURITY_CONTEXT_HEADER_ATTRIBUTE = "SPRING_SECURITY_CONTEXT";

	public static SecurityContext getSecurityContextFromHeader(Message<?> message) {
		return (SecurityContext) message.getHeaders().get(SECURITY_CONTEXT_HEADER_ATTRIBUTE);
	}

	public static Message<?> setSecurityContextHeader(SecurityContext sctx, Message<?> message) {
		return MessageBuilder.fromMessage(message)
				.setHeader(SECURITY_CONTEXT_HEADER_ATTRIBUTE, sctx).build();
	}

}
