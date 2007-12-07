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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.consumer.AbstractConsumer;
import org.springframework.integration.channel.consumer.ConsumerType;
import org.springframework.integration.channel.consumer.EventDrivenConsumer;
import org.springframework.integration.channel.consumer.FixedDelayConsumer;
import org.springframework.integration.channel.consumer.FixedRateConsumer;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.util.Assert;

/**
 * A central component for registering channels and endpoints. The message bus
 * will autodetect channels and endpoints from its host application context.
 * 
 * @author Mark Fisher
 */
public class MessageBus implements ChannelResolver, ApplicationContextAware, Lifecycle {

	private final Log logger = LogFactory.getLog(getClass());

	private Map<String, MessageChannel> channels = new ConcurrentHashMap<String, MessageChannel>();

	private Map<String, MessageEndpoint> endpoints = new ConcurrentHashMap<String, MessageEndpoint>();

	private List<AbstractConsumer> consumers = new CopyOnWriteArrayList<AbstractConsumer>();

	private ApplicationContext applicationContext;

	private boolean running;

	private Object lifecycleMonitor = new Object();


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
			this.channels.put(entry.getKey(), entry.getValue());
			if (logger.isInfoEnabled()) {
				logger.info("registered channel '" + entry.getKey() + "'");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void initEndpoints() {
		Map<String, MessageEndpoint> endpointBeans = (Map<String, MessageEndpoint>) this.applicationContext
				.getBeansOfType(MessageEndpoint.class);
		for (Map.Entry<String, MessageEndpoint> entry : endpointBeans.entrySet()) {
			this.endpoints.put(entry.getKey(), entry.getValue());
			if (logger.isInfoEnabled()) {
				logger.info("registered endpoint '" + entry.getKey() + "'");
			}
		}
	}

	public MessageChannel resolve(String channelName) {
		return this.channels.get(channelName);
	}

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.isRunning()) {
				this.running = true;
				this.activateEndpoints();
			}
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				this.running = false;
				this.deactivateEndpoints();
			}
		}
	}

	private void activateEndpoints() {
		for (MessageEndpoint endpoint : this.endpoints.values()) {
			MessageSource source = endpoint.getSource();
			ConsumerType consumerType = endpoint.getConsumerType();
			AbstractConsumer consumer = createConsumer(consumerType, source, endpoint);
			consumer.initialize();
			consumer.start();
		}
	}

	private void deactivateEndpoints() {
		for (AbstractConsumer consumer : this.consumers) {
			consumer.stop();
		}
	}


	/**
	 * Create a consumer based upon the specified consumer type.
	 */
	private AbstractConsumer createConsumer(ConsumerType type, MessageSource source, MessageEndpoint endpoint) {
		if (type.equals(ConsumerType.EVENT_DRIVEN)) {
			return new EventDrivenConsumer(source, endpoint);
		}
		else if (type.equals(ConsumerType.FIXED_RATE)) {
			return new FixedRateConsumer(source, endpoint);
		}
		else if (type.equals(ConsumerType.FIXED_DELAY)) {
			return new FixedDelayConsumer(source, endpoint);
		}
		else {
			throw new UnsupportedOperationException("the consumerType '"
					+ type.name() + "' is not supported.");
		}
	}

}
