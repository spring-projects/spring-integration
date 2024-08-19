/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.mqtt.inbound;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT Message-Driven Channel Adapters.
 *
 * @param <T> MQTT Client type
 * @param <C> MQTT connection options type (v5 or v3)
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Trung Pham
 * @author Mikhail Polivakha
 * @author Artem Vozhdayenko
 * @author Jiri Soucek
 *
 * @since 4.0
 *
 */
@ManagedResource
@IntegrationManagedResource
public abstract class AbstractMqttMessageDrivenChannelAdapter<T, C> extends MessageProducerSupport
		implements ApplicationEventPublisherAware, ClientManager.ConnectCallback {

	protected final Lock topicLock = new ReentrantLock(); // NOSONAR

	private final String url;

	private final String clientId;

	private final Map<String, Integer> topics;

	private final ClientManager<T, C> clientManager;

	private long completionTimeout = ClientManager.DEFAULT_COMPLETION_TIMEOUT;

	private long disconnectCompletionTimeout = ClientManager.DISCONNECT_COMPLETION_TIMEOUT;

	private boolean manualAcks;

	private ApplicationEventPublisher applicationEventPublisher;

	private MqttMessageConverter converter;

	public AbstractMqttMessageDrivenChannelAdapter(@Nullable String url, String clientId, String... topic) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		this.url = url;
		this.clientId = clientId;
		this.topics = initTopics(topic);
		this.clientManager = null;
	}

	public AbstractMqttMessageDrivenChannelAdapter(ClientManager<T, C> clientManager, String... topic) {
		Assert.notNull(clientManager, "'clientManager' cannot be null");
		this.clientManager = clientManager;
		this.topics = initTopics(topic);
		this.url = null;
		this.clientId = null;
	}

	private static Map<String, Integer> initTopics(String[] topics) {
		validateTopics(topics);

		return Arrays.stream(topics)
				.collect(Collectors.toMap(Function.identity(), (key) -> 1, (x, y) -> y, LinkedHashMap::new));
	}

	private static void validateTopics(String[] topics) {
		Assert.notNull(topics, "'topics' cannot be null");
		Assert.noNullElements(topics, "'topics' cannot have null elements");

		for (String topic : topics) {
			Assert.hasText(topic, "The topic to subscribe cannot be empty string");
		}
	}

	public void setConverter(MqttMessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	@Nullable
	protected ClientManager<T, C> getClientManager() {
		return this.clientManager;
	}

	/**
	 * Set the QoS for each topic; a single value will apply to all topics otherwise
	 * the correct number of qos values must be provided.
	 * @param qos The qos value(s).
	 * @since 4.1
	 */
	public void setQos(int... qos) {
		Assert.notNull(qos, "'qos' cannot be null");
		if (qos.length == 1) {
			for (Map.Entry<String, Integer> topic : this.topics.entrySet()) {
				topic.setValue(qos[0]);
			}
		}
		else {
			Assert.isTrue(qos.length == this.topics.size(),
					"When setting qos, the array must be the same length as the topics");
			int n = 0;
			for (Map.Entry<String, Integer> topic : this.topics.entrySet()) {
				topic.setValue(qos[n++]);
			}
		}
	}

	@ManagedAttribute
	public int[] getQos() {
		this.topicLock.lock();
		try {
			int[] topicQos = new int[this.topics.size()];
			int n = 0;
			for (int qos : this.topics.values()) {
				topicQos[n++] = qos;
			}
			return topicQos;
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Nullable
	protected String getUrl() {
		return this.url;
	}

	@Nullable
	protected String getClientId() {
		return this.clientId;
	}

	protected MqttMessageConverter getConverter() {
		return this.converter;
	}

	@ManagedAttribute
	public String[] getTopic() {
		this.topicLock.lock();
		try {
			return this.topics.keySet().toArray(new String[0]);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Set the completion timeout when disconnecting.
	 * Default {@value ClientManager#DISCONNECT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 5.1.10
	 */
	public void setDisconnectCompletionTimeout(long completionTimeout) {
		this.disconnectCompletionTimeout = completionTimeout;
	}

	protected long getDisconnectCompletionTimeout() {
		return this.disconnectCompletionTimeout;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.clientManager != null) {
			this.clientManager.addCallback(this);
			if (this.clientManager.isConnected()) {
				connectComplete(false);
			}
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (this.clientManager != null) {
			this.clientManager.removeCallback(this);
		}
	}

	@Override
	public String getComponentType() {
		return "mqtt:inbound-channel-adapter";
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher; // NOSONAR (inconsistent synchronization)
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	/**
	 * Set the acknowledgment mode to manual.
	 * @param manualAcks true for manual acks.
	 * @since 5.3
	 */
	public void setManualAcks(boolean manualAcks) {
		this.manualAcks = manualAcks;
	}

	protected boolean isManualAcks() {
		return this.clientManager == null ? this.manualAcks : this.clientManager.isManualAcks();
	}

	/**
	 * Set the completion timeout for operations.
	 * Default {@value ClientManager#DEFAULT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 4.1
	 */
	public void setCompletionTimeout(long completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

	protected long getCompletionTimeout() {
		return this.completionTimeout;
	}

	/**
	 * Add a topic to the subscribed list.
	 * @param topic The topic.
	 * @param qos The qos.
	 * @throws MessagingException if the topic is already in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void addTopic(String topic, int qos) {
		validateTopics(new String[] {topic});
		this.topicLock.lock();
		try {
			if (this.topics.containsKey(topic)) {
				throw new MessagingException("Topic '" + topic + "' is already subscribed.");
			}
			this.topics.put(topic, qos);
			logger.debug(LogMessage.format("Added '%s' to subscriptions.", topic));
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Add a topic (or topics) to the subscribed list (qos=1).
	 * @param topics The topics.
	 * @throws MessagingException if the topics is already in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void addTopic(String... topics) {
		validateTopics(topics);
		this.topicLock.lock();
		try {
			for (String t : topics) {
				addTopic(t, 1);
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Add topics to the subscribed list.
	 * @param topics The topics.
	 * @param qos The qos for each topic.
	 * @throws MessagingException if a topics is already in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void addTopics(String[] topics, int[] qos) {
		validateTopics(topics);
		Assert.isTrue(topics.length == qos.length, "topics and qos arrays must the be the same length.");
		this.topicLock.lock();
		try {
			for (String newTopic : topics) {
				if (this.topics.containsKey(newTopic)) {
					throw new MessagingException("Topic '" + newTopic + "' is already subscribed.");
				}
			}
			for (int i = 0; i < topics.length; i++) {
				addTopic(topics[i], qos[i]);
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Remove a topic (or topics) from the subscribed list.
	 * @param topic The topic.
	 * @throws MessagingException if the topic is not in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void removeTopic(String... topic) {
		this.topicLock.lock();
		try {
			for (String name : topic) {
				if (this.topics.remove(name) != null) {
					logger.debug(LogMessage.format("Removed '%s' from subscriptions.", name));
				}
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

}
