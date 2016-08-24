/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.jms.util.JmsAdapterUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.Assert;

/**
 * A message-driven endpoint that receive JMS messages, converts them into
 * Spring Integration Messages, and then sends the result to a channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsMessageDrivenEndpoint extends AbstractEndpoint implements DisposableBean, OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer listenerContainer;

	private final boolean externalContainer;

	private final ChannelPublishingJmsMessageListener listener;

	private volatile String sessionAcknowledgeMode;

	/**
	 * Construct an instance with an externally configured container.
	 * @param listenerContainer the container.
	 * @param listener the listener.
	 */
	public JmsMessageDrivenEndpoint(AbstractMessageListenerContainer listenerContainer,
			ChannelPublishingJmsMessageListener listener) {
		this(listenerContainer, listener, true);
	}

	/**
	 * Construct an instance with an argument indicating whether the container's ack mode should
	 * be overridden with {@link #setSessionAcknowledgeMode(String) sessionAcknowledgeMode}, default
	 * 'transacted'.
	 * @param listenerContainer the container.
	 * @param listener the listener.
	 * @param externalContainer true if the container is externally configured and should not have its ackmode
	 * coerced when no sessionAcknowledgeMode was supplied.
	 */
	private JmsMessageDrivenEndpoint(AbstractMessageListenerContainer listenerContainer,
	                                 ChannelPublishingJmsMessageListener listener, boolean externalContainer) {
		Assert.notNull(listenerContainer, "listener container must not be null");
		Assert.notNull(listener, "listener must not be null");
		if (logger.isWarnEnabled() && listenerContainer.getMessageListener() != null) {
			logger.warn("The provided listener container already has a MessageListener implementation, " +
					"but it will be overridden by the provided ChannelPublishingJmsMessageListener.");
		}
		listenerContainer.setMessageListener(listener);
		this.listener = listener;
		this.listenerContainer = listenerContainer;
		this.listenerContainer.setAutoStartup(false);
		setPhase(Integer.MAX_VALUE / 2);
		this.externalContainer = externalContainer;
	}

	/**
	 * Set the session acknowledge mode on the listener container. It will override the
	 * container setting even if an external container is provided. Defaults to null
	 * (won't change container) if an external container is provided or `transacted` when
	 * the framework creates an implicit {@link DefaultMessageListenerContainer}.
	 * @param sessionAcknowledgeMode the acknowledge mode.
	 */
	public void setSessionAcknowledgeMode(String sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
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
		String sessionAcknowledgeMode = this.sessionAcknowledgeMode;
		if (sessionAcknowledgeMode == null && !this.externalContainer
				&& DefaultMessageListenerContainer.class.isAssignableFrom(this.listenerContainer.getClass())) {
			sessionAcknowledgeMode = JmsAdapterUtils.SESSION_TRANSACTED_STRING;
		}
		Integer acknowledgeMode = JmsAdapterUtils.parseAcknowledgeMode(sessionAcknowledgeMode);
		if (acknowledgeMode != null) {
			if (acknowledgeMode.intValue() == JmsAdapterUtils.SESSION_TRANSACTED) {
				this.listenerContainer.setSessionTransacted(true);
			}
			else {
				this.listenerContainer.setSessionAcknowledgeMode(acknowledgeMode);
			}
		}
		this.listener.setComponentName(this.getComponentName());
	}

	@Override
	protected void doStart() {
		this.listener.start();
		if (!this.listenerContainer.isRunning()) {
			this.listenerContainer.start();
		}
	}

	@Override
	protected void doStop() {
		this.listenerContainer.stop();
		this.listener.stop();
	}

	@Override
	public void destroy() throws Exception {
		if (this.isRunning()) {
			this.stop();
		}
		this.listenerContainer.destroy();
	}


	@Override
	public int beforeShutdown() {
		this.stop();
		return 0;
	}


	@Override
	public int afterShutdown() {
		return 0;
	}

}
