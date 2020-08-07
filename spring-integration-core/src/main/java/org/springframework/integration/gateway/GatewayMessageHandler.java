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

package org.springframework.integration.gateway;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * The {@link AbstractReplyProducingMessageHandler} implementation for mid-flow Gateway.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class GatewayMessageHandler extends AbstractReplyProducingMessageHandler implements ManageableLifecycle {

	private final GatewayProxyFactoryBean gatewayProxyFactoryBean;

	private RequestReplyExchanger exchanger;

	private volatile boolean running;

	public GatewayMessageHandler() {
		this.gatewayProxyFactoryBean = new GatewayProxyFactoryBean();
	}

	public void setRequestChannel(MessageChannel requestChannel) {
		this.gatewayProxyFactoryBean.setDefaultRequestChannel(requestChannel);
	}

	public void setRequestChannelName(String requestChannel) {
		this.gatewayProxyFactoryBean.setDefaultRequestChannelName(requestChannel);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.gatewayProxyFactoryBean.setDefaultReplyChannel(replyChannel);
	}

	public void setReplyChannelName(String replyChannel) {
		this.gatewayProxyFactoryBean.setDefaultReplyChannelName(replyChannel);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.gatewayProxyFactoryBean.setErrorChannel(errorChannel);
	}

	public void setErrorChannelName(String errorChannel) {
		this.gatewayProxyFactoryBean.setErrorChannelName(errorChannel);
	}

	public void setRequestTimeout(Long requestTimeout) {
		this.gatewayProxyFactoryBean.setDefaultRequestTimeout(requestTimeout);
	}

	public void setReplyTimeout(Long replyTimeout) {
		this.gatewayProxyFactoryBean.setDefaultReplyTimeout(replyTimeout);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		if (this.exchanger == null) {
			synchronized (this) {
				if (this.exchanger == null) {
					initialize();
				}
			}
		}
		return this.exchanger.exchange(requestMessage);
	}

	private void initialize() {
		BeanFactory beanFactory = getBeanFactory();

		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) beanFactory).initializeBean(this.gatewayProxyFactoryBean,
					getComponentName() + "#gpfb");
		}
		try {
			this.exchanger = (RequestReplyExchanger) this.gatewayProxyFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BeanCreationException("Can't instantiate the GatewayProxyFactoryBean: " + this, e);
		}
		if (this.running) {
			// We must stop gatewayProxyFactoryBean because after the normal start its "gatewayMap" is still empty
			this.gatewayProxyFactoryBean.stop();
			this.gatewayProxyFactoryBean.start();
		}
	}

	@Override
	public void start() {
		this.gatewayProxyFactoryBean.start();
		this.running = true;
	}

	@Override
	public void stop() {
		this.gatewayProxyFactoryBean.stop();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
