/*
 * Copyright 2013-2014 the original author or authors.
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
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Gary Russell
 * @since 0.5
 */
public class KafkaProducerMessageHandler extends AbstractMessageHandler
		implements IntegrationEvaluationContextAware {

	private final KafkaProducerContext kafkaProducerContext;

	private EvaluationContext evaluationContext;

	private volatile Expression topicExpression;

	private volatile Expression messageKeyExpression;

	private volatile Expression partitionExpression;

	@SuppressWarnings("unchecked")
	public KafkaProducerMessageHandler(final KafkaProducerContext kafkaProducerContext) {
		this.kafkaProducerContext = kafkaProducerContext;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	public void setTopicExpression(Expression topicExpression) {
		this.topicExpression = topicExpression;
	}

	public void setMessageKeyExpression(Expression messageKeyExpression) {
		this.messageKeyExpression = messageKeyExpression;
	}

	public void setPartitionExpression(Expression partitionExpression) {
		this.partitionExpression = partitionExpression;
	}

	public KafkaProducerContext getKafkaProducerContext() {
		return this.kafkaProducerContext;
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) throws Exception {
		String topic = this.topicExpression != null ?
				this.topicExpression.getValue(getEvaluationContext(), message, String.class)
				: message.getHeaders().get(KafkaHeaders.TOPIC, String.class);

		Integer partitionId = this.partitionExpression != null ?
				this.partitionExpression.getValue(getEvaluationContext(), message, Integer.class)
				: message.getHeaders().get(KafkaHeaders.PARTITION_ID, Integer.class);

		Object messageKey = this.messageKeyExpression != null
				? this.messageKeyExpression.getValue(getEvaluationContext(), message)
				: message.getHeaders().get(KafkaHeaders.MESSAGE_KEY);

		this.kafkaProducerContext.send(topic, partitionId, messageKey, message.getPayload());
	}

	private EvaluationContext getEvaluationContext() {
		// Consider moving this back into onInit() once https://jira.spring.io/browse/INT-3749 is fixed
		if (this.evaluationContext == null) {
			throw new IllegalStateException("Evaluation context not initialized");
		}
		return this.evaluationContext;
	}

	@Override
	public String getComponentType() {
		return "kafka:outbound-channel-adapter";
	}

}
