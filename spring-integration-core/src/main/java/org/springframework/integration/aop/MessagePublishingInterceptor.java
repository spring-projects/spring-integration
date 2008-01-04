/*
 * Copyright 2002-2007 the original author or authors.
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
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * Interceptor that publishes a target method's return value to a channel.
 * 
 * @author Mark Fisher
 */
public class MessagePublishingInterceptor implements MethodInterceptor {

	protected Log logger = LogFactory.getLog(getClass());

	private MessageMapper mapper = new SimplePayloadMessageMapper();

	private MessageChannel defaultChannel;


	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	/**
	 * Specify the {@link MessageMapper} to use when creating a message from the
	 * return value Object. The default is a {@link SimplePayloadMessageMapper}.
	 * 
	 * @param mapper the mapper to use
	 */
	public void setMessageMapper(MessageMapper mapper) {
		Assert.notNull(mapper, "mapper must not be null");
		this.mapper = mapper;
	}

	/**
	 * Invoke the target method and publish its return value.
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object retval = invocation.proceed();
		if (retval != null) {
			MessageChannel channel = this.resolveChannel(invocation);
			if (channel == null) {
				if (logger.isWarnEnabled()) {
					logger.warn("unable to resolve channel for intercepted method '" +
							invocation.getMethod().getName() + "'");
				}
			}
			else {
				channel.send(mapper.toMessage(retval));
			}
		}
		return retval;
	}

	/**
	 * Subclasses may override this method to provide custom behavior.
	 */
	protected MessageChannel resolveChannel(MethodInvocation invocation) {
		return this.defaultChannel;
	}

}
