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

package org.springframework.integration.security.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.message.Message;
import org.springframework.integration.security.SecurityContextUtils;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * Propagates the {@link SecurityContext} associated with the current thread
 * (if any) by adding it to the header of sent messages.
 * 
 * @author Jonas Partner
 */
public class SecurityContextPropagatingChannelInterceptor extends ChannelInterceptorAdapter {

	private final Log logger = LogFactory.getLog(this.getClass());


	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		this.setSecurityContextAttribute(message);
		return message;
	}

	protected void setSecurityContextAttribute(Message<?> message) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (securityContext.getAuthentication() != null) {
			SecurityContextUtils.setSecurityContextHeader(securityContext, message);
		}
		else if (logger.isInfoEnabled()) {
			logger.info("No security context found");
		}
	}

}
