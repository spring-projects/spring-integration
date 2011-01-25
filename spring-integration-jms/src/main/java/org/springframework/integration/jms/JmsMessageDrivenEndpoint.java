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

package org.springframework.integration.jms;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.util.Assert;

/**
 * A message-driven endpoint that receive JMS messages, converts them into
 * Spring Integration Messages, and then sends the result to a channel.
 * 
 * @author Mark Fisher
 */
public class JmsMessageDrivenEndpoint extends AbstractEndpoint implements DisposableBean {

	private final AbstractMessageListenerContainer listenerContainer;

	private final ChannelPublishingJmsMessageListener listener;


	public JmsMessageDrivenEndpoint(AbstractMessageListenerContainer listenerContainer, ChannelPublishingJmsMessageListener listener) {
		Assert.notNull(listenerContainer, "listener container must not be null");
		Assert.notNull(listener, "listener must not be null");
		if (logger.isWarnEnabled() && listenerContainer.getMessageListener() != null) {
			logger.warn("The provided listener container already has a MessageListener implementation, " +
					"but it will be overridden by the provided ChannelPublishingJmsMessageListener.");
		}
		listenerContainer.setMessageListener(listener);
		this.listener = listener;
		this.listenerContainer = listenerContainer;
	}


	@Override
	public String getComponentType() {
		return "jms:message-driven-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		this.listener.afterPropertiesSet();
		if (!this.listenerContainer.isActive()) {
			this.listenerContainer.afterPropertiesSet();
		}
		listener.setComponentName(this.getComponentName());
	}

	protected void doStart() {
		if (!this.listenerContainer.isRunning()) {
			this.listenerContainer.start();
		}
	}

	protected void doStop() {
		this.listenerContainer.stop();
	}

	public void destroy() throws Exception {
		if (this.isRunning()) {
			this.stop();
		}
		this.listenerContainer.destroy();
	}

}
