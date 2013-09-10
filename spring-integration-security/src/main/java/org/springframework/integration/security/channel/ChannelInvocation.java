/*
 * Copyright 2002-2010 the original author or authors.
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

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Secured object for {@link ChannelSecurityInterceptor}. Maintains a reference
 * to the original {@link MethodInvocation} instance and provides convenient
 * access to the secured {@link MessageChannel}. If the intercepted invocation
 * is a <em>send</em> operation, the {@link Message} is also available.
 * 
 * @author Mark Fisher
 */
public class ChannelInvocation {

	private final MessageChannel channel;

	private final Message<?> message;

	private final MethodInvocation methodInvocation;


	/**
	 * @param methodInvocation the intercepted MethodInvocation instance
	 */
	public ChannelInvocation(MethodInvocation methodInvocation) {
		Assert.notNull(methodInvocation, "MethodInvocation must not be null");
		Assert.isAssignable(MessageChannel.class, methodInvocation.getThis().getClass(),
				"MethodInvocation must be on a MessageChannel");
		this.channel = (MessageChannel) methodInvocation.getThis();
		if (methodInvocation.getMethod().getName().equals("send")) {
			if (methodInvocation.getArguments().length < 1 || !(methodInvocation.getArguments()[0] instanceof Message<?>)) {
				throw new IllegalStateException("expected a Message as the first parameter of the channel's send method");
			}
			this.message = (Message<?>) methodInvocation.getArguments()[0];
		}
		else {
			this.message = null;
		}
		this.methodInvocation = methodInvocation;
	}


	public MessageChannel getChannel() {
		return this.channel;
	}

	public Message<?> getMessage() {
		return this.message;
	}

	public MethodInvocation getMethodInvocation() {
		return this.methodInvocation;
	}

	public boolean isSend() {
		return "send".equals(this.methodInvocation.getMethod().getName());
	}

	public boolean isReceive() {
		return "receive".equals(this.methodInvocation.getMethod().getName());
	}

}
