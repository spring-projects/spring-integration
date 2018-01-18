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

package org.springframework.integration.zeromq.inbound;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Abstract class for ZMQ Message-Driven Channel Adapters.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
@ManagedResource
@IntegrationManagedResource
public abstract class AbstractZeromqMessageDrivenChannelAdapter extends MessageProducerSupport {

	private final String url;

	private final String clientId;

	private AtomicReference<Topic> topic;

	private volatile org.springframework.integration.zeromq.support.ZeromqMessageConverter converter;

	protected final Lock topicLock = new ReentrantLock();

	public AbstractZeromqMessageDrivenChannelAdapter(String url, String clientId) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		this.url = url;
		this.clientId = clientId;
	}

	public void setConverter(org.springframework.integration.zeromq.support.ZeromqMessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected String getUrl() {
		return this.url;
	}

	protected String getClientId() {
		return this.clientId;
	}

	protected org.springframework.integration.zeromq.support.ZeromqMessageConverter getConverter() {
		return this.converter;
	}

	@ManagedAttribute
	public String getTopic() {
		this.topicLock.lock();
		try {
			if (this.topic != null) {
				String topicName = this.topic.get().getTopic();
				return topicName;
			}
			else {
				return null;
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	public String getComponentType() {
		return "zeromq:inbound-channel-adapter";
	}

	/**
	 * Set topic.
	 * @param topic The topic.
	 * @throws MessagingException if the topic is already in the list.
	 * @since 5.0.1
	 */
	@ManagedOperation
	public void setTopic(String topic) {
		this.topicLock.lock();
		try {
			Topic topik = new Topic(topic);
			if (Objects.isNull(this.topic)) {
				this.topic = new AtomicReference<>();
			}
			if (Objects.nonNull(this.topic.get()) && this.topic.get().equals(topik)) {
				throw new MessagingException("Topic '" + topic + "' is already subscribed");
			}
			this.topic.set(topik);
			if (this.logger.isDebugEnabled()) {
				logger.debug("Set '" + topic + "' to subscriptions.");
			}
		}
		finally {
			this.topicLock.unlock();
		}
	}

	/**
	 * Remove topic.
	 * @param topic The topic.
	 * @throws MessagingException if the topic is not in the list.
	 * @since 5.0.1
	 */
	@ManagedOperation
	public void removeTopic(String topic) {
		this.topicLock.lock();
		try {
			if (this.topic.compareAndSet(new Topic(topic), null)) {
				if (this.logger.isDebugEnabled()) {
					logger.debug("Removed '" + topic + "' from subscriptions");
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
			org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter zmqMessageConverter = new org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter();
			zmqMessageConverter.setBeanFactory(getBeanFactory());
			this.converter = zmqMessageConverter;
		}
	}

	private static final class Topic {

		private final String topic;

		Topic(String topic) {
			this.topic = topic;
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
			return "Topic [topic=" + this.topic + "]";
		}
	}
}
