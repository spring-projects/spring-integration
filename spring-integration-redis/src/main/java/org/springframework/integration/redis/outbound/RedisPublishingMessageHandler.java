/*
 * Copyright 2007-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.redis.outbound;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class RedisPublishingMessageHandler extends AbstractMessageHandler implements IntegrationEvaluationContextAware {

	private final RedisTemplate<?, ?> template;

	private volatile EvaluationContext evaluationContext;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile RedisSerializer<?> serializer = new StringRedisSerializer();

	private volatile Expression topicExpression;

	public RedisPublishingMessageHandler(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.template = new RedisTemplate<Object, Object>();
		this.template.setConnectionFactory(connectionFactory);
		this.template.setEnableDefaultSerializer(false);
		this.template.afterPropertiesSet();
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.serializer = serializer;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * @param defaultTopic The default topic.
	 *
	 * @deprecated in favor of {@link #setTopicExpression(Expression)} or {@link #setTopic(String)}
	 */
	@Deprecated
	public void setDefaultTopic(String defaultTopic) {
		Assert.hasText(defaultTopic, "'defaultTopic' must not be an empty string.");
		this.setTopicExpression(new LiteralExpression(defaultTopic));
	}

	public void setTopic(String topic) {
		Assert.hasText(topic, "'topic' must not be an empty string.");
		this.setTopicExpression(new LiteralExpression(topic));
	}

	public void setTopicExpression(Expression topicExpression) {
		Assert.notNull(topicExpression, "'topicExpression' must not be null.");
		this.topicExpression = topicExpression;
	}

	@Override
	public String getComponentType() {
		return "redis:outbound-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(topicExpression, "'topicExpression' must not be null.");
		if (this.messageConverter instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageConverter).setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String topic = this.topicExpression.getValue(this.evaluationContext, message, String.class);
		Object value = this.messageConverter.fromMessage(message, null);

		if (value instanceof byte[]) {
			this.template.convertAndSend(topic, value);
		}
		else {
			this.template.convertAndSend(topic, ((RedisSerializer<Object>) this.serializer).serialize(value));
		}
	}

}
