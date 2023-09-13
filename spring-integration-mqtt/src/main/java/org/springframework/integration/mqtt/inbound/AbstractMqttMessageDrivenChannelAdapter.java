/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.endpoint.MessageProducerSupport;
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
 * @author Gary Russell
 * @author Artem Bilan
 * @author Trung Pham
 * @author Mikhail Polivakha
 *
 * @since 4.0
 *
 */
@ManagedResource
@IntegrationManagedResource
public abstract class AbstractMqttMessageDrivenChannelAdapter extends MessageProducerSupport
		implements ApplicationEventPublisherAware {

	/**
	 * The default completion timeout in milliseconds.
	 */
	public static final long DEFAULT_COMPLETION_TIMEOUT = 30_000L;

	private final String url;

	private final String clientId;

	private final Set<Topic> topics;

	private long completionTimeout = DEFAULT_COMPLETION_TIMEOUT;

	private boolean manualAcks;

	private ApplicationEventPublisher applicationEventPublisher;

	private MqttMessageConverter converter;

	protected final Lock topicLock = new ReentrantLock(); // NOSONAR

	public AbstractMqttMessageDrivenChannelAdapter(@Nullable String url, String clientId, String... topic) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		Assert.notNull(topic, "'topics' cannot be null");
		Assert.noNullElements(topic, "'topics' cannot have null elements");
		this.url = url;
		this.clientId = clientId;
		validateTopics(topic);
		this.topics = new LinkedHashSet<>();
		for (String t : topic) {
			this.topics.add(new Topic(t, 1));
		}
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

	/**
	 * Set the QoS for each topic; a single value will apply to all topics otherwise
	 * the correct number of qos values must be provided.
	 * @param qos The qos value(s).
	 * @since 4.1
	 */
	public void setQos(int... qos) {
		Assert.notNull(qos, "'qos' cannot be null");
		if (qos.length == 1) {
			for (Topic topic : this.topics) {
				topic.setQos(qos[0]);
			}
		}
		else {
			Assert.isTrue(qos.length == this.topics.size(),
					"When setting qos, the array must be the same length as the topics");
			int n = 0;
			for (Topic topic : this.topics) {
				topic.setQos(qos[n++]);
			}
		}
	}

	@ManagedAttribute
	public int[] getQos() {
		this.topicLock.lock();
		try {
			int[] topicQos = new int[this.topics.size()];
			int n = 0;
			for (Topic topic : this.topics) {
				topicQos[n++] = topic.getQos();
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
			String[] topicNames = new String[this.topics.size()];
			int n = 0;
			for (Topic topic : this.topics) {
				topicNames[n++] = topic.getTopic();
			}
			return topicNames;
		}
		finally {
			this.topicLock.unlock();
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
		return this.manualAcks;
	}

	/**
	 * Set the completion timeout for operations. Not settable using the namespace.
	 * Default {@value #DEFAULT_COMPLETION_TIMEOUT} milliseconds.
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
			Topic topik = new Topic(topic, qos);
			if (this.topics.contains(topik)) {
				throw new MessagingException("Topic '" + topic + "' is already subscribed.");
			}
			this.topics.add(topik);
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
			for (String topic : topics) {
				if (this.topics.contains(new Topic(topic, 0))) {
					throw new MessagingException("Topic '" + topic + "' is already subscribed.");
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
			for (String t : topic) {
				if (this.topics.remove(new Topic(t, 0))) {
					logger.debug(LogMessage.format("Removed '%s' from subscriptions.", t));
				}
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * @since 4.1
	 */
	private static final class Topic {

		private final String topic;

		private volatile int qos;

		Topic(String topic, int qos) {
			this.topic = topic;
			this.qos = qos;
		}

		private int getQos() {
			return this.qos;
		}

		private void setQos(int qos) {
			this.qos = qos;
		}

		private String getTopic() {
			return this.topic;
		}

		@Override
		public int hashCode() {
			return this.topic.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Topic other = (Topic) obj;
			return Objects.equals(this.topic, other.topic);
		}

		@Override
		public String toString() {
			return "Topic [topic=" + this.topic + ", qos=" + this.qos + "]";
		}

	}

}
