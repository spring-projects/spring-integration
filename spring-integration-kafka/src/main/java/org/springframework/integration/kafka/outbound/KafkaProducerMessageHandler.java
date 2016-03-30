/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.integration.kafka.outbound;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Kafka Message Handler.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Gary Russell
 * @author Marius Bogoevici
 * @since 0.5
 */
public class KafkaProducerMessageHandler<K, V> extends AbstractMessageHandler {

	private final KafkaTemplate<K, V> kafkaTemplate;

	private EvaluationContext evaluationContext;

	private boolean enableHeaderRouting = true;

	private volatile Expression topicExpression;

	private volatile Expression messageKeyExpression;

	private volatile Expression partitionIdExpression;

	public KafkaProducerMessageHandler(final KafkaTemplate<K, V> kafkaTemplate) {
		Assert.notNull(kafkaTemplate, "kafkaTemplate cannot be null");
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * Enable the use of headers for determining the target topic and partition of outbound messages. By default it is
	 * set to true, but it can be disabled when those values are produced by upstream components that read messages
	 * from Kafka sources themselves.
	 * @param enableHeaderRouting whether the topic and destination headers should be considered
	 * @since 1.3
	 * @see KafkaHeaders#TOPIC
	 * @see KafkaHeaders#PARTITION_ID
	 */
	public void setEnableHeaderRouting(boolean enableHeaderRouting) {
		this.enableHeaderRouting = enableHeaderRouting;
	}

	public void setTopicExpression(Expression topicExpression) {
		this.topicExpression = topicExpression;
	}

	public void setMessageKeyExpression(Expression messageKeyExpression) {
		this.messageKeyExpression = messageKeyExpression;
	}

	/**
	 * Set the partition expression.
	 * @param partitionExpression an expression that returns a partition id
	 * @deprecated as of 1.3, {@link #setPartitionIdExpression(Expression)} should be used instead
	 */
	@Deprecated
	public void setPartitionExpression(Expression partitionExpression) {
		setPartitionIdExpression(partitionExpression);
	}

	public void setPartitionIdExpression(Expression partitionIdExpression) {
		this.partitionIdExpression = partitionIdExpression;
	}

	public KafkaTemplate<?, ?> getKafkaTemplate() {
		return this.kafkaTemplate;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleMessageInternal(final Message<?> message) throws Exception {
		String topic = this.topicExpression != null ?
				this.topicExpression.getValue(this.evaluationContext, message, String.class)
				//TODO revise the headers fallback behavior in favor of just expression
				: (this.enableHeaderRouting ? message.getHeaders().get(KafkaHeaders.TOPIC, String.class) : null);

		Integer partitionId = this.partitionIdExpression != null ?
				this.partitionIdExpression.getValue(this.evaluationContext, message, Integer.class)
				: (this.enableHeaderRouting ? message.getHeaders().get(KafkaHeaders.PARTITION_ID, Integer.class) : null);

		Object messageKey = this.messageKeyExpression != null
				? this.messageKeyExpression.getValue(this.evaluationContext, message)
				: message.getHeaders().get(KafkaHeaders.MESSAGE_KEY);

		if (partitionId == null) {
			if (messageKey == null) {
				this.kafkaTemplate.send(topic, (V) message.getPayload());
			}
			else {
				this.kafkaTemplate.send(topic, (K) messageKey, (V) message.getPayload());
			}
		}
		else {
			if (messageKey == null) {
				this.kafkaTemplate.send(topic, partitionId, (V) message.getPayload());
			}
			else {
				this.kafkaTemplate.send(topic, partitionId, (K) messageKey, (V) message.getPayload());
			}
		}
	}

	@Override
	public String getComponentType() {
		return "kafka:outbound-channel-adapter";
	}

}
