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

package org.springframework.integration.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Interceptor that publishes a target method's return value to a channel.
 * 
 * @author Mark Fisher
 */
public class MessagePublishingInterceptor implements MethodInterceptor {

	public static enum PayloadType { RETURN_VALUE, ARGUMENTS, EXCEPTION };


	protected final Log logger = LogFactory.getLog(getClass());

	private volatile MessageChannel outputChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

	private volatile PayloadType payloadType = PayloadType.RETURN_VALUE;


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setPayloadType(PayloadType payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null");
		this.payloadType = payloadType;
	}

	/**
	 * Invoke the target method and publish its return value.
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable {
		PayloadType payloadType = this.determinePayloadType(invocation);
		if (payloadType.equals(PayloadType.ARGUMENTS)) {
			this.sendMessage(invocation.getArguments(), invocation);
		}
		Object retval = null;
		Throwable throwable = null;
		try {
			retval = invocation.proceed();
			return retval;
		}
		catch (Throwable t) {
			throwable = t;
			throw t;
		}
		finally {
			if (payloadType.equals(PayloadType.RETURN_VALUE)) {
				this.sendMessage(retval, invocation);
			}
			else if (payloadType.equals(PayloadType.EXCEPTION)) {
				this.sendMessage(throwable, invocation);
			}
		}
	}

	private void sendMessage(Object payload, MethodInvocation invocation) {
		if (payload != null) {
			Message<?> message = (payload instanceof Message)
					? (Message<?>) payload
					: MessageBuilder.withPayload(payload).build();
			this.channelTemplate.send(message, this.resolveChannel(invocation));
		}
	}

	/**
	 * Subclasses may override this method to provide custom behavior.
	 */
	protected MessageChannel resolveChannel(MethodInvocation invocation) {
		return this.outputChannel;
	}

	protected PayloadType determinePayloadType(MethodInvocation invocation) {
		return this.payloadType;
	}

}
