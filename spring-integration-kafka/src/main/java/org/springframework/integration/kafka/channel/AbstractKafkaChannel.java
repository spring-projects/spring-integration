/*
 * Copyright 2020 the original author or authors.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Abstract MessageChannel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public abstract class AbstractKafkaChannel extends AbstractMessageChannel {

	private final KafkaOperations<?, ?> template;

	protected final String topic; // NOSONAR final

	private String groupId;

	/**
	 * Construct an instance with the provided {@link KafkaOperations} and topic.
	 * @param template the template.
	 * @param topic the topic.
	 */
	public AbstractKafkaChannel(KafkaOperations<?, ?> template, String topic) {
		Assert.notNull(template, "'template' cannot be null");
		Assert.notNull(topic, "'topic' cannot be null");
		this.template = template;
		this.topic = topic;
	}

	/**
	 * Set the group id for the consumer; if not set, the bean name will be used.
	 * @param groupId the group id.
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	protected String getGroupId() {
		return this.groupId;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		try {
			this.template.send(MessageBuilder.fromMessage(message)
					.setHeader(KafkaHeaders.TOPIC, this.topic)
					.build())
					.get(timeout < 0 ? Long.MAX_VALUE : timeout, TimeUnit.MILLISECONDS);
		}
		catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
			this.logger.debug(() -> "Interrupted while waiting for send result for: " + message);
			return false;
		}
		catch (ExecutionException e) {
			this.logger.error(e.getCause(), () -> "Interrupted while waiting for send result for: " + message);
			return false;
		}
		catch (TimeoutException e) {
			this.logger.debug(e, () -> "Timed out while waiting for send result for: " + message);
			return false;
		}
		return true;
	}

}
