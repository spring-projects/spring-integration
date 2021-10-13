/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.integration.kafka.channel;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * Subscribable channel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public class SubscribableKafkaChannel extends AbstractKafkaChannel implements SubscribableChannel,
		ManageableSmartLifecycle {

	private static final int DEFAULT_PHASE = Integer.MAX_VALUE / 2; // same as MessageProducerSupport

	private final KafkaListenerContainerFactory<?> factory;

	private MessageDispatcher dispatcher;

	private MessageListenerContainer container;

	private boolean autoStartup = true;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;

	/**
	 * Construct an instance with the provided parameters.
	 * @param template template for sending.
	 * @param factory factory for creating a container for receiving.
	 * @param channelTopic the topic.
	 */
	public SubscribableKafkaChannel(KafkaOperations<?, ?> template, KafkaListenerContainerFactory<?> factory,
			String channelTopic) {

		super(template, channelTopic);
		Assert.notNull(factory, "'factory' cannot be null");
		this.factory = factory;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Set the phase.
	 * @param phase the phase.
	 * @see org.springframework.context.Phased
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Set the auto startup.
	 * @param autoStartup true to automatically start.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	protected void onInit() {
		this.dispatcher = createDispatcher();
		this.container = this.factory.createContainer(this.topic);
		String groupId = getGroupId();
		ContainerProperties containerProperties = this.container.getContainerProperties();
		containerProperties.setGroupId(groupId != null ? groupId : getBeanName());
		containerProperties.setMessageListener(new IntegrationRecordMessageListener());
	}

	protected MessageDispatcher createDispatcher() {
		UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
		unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
		return unicastingDispatcher;
	}

	@Override
	public void start() {
		this.container.start();
		this.running = true;
	}

	@Override
	public void stop() {
		this.container.stop();
		this.running = false;
	}

	@Override
	public void stop(Runnable callback) {
		this.container.stop(() -> {
			callback.run();
			this.running = false;
		});
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		return this.dispatcher.addHandler(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		return this.dispatcher.removeHandler(handler);
	}


	private class IntegrationRecordMessageListener extends RecordMessagingMessageListenerAdapter<Object, Object> {

		IntegrationRecordMessageListener() {
			super(null, null); // NOSONAR - out of use
		}

		@Override
		public void onMessage(ConsumerRecord<Object, Object> record, Acknowledgment acknowledgment,
				Consumer<?, ?> consumer) {

			SubscribableKafkaChannel.this.dispatcher.dispatch(toMessagingMessage(record, acknowledgment, consumer));
		}

	}

}
