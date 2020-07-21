/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
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
 *
 * @since 4.0
 *
 */
@ManagedResource
@IntegrationManagedResource
public abstract class AbstractMqttMessageDrivenChannelAdapter extends MessageProducerSupport {

	private final String url;

	private final String clientId;

	private final Set<Topic> topics;

	private volatile MqttMessageConverter converter;

	protected final Lock topicLock = new ReentrantLock(); // NOSONAR

	public AbstractMqttMessageDrivenChannelAdapter(@Nullable String url, String clientId, String... topic) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		Assert.notNull(topic, "'topics' cannot be null");
		Assert.noNullElements(topic, "'topics' cannot have null elements");
		this.url = url;
		this.clientId = clientId;
		this.topics = new LinkedHashSet<>();
		for (String t : topic) {
			this.topics.add(new Topic(t, 1));
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

	/**
	 * Add a topic to the subscribed list.
	 * @param topic The topic.
	 * @param qos The qos.
	 * @throws MessagingException if the topic is already in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void addTopic(String topic, int qos) {
		this.topicLock.lock();
		try {
			Topic topik = new Topic(topic, qos);
			if (this.topics.contains(topik)) {
				throw new MessagingException("Topic '" + topic + "' is already subscribed.");
			}
			this.topics.add(topik);
			if (this.logger.isDebugEnabled()) {
				logger.debug("Added '" + topic + "' to subscriptions.");
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Add a topic (or topics) to the subscribed list (qos=1).
	 * @param topic The topics.
	 * @throws MessagingException if the topic is already in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void addTopic(String... topic) {
		Assert.notNull(topic, "'topic' cannot be null");
		this.topicLock.lock();
		try {
			for (String t : topic) {
				addTopic(t, 1);
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Add topics to the subscribed list.
	 * @param topic The topics.
	 * @param qos The qos for each topic.
	 * @throws MessagingException if a topic is already in the list.
	 * @since 4.1
	 */
	@ManagedOperation
	public void addTopics(String[] topic, int[] qos) {
		Assert.notNull(topic, "'topic' cannot be null.");
		Assert.noNullElements(topic, "'topic' cannot contain any null elements.");
		Assert.isTrue(topic.length == qos.length, "topic and qos arrays must the be the same length.");
		this.topicLock.lock();
		try {
			for (String topik : topic) {
				if (this.topics.contains(new Topic(topik, 0))) {
					throw new MessagingException("Topic '" + topik + "' is already subscribed.");
				}
			}
			for (int i = 0; i < topic.length; i++) {
				addTopic(topic[i], qos[i]);
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
				if (this.topics.remove(new Topic(t, 0)) && this.logger.isDebugEnabled()) {
					logger.debug("Removed '" + t + "' from subscriptions.");
				}
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.converter == null) {
			DefaultPahoMessageConverter pahoMessageConverter = new DefaultPahoMessageConverter();
			pahoMessageConverter.setBeanFactory(getBeanFactory());
			this.converter = pahoMessageConverter;

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
			if (this.topic == null) {
				if (other.topic != null) {
					return false;
				}
			}
			else if (!this.topic.equals(other.topic)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "Topic [topic=" + this.topic + ", qos=" + this.qos + "]";
		}

	}

}
