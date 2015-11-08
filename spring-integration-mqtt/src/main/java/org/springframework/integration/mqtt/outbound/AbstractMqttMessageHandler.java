/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT outbound channel adapters.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public abstract class AbstractMqttMessageHandler extends AbstractMessageHandler implements Lifecycle {

	private final String url;

	private final String clientId;

	private volatile String defaultTopic;

	private volatile int defaultQos = 0;

	private volatile boolean defaultRetained = false;

	private volatile MessageConverter converter;

	private boolean running;

	private volatile int clientInstance;

	public AbstractMqttMessageHandler(String url, String clientId) {
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		this.url = url;
		this.clientId = clientId;
	}

	public void setDefaultTopic(String defaultTopic) {
		this.defaultTopic = defaultTopic;
	}

	public void setDefaultQos(int defaultQos) {
		this.defaultQos = defaultQos;
	}

	public void setDefaultRetained(boolean defaultRetain) {
		this.defaultRetained = defaultRetain;
	}

	public void setConverter(MessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected MessageConverter getConverter() {
		return converter;
	}

	protected String getUrl() {
		return url;
	}

	public String getClientId() {
		return clientId;
	}

	/**
	 * Incremented each time the client is connected.
	 * @return The instance;
	 * @since 4.1
	 */
	public int getClientInstance() {
		return clientInstance;
	}

	@Override
	public String getComponentType() {
		return "mqtt:outbound-channel-adapter";
	}

	protected void incrementClientInstance() {
		this.clientInstance++;//NOSONAR - false positive - called from synchronized block
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.converter == null) {
			this.converter = new DefaultPahoMessageConverter(this.defaultQos, this.defaultRetained);
		}
	}

	@Override
	public final void start() {
		this.doStart();
		this.running = true;
	}

	protected abstract void doStart();

	@Override
	public final void stop() {
		this.doStop();
		this.running = false;
	}

	protected abstract void doStop();

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void handleMessageInternal(Message<?> message) throws Exception {
		connectIfNeeded();
		String topic = (String) message.getHeaders().get(MqttHeaders.TOPIC);
		Object mqttMessage = this.converter.fromMessage(message, Object.class);
		if (topic == null && this.defaultTopic == null) {
			throw new MessageHandlingException(message,
					"No '" + MqttHeaders.TOPIC + "' header and no default topic defined");
		}
		this.publish(topic == null ? this.defaultTopic : topic, mqttMessage, message);
	}

	/**
	 * Invoked before {@link #publish(String, Object, Message)}.
	 * @deprecated subclasses should check the connection in
	 * {@link #publish(String, Object, Message)}.
	 */
	@Deprecated
	protected void connectIfNeeded() {
	}

	protected abstract void publish(String topic, Object mqttMessage, Message<?> message) throws Exception;

}
