/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.mqtt.outbound;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT outbound channel adapters.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 *
 */
public abstract class AbstractMqttMessageHandler extends AbstractMessageHandler implements Lifecycle {

	private static final MessageProcessor<String> DEFAULT_TOPIC_PROCESSOR =
			m -> (String) m.getHeaders().get(MqttHeaders.TOPIC);

	private final AtomicBoolean running = new AtomicBoolean();

	private final String url;

	private final String clientId;

	private String defaultTopic;

	private MessageProcessor<String> topicProcessor = DEFAULT_TOPIC_PROCESSOR;

	private int defaultQos = 0;

	private MessageProcessor<Integer> qosProcessor;

	private boolean defaultRetained = false;

	private MessageProcessor<Boolean> retainedProcessor;

	private MessageConverter converter;

	private int clientInstance;

	public AbstractMqttMessageHandler(String url, String clientId) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		this.url = url;
		this.clientId = clientId;
	}

	/**
	 * Set the topic to which the message will be published if the
	 * {@link #setTopicExpression(Expression) topicExpression} evaluates to `null`.
	 * @param defaultTopic the default topic.
	 */
	public void setDefaultTopic(String defaultTopic) {
		this.defaultTopic = defaultTopic;
	}

	/**
	 * Set the topic expression; default "headers['mqtt_topic']".
	 * @param topicExpression the expression.
	 * @since 5.0
	 */
	public void setTopicExpression(Expression topicExpression) {
		Assert.notNull(topicExpression, "'topicExpression' cannot be null");
		this.topicProcessor = new ExpressionEvaluatingMessageProcessor<>(topicExpression);
	}

	/**
	 * Set the topic expression; default "headers['mqtt_topic']".
	 * @param topicExpression the expression.
	 * @since 5.0
	 */
	public void setTopicExpressionString(String topicExpression) {
		Assert.hasText(topicExpression, "'topicExpression' must not be null or empty");
		this.topicProcessor = new ExpressionEvaluatingMessageProcessor<>(topicExpression);
	}

	/**
	 * Set the qos for messages if the {@link #setQosExpression(Expression) qosExpression}
	 * evaluates to null.
	 * @param defaultQos the default qos.
	 */
	public void setDefaultQos(int defaultQos) {
		this.defaultQos = defaultQos;
	}

	/**
	 * Set the qos expression; default "headers['mqtt_qos']".
	 * @param qosExpression the expression.
	 * @since 5.0
	 */
	public void setQosExpression(Expression qosExpression) {
		Assert.notNull(qosExpression, "'qosExpression' cannot be null");
		this.qosProcessor = new ExpressionEvaluatingMessageProcessor<>(qosExpression);
	}

	/**
	 * Set the qos expression; default "headers['mqtt_qos']".
	 * @param qosExpression the expression.
	 * @since 5.0
	 */
	public void setQosExpressionString(String qosExpression) {
		Assert.hasText(qosExpression, "'qosExpression' must not be null or empty");
		this.qosProcessor = new ExpressionEvaluatingMessageProcessor<>(qosExpression);
	}

	/**
	 * Set the retained boolean for messages if the
	 * {@link #setRetainedExpression(Expression) retainedExpression} evaluates to null.
	 * @param defaultRetained the default defaultRetained.
	 */
	public void setDefaultRetained(boolean defaultRetained) {
		this.defaultRetained = defaultRetained;
	}

	/**
	 * Set the retained expression; default "headers['mqtt_retained']".
	 * @param retainedExpression the expression.
	 * @since 5.0
	 */
	public void setRetainedExpression(Expression retainedExpression) {
		Assert.notNull(retainedExpression, "'qosExpression' cannot be null");
		this.retainedProcessor = new ExpressionEvaluatingMessageProcessor<>(retainedExpression);
	}

	/**
	 * Set the retained expression; default "headers['mqtt_retained']".
	 * @param retainedExpression the expression.
	 * @since 5.0
	 */
	public void setRetainedExpressionString(String retainedExpression) {
		Assert.hasText(retainedExpression, "'qosExpression' must not be null or empty");
		this.retainedProcessor = new ExpressionEvaluatingMessageProcessor<>(retainedExpression);
	}

	public void setConverter(MessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected MessageConverter getConverter() {
		return this.converter;
	}

	protected String getUrl() {
		return this.url;
	}

	public String getClientId() {
		return this.clientId;
	}

	/**
	 * Incremented each time the client is connected.
	 * @return The instance;
	 * @since 4.1
	 */
	public int getClientInstance() {
		return this.clientInstance;
	}

	@Override
	public String getComponentType() {
		return "mqtt:outbound-channel-adapter";
	}

	protected void incrementClientInstance() {
		this.clientInstance++; //NOSONAR - false positive - called from synchronized block
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.topicProcessor instanceof BeanFactoryAware && getBeanFactory() != null) {
			((BeanFactoryAware) this.topicProcessor).setBeanFactory(getBeanFactory());
		}
		if (this.qosProcessor instanceof BeanFactoryAware && getBeanFactory() != null) {
			((BeanFactoryAware) this.qosProcessor).setBeanFactory(getBeanFactory());
		}
		if (this.retainedProcessor instanceof BeanFactoryAware && getBeanFactory() != null) {
			((BeanFactoryAware) this.retainedProcessor).setBeanFactory(getBeanFactory());
		}
		if (this.converter == null) {
			DefaultPahoMessageConverter mConverter = new DefaultPahoMessageConverter(
					m -> {
						if (this.qosProcessor == null) {
							return MqttMessageConverter.defaultQosFunction(this.defaultQos).apply(m);
						}
						else {
							Integer result = this.qosProcessor.processMessage(m);
							if (result == null) {
								return this.defaultQos;
							}
							else {
								return result;
							}
						}
					},
					m -> {
						if (this.retainedProcessor == null) {
							return  MqttMessageConverter.defaultRetainedFunction(this.defaultRetained).apply(m);
						}
						else {
							Boolean result = this.retainedProcessor.processMessage(m);
							if (result == null) {
								return this.defaultRetained;
							}
							else {
								return result;
							}
						}
					});
			if (getBeanFactory() != null) {
				mConverter.setBeanFactory(getBeanFactory());
			}
			this.converter = mConverter;
		}
	}

	@Override
	public final void start() {
		if (!this.running.getAndSet(true)) {
			doStart();
		}
	}

	protected abstract void doStart();

	@Override
	public final void stop() {
		if (this.running.getAndSet(false)) {
			doStop();
		}
	}

	protected abstract void doStop();

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object mqttMessage = this.converter.fromMessage(message, Object.class);
		String topic = this.topicProcessor.processMessage(message);
		if (topic == null && this.defaultTopic == null) {
			throw new MessageHandlingException(message,
					"No topic could be determined from the message and no default topic defined");
		}
		this.publish(topic == null ? this.defaultTopic : topic, mqttMessage, message);
	}

	protected abstract void publish(String topic, Object mqttMessage, Message<?> message) throws Exception;

}
