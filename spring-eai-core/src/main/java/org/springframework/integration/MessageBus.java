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

package org.springframework.integration;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.util.Assert;

/**
 * A central component for registering channels and endpoints. The message bus
 * will autodetect channels and endpoints from its host application context.
 * 
 * @author Mark Fisher
 */
public class MessageBus implements ApplicationContextAware {

	private final Log logger = LogFactory.getLog(getClass());

	private ChannelRegistry channelRegistry = new ChannelRegistry();

	private EndpointRegistry endpointRegistry = new EndpointRegistry();

	private ApplicationContext applicationContext;


	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "applicationContext must not be null");
		this.applicationContext = applicationContext;
		this.initChannels();
		this.initEndpoints();
	}

	@SuppressWarnings("unchecked")
	private void initChannels() {
		Map<String, MessageChannel> channelBeans = (Map<String, MessageChannel>) this.applicationContext
				.getBeansOfType(MessageChannel.class);
		for (Map.Entry<String, MessageChannel> entry : channelBeans.entrySet()) {
			this.registerChannel(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void initEndpoints() {
		Map<String, MessageEndpoint> endpointBeans = (Map<String, MessageEndpoint>) this.applicationContext
				.getBeansOfType(MessageEndpoint.class);
		for (Map.Entry<String, MessageEndpoint> entry : endpointBeans.entrySet()) {
			this.registerEndpoint(entry.getKey(), entry.getValue());
		}
	}

	public void registerChannel(String name, MessageChannel channel) {
		this.channelRegistry.register(name, channel);
		if (logger.isInfoEnabled()) {
			logger.info("registering channel '" + name + "'");
		}
	}

	public void registerEndpoint(String name, MessageEndpoint endpoint) {
		if (endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).start();
		}
		this.endpointRegistry.register(name, endpoint);
		if (logger.isInfoEnabled()) {
			logger.info("registering endpoint '" + name + "'");
		}
	}

}
