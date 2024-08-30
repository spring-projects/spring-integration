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

package org.springframework.integration.mqtt.outbound;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageNotDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT outbound channel adapters.
 *
 * @param <T> MQTT Client type
 * @param <C> MQTT connection options type (v5 or v3)
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 4.0
 *
 */
public abstract class AbstractMqttMessageHandler<T, C> extends AbstractMessageHandler
		implements ManageableLifecycle, ApplicationEventPublisherAware {

	/**
	 * The default disconnect completion timeout in milliseconds.
	 */
	public static final long DISCONNECT_COMPLETION_TIMEOUT = 5_000L;

	/**
	 * The default completion timeout in milliseconds.
	 */
	public static final long DEFAULT_COMPLETION_TIMEOUT = 30_000L;

	private static final MessageProcessor<String> DEFAULT_TOPIC_PROCESSOR =
			(message) -> message.getHeaders().get(MqttHeaders.TOPIC, String.class);

	protected final Lock lock = new ReentrantLock();

	private final AtomicBoolean running = new AtomicBoolean();

	private final String url;

	private final String clientId;

	private final ClientManager<T, C> clientManager;

	private boolean async;

	private boolean asyncEvents;

	private long completionTimeout = DEFAULT_COMPLETION_TIMEOUT;

	private long disconnectCompletionTimeout = DISCONNECT_COMPLETION_TIMEOUT;

	private String defaultTopic;

	private MessageProcessor<String> topicProcessor = DEFAULT_TOPIC_PROCESSOR;

	private int defaultQos = 0;

	private MessageProcessor<Integer> qosProcessor = MqttMessageConverter.defaultQosProcessor();

	private boolean defaultRetained;

	private MessageProcessor<Boolean> retainedProcessor = MqttMessageConverter.defaultRetainedProcessor();

	private MessageConverter converter;

	private ApplicationEventPublisher applicationEventPublisher;

	private int clientInstance;

	public AbstractMqttMessageHandler(@Nullable String url, String clientId) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		this.url = url;
		this.clientId = clientId;
		this.clientManager = null;
	}

	public AbstractMqttMessageHandler(ClientManager<T, C> clientManager) {
		Assert.notNull(clientManager, "'clientManager' cannot be null or empty");
		this.clientManager = clientManager;
		this.url = null;
		this.clientId = null;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	/**
	 * Set the topic to which the message will be published if the
	 * {@link #setTopicExpression(Expression) topicExpression} evaluates to `null`.
	 * @param defaultTopic the default topic.
	 */
	public void setDefaultTopic(String defaultTopic) {
		Assert.hasText(defaultTopic, "'defaultTopic' must not be empty");
		this.defaultTopic = defaultTopic;
	}

	protected String getDefaultTopic() {
		return this.defaultTopic;
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

	protected MessageProcessor<String> getTopicProcessor() {
		return this.topicProcessor;
	}

	/**
	 * Set the qos for messages if the {@link #setQosExpression(Expression) qosExpression}
	 * evaluates to null. Only applies if a message converter is not provided.
	 * @param defaultQos the default qos.
	 * @see #setConverter(MessageConverter)
	 */
	public void setDefaultQos(int defaultQos) {
		this.defaultQos = defaultQos;
	}

	protected int getDefaultQos() {
		return this.defaultQos;
	}

	/**
	 * Set the qos expression; default "headers['mqtt_qos']".
	 * Only applies if a message converter is not provided.
	 * @param qosExpression the expression.
	 * @since 5.0
	 * @see #setConverter(MessageConverter)
	 */
	public void setQosExpression(Expression qosExpression) {
		Assert.notNull(qosExpression, "'qosExpression' cannot be null");
		this.qosProcessor = new ExpressionEvaluatingMessageProcessor<>(qosExpression);
	}

	/**
	 * Set the qos expression; default "headers['mqtt_qos']".
	 * Only applies if a message converter is not provided.
	 * @param qosExpression the expression.
	 * @since 5.0
	 * @see #setConverter(MessageConverter)
	 */
	public void setQosExpressionString(String qosExpression) {
		Assert.hasText(qosExpression, "'qosExpression' must not be null or empty");
		this.qosProcessor = new ExpressionEvaluatingMessageProcessor<>(qosExpression);
	}

	protected MessageProcessor<Integer> getQosProcessor() {
		return this.qosProcessor;
	}

	/**
	 * Set the retained boolean for messages if the
	 * {@link #setRetainedExpression(Expression) retainedExpression} evaluates to null.
	 * Only applies if a message converter is not provided.
	 * @param defaultRetained the default defaultRetained.
	 * @see #setConverter(MessageConverter)
	 */
	public void setDefaultRetained(boolean defaultRetained) {
		this.defaultRetained = defaultRetained;
	}

	protected boolean getDefaultRetained() {
		return this.defaultRetained;
	}

	/**
	 * Set the retained expression; default "headers['mqtt_retained']".
	 * Only applies if a message converter is not provided.
	 * @param retainedExpression the expression.
	 * @since 5.0
	 * @see #setConverter(MessageConverter)
	 */
	public void setRetainedExpression(Expression retainedExpression) {
		Assert.notNull(retainedExpression, "'qosExpression' cannot be null");
		this.retainedProcessor = new ExpressionEvaluatingMessageProcessor<>(retainedExpression);
	}

	/**
	 * Set the retained expression; default "headers['mqtt_retained']".
	 * Only applies if a message converter is not provided.
	 * @param retainedExpression the expression.
	 * @since 5.0
	 * @see #setConverter(MessageConverter)
	 */
	public void setRetainedExpressionString(String retainedExpression) {
		Assert.hasText(retainedExpression, "'qosExpression' must not be null or empty");
		this.retainedProcessor = new ExpressionEvaluatingMessageProcessor<>(retainedExpression);
	}

	protected MessageProcessor<Boolean> getRetainedProcessor() {
		return this.retainedProcessor;
	}

	/**
	 * Set the message converter to use; if this is provided, the adapter qos and retained
	 * settings are ignored.
	 * @param converter the converter.
	 */
	public void setConverter(MessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected MessageConverter getConverter() {
		return this.converter;
	}

	@Nullable
	protected String getUrl() {
		return this.url;
	}

	@Nullable
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
		this.clientInstance++;
	}

	/**
	 * Set the completion timeout for async operations. Not settable using the namespace.
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
	 * Set the completion timeout when disconnecting. Not settable using the namespace.
	 * Default {@value #DISCONNECT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 5.1.10
	 */
	public void setDisconnectCompletionTimeout(long completionTimeout) {
		this.disconnectCompletionTimeout = completionTimeout;
	}

	protected long getDisconnectCompletionTimeout() {
		return this.disconnectCompletionTimeout;
	}

	@Nullable
	protected ClientManager<T, C> getClientManager() {
		return this.clientManager;
	}

	/**
	 * Set to true if you don't want to block when sending messages. Default false.
	 * When true, message sent/delivered events will be published for reception
	 * by a suitably configured 'ApplicationListener' or an event
	 * inbound-channel-adapter.
	 * @param async true for async.
	 * @see #setAsyncEvents(boolean)
	 */
	public void setAsync(boolean async) {
		this.async = async;
	}

	protected boolean isAsync() {
		return this.async;
	}

	/**
	 * When {@link #setAsync(boolean)} is true, setting this to true enables
	 * publication of {@link MqttMessageSentEvent} and {@link MqttMessageDeliveredEvent}
	 * to be emitted. Default false.
	 * @param asyncEvents the asyncEvents.
	 */
	public void setAsyncEvents(boolean asyncEvents) {
		this.asyncEvents = asyncEvents;
	}

	@Override
	protected void onInit() {
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			if (this.topicProcessor instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(beanFactory);
			}
			if (this.qosProcessor instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(beanFactory);
			}
			if (this.retainedProcessor instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(beanFactory);
			}
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
	protected void handleMessageInternal(Message<?> message) {
		Object mqttMessage = this.converter.fromMessage(message, Object.class);
		String topic = this.topicProcessor.processMessage(message);
		if (topic == null) {
			topic = this.defaultTopic;
		}

		Assert.state(topic != null, "No topic could be determined from the message and no default topic defined");

		publish(topic, mqttMessage, message);
	}

	protected void messageSentEvent(Message<?> message, String topic, int messageId) {
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (this.async && this.asyncEvents && applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(
					new MqttMessageSentEvent(this, message, topic, messageId, getClientId(),
							getClientInstance()));
		}
	}

	protected void sendDeliveryCompleteEvent(int messageId) {
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (this.async && this.asyncEvents && applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(
					new MqttMessageDeliveredEvent(this, messageId, getClientId(), getClientInstance()));
		}
	}

	protected void sendFailedDeliveryEvent(int messageId, Throwable exception) {
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (this.async && this.asyncEvents && applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(
					new MqttMessageNotDeliveredEvent(this, messageId, getClientId(), getClientInstance(), exception));
		}
	}

	protected abstract void publish(String topic, Object mqttMessage, Message<?> message);

}
