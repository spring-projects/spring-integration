/*
 * Copyright 2016-2017 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
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
public class JmsInboundGateway extends MessagingGatewaySupport implements DisposableBean, OrderlyShutdownCapable {

	private final JmsMessageDrivenEndpoint endpoint;

	public JmsInboundGateway(AbstractMessageListenerContainer listenerContainer,
			ChannelPublishingJmsMessageListener listener) {
		this.endpoint = new JmsMessageDrivenEndpoint(listenerContainer, listener);
	}

	@Override
	public void setRequestChannel(MessageChannel requestChannel) {
		super.setRequestChannel(requestChannel);
		this.endpoint.getListener().setRequestChannel(requestChannel);
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
	protected void onInit() throws Exception {
		this.endpoint.afterPropertiesSet();
	}

	public ChannelPublishingJmsMessageListener getListener() {
		return this.endpoint.getListener();
	}

	@Override
	protected void doStart() {
		this.endpoint.start();
	}

	@Override
	protected void doStop() {
		this.endpoint.stop();
	}

	@Override
	public void destroy() throws Exception {
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
