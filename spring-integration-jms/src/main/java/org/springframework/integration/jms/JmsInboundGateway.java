/*
 * Copyright 2016-2020 the original author or authors.
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
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.MessageChannel;

/**
 * A wrapper around the {@link JmsMessageDrivenEndpoint} implementing
 * {@link MessagingGatewaySupport}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class JmsInboundGateway extends MessagingGatewaySupport implements OrderlyShutdownCapable {

	private final JmsMessageDrivenEndpoint endpoint;

	public JmsInboundGateway(AbstractMessageListenerContainer listenerContainer,
			ChannelPublishingJmsMessageListener listener) {

		this.endpoint = new JmsMessageDrivenEndpoint(listenerContainer, listener);
	}

	@Override
	public void setRequestChannel(MessageChannel requestChannel) {
		super.setRequestChannel(requestChannel);
		this.endpoint.setOutputChannel(requestChannel);
	}

	@Override
	public void setRequestChannelName(String requestChannelName) {
		super.setRequestChannelName(requestChannelName);
		this.endpoint.setOutputChannelName(requestChannelName);
	}

	@Override
	public void setReplyChannel(MessageChannel replyChannel) {
		super.setReplyChannel(replyChannel);
		this.endpoint.getListener().setReplyChannel(replyChannel);
	}

	@Override
	public void setReplyChannelName(String replyChannelName) {
		super.setReplyChannelName(replyChannelName);
		this.endpoint.getListener().setReplyChannelName(replyChannelName);
	}

	@Override
	public void setErrorChannel(MessageChannel errorChannel) {
		super.setErrorChannel(errorChannel);
		this.endpoint.setErrorChannel(errorChannel);
	}

	@Override
	public void setErrorChannelName(String errorChannelName) {
		super.setErrorChannelName(errorChannelName);
		this.endpoint.setErrorChannelName(errorChannelName);
	}

	@Override
	public void setRequestTimeout(long requestTimeout) {
		super.setRequestTimeout(requestTimeout);
		this.endpoint.setSendTimeout(requestTimeout);
	}

	@Override
	public void setReplyTimeout(long replyTimeout) {
		super.setReplyTimeout(replyTimeout);
		this.endpoint.getListener().setReplyTimeout(replyTimeout);
	}

	@Override
	public void setErrorOnTimeout(boolean errorOnTimeout) {
		super.setErrorOnTimeout(errorOnTimeout);
		this.endpoint.getListener().setErrorOnTimeout(errorOnTimeout);
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		super.setShouldTrack(shouldTrack);
		this.endpoint.setShouldTrack(shouldTrack);
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
		this.endpoint.setShutdownContainerOnStop(shutdownContainerOnStop);
	}



	@Override
	public String getComponentType() {
		return this.endpoint.getComponentType();
	}

	@Override
	public void setComponentName(String componentName) {
		super.setComponentName(componentName);
		this.endpoint.setComponentName(getComponentName());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);
		this.endpoint.setApplicationContext(applicationContext);
		this.endpoint.setBeanFactory(applicationContext);
		this.endpoint.getListener().setBeanFactory(applicationContext);
	}

	@Override
	protected void onInit() {
		this.endpoint.afterPropertiesSet();
	}

	public ChannelPublishingJmsMessageListener getListener() {
		return this.endpoint.getListener();
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.endpoint.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.endpoint.stop();
	}

	@Override
	public void destroy() {
		this.endpoint.destroy();
		super.destroy();
	}

	@Override
	public int beforeShutdown() {
		return this.endpoint.beforeShutdown();
	}

	@Override
	public int afterShutdown() {
		return this.endpoint.afterShutdown();
	}

}
