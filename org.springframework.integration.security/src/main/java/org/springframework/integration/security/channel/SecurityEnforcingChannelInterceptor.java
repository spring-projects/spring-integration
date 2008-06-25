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

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.message.Message;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.util.Assert;

/**
 * Delegates to the provided instance of {@link AccessDecisionManager} to
 * enforce the security on the send and receive calls on the {@ MessageChannel}.
 * 
 * @author Jonas Partner
 */
public class SecurityEnforcingChannelInterceptor extends ChannelInterceptorAdapter {

	private final AccessDecisionManager accessDecisionManger;

	private volatile ConfigAttributeDefinition sendSecurityAttributes;

	private volatile ConfigAttributeDefinition receiveSecurityAttributes;

	public SecurityEnforcingChannelInterceptor(AccessDecisionManager accessDecisionManager) {
		Assert.notNull(accessDecisionManager, "AccessDecisionManager must not be null");
		this.accessDecisionManger = accessDecisionManager;
	}

	public ConfigAttributeDefinition getSendSecurityAttributes() {
		return this.sendSecurityAttributes;
	}

	public void setSendSecurityAttributes(ConfigAttributeDefinition sendSecurityAttributes) {
		this.sendSecurityAttributes = sendSecurityAttributes;
	}

	public ConfigAttributeDefinition getReceiveSecurityAttributes() {
		return this.receiveSecurityAttributes;
	}

	public void setReceiveSecurityAttributes(ConfigAttributeDefinition receiveSecurityAttributes) {
		this.receiveSecurityAttributes = receiveSecurityAttributes;
	}

	@Override
	public boolean preSend(Message<?> message, MessageChannel channel) {
		this.checkSend(channel);
		return true;
	}

	@Override
	public boolean preReceive(MessageChannel channel) {
		this.checkReceive(channel);
		return super.preReceive(channel);
	}

	private void checkSend(MessageChannel channel) {
		this.checkPermission(channel, this.sendSecurityAttributes);
	}

	private void checkReceive(MessageChannel channel) {
		this.checkPermission(channel, this.receiveSecurityAttributes);
	}

	private void checkPermission(MessageChannel messageChannel, ConfigAttributeDefinition securityAttributes) {
		if (securityAttributes != null) {
			this.accessDecisionManger.decide(SecurityContextHolder.getContext().getAuthentication(), messageChannel,
					securityAttributes);
		}
	}

}
