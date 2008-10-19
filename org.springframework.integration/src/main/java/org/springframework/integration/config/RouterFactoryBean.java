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

package org.springframework.integration.config;

import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Router.
 * 
 * @author Mark Fisher
 */
public class RouterFactoryBean extends AbstractConsumerFactoryBean {

	private volatile ChannelResolver channelResolver;

	private volatile MessageChannel defaultOutputChannel;


	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	@Override
	protected MessageConsumer createConsumer(Object targetObject, String targetMethodName) {
		Assert.notNull(targetObject, "target object must not be null");
		AbstractMessageRouter router = this.createRouter(targetObject, targetMethodName);
		if (this.defaultOutputChannel != null) {
			router.setDefaultOutputChannel(this.defaultOutputChannel);
		}
		return router;
	}

	private AbstractMessageRouter createRouter(Object targetObject, String targetMethodName) {
		if (targetObject instanceof AbstractMessageRouter) {
			Assert.isTrue(!StringUtils.hasText(targetMethodName),
					"target method should not be provided when the target " +
					"object is an implementation of AbstractMessageRouter");
			return (AbstractMessageRouter) targetObject;
		}
		MethodInvokingRouter router = (StringUtils.hasText(targetMethodName))
				? new MethodInvokingRouter(targetObject, targetMethodName)
				: new MethodInvokingRouter(targetObject);
		if (this.channelResolver != null) {
			router.setChannelResolver(this.channelResolver);
		}
		return router;
	}

}
