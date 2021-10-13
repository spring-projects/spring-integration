/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.jms.util.JmsAdapterUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
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
public class JmsMessageDrivenEndpoint extends MessageProducerSupport implements OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer listenerContainer;

	private final boolean externalContainer;

	private final ChannelPublishingJmsMessageListener listener;

	private String sessionAcknowledgeMode;

	private boolean shutdownContainerOnStop = true;

	private volatile boolean hasStopped;

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
	 * @param externalContainer true if the container is externally configured and should not have its ack mode
	 * coerced when no sessionAcknowledgeMode was supplied.
	 */
	private JmsMessageDrivenEndpoint(AbstractMessageListenerContainer listenerContainer,
			ChannelPublishingJmsMessageListener listener, boolean externalContainer) {

		Assert.notNull(listenerContainer, "listener container must not be null");
		Assert.notNull(listener, "listener must not be null");
		Assert.isNull(listenerContainer.getMessageListener(),
				"The listenerContainer provided to a JMS Inbound Endpoint " +
						"must not have a MessageListener configured since the endpoint " +
						"configures its own listener implementation.");
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
	public void setOutputChannel(MessageChannel outputChannel) {
		super.setOutputChannel(outputChannel);
		this.listener.setRequestChannel(outputChannel);
	}

	@Override
	public void setOutputChannelName(String outputChannelName) {
		super.setOutputChannelName(outputChannelName);
		this.listener.setRequestChannelName(outputChannelName);
	}

	@Override
	public void setErrorChannel(MessageChannel errorChannel) {
		super.setErrorChannel(errorChannel);
		this.listener.setErrorChannel(errorChannel);
	}

	@Override
	public void setErrorChannelName(String errorChannelName) {
		super.setErrorChannelName(errorChannelName);
		this.listener.setErrorChannelName(errorChannelName);
	}

	@Override
	public void setSendTimeout(long sendTimeout) {
		super.setSendTimeout(sendTimeout);
		this.listener.setRequestTimeout(sendTimeout);
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		super.setShouldTrack(shouldTrack);
		this.listener.setShouldTrack(shouldTrack);
	}

	/**
	 * Set to false to prevent listener container shutdown when the endpoint is stopped.
	 * Then, if so configured, any cached consumer(s) in the container will remain.
	 * Otherwise the shared connection and will be closed and the listener invokers shut
	 * down; this behavior is new starting with version 5.1. Default: true.
	 * @param shutdownContainerOnStop false to not shutdown.
	 * @since 5.1
	 */
	public void setShutdownContainerOnStop(boolean shutdownContainerOnStop) {
		this.shutdownContainerOnStop = shutdownContainerOnStop;
	}

	public ChannelPublishingJmsMessageListener getListener() {
		return this.listener;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);
		this.listener.setBeanFactory(applicationContext);
	}

	@Override
	public String getComponentType() {
		return "jms:message-driven-channel-adapter";
	}

	@Override
	public void afterSingletonsInstantiated() {
		// skip the output channel requirement assertion for when the listener is pre-built
	}

	@Override
	protected void onInit() {
		super.onInit();

		this.listener.afterPropertiesSet();
		if (!this.listenerContainer.isActive()) {
			this.listenerContainer.afterPropertiesSet();
		}
		String sessionAckMode = this.sessionAcknowledgeMode;
		if (sessionAckMode == null && !this.externalContainer
				&& DefaultMessageListenerContainer.class.isAssignableFrom(this.listenerContainer.getClass())) {
			sessionAckMode = JmsAdapterUtils.SESSION_TRANSACTED_STRING;
		}
		Integer acknowledgeMode = JmsAdapterUtils.parseAcknowledgeMode(sessionAckMode);
		if (acknowledgeMode != null) {
			if (JmsAdapterUtils.SESSION_TRANSACTED == acknowledgeMode) {
				this.listenerContainer.setSessionTransacted(true);
			}
			else {
				this.listenerContainer.setSessionAcknowledgeMode(acknowledgeMode);
			}
		}
		this.listener.setComponentName(getComponentName());
	}

	@Override
	protected void doStart() {
		this.listener.start();
		if (!this.listenerContainer.isRunning()) {
			if (this.hasStopped) {
				this.listenerContainer.initialize();
				this.hasStopped = false;
			}
			this.listenerContainer.start();
		}
	}

	@Override
	protected void doStop() {
		this.listenerContainer.stop();
		if (this.shutdownContainerOnStop) {
			this.hasStopped = true;
			this.listenerContainer.shutdown();
		}
		this.listener.stop();
	}

	@Override
	public void destroy() {
		super.destroy();
		this.listenerContainer.destroy();
	}

	@Override
	public int beforeShutdown() {
		stop();
		return 0;
	}

	@Override
	public int afterShutdown() {
		return 0;
	}

}
