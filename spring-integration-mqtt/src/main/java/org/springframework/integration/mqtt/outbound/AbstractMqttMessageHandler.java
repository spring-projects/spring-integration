/*
 * Copyright 2002-2013 the original author or authors.
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

import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT outbound channel adapters.
 * @author Gary Russell
 * @since 1.0
 *
 */
public abstract class AbstractMqttMessageHandler extends AbstractMessageHandler implements SmartLifecycle {

	private final String url;

	private final String clientId;

	private volatile String defaultTopic;

	private volatile int defaultQos = 0;

	private volatile boolean defaultRetained = false;

	private volatile MqttMessageConverter converter;

	private boolean running;

	private volatile int phase;

	private volatile boolean autoStartup;

	public AbstractMqttMessageHandler(String url, String clientId) {
		Assert.hasText(url, "'url' cannot be null or empty");
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

	public void setConverter(MqttMessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected String getUrl() {
		return url;
	}

	protected String getClientId() {
		return clientId;
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
	}

	protected abstract void doStart();

	@Override
	public final void stop() {
		this.doStop();
	}

	protected abstract void doStop();

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		this.connectIfNeeded();
		String topic = (String) message.getHeaders().get(MqttHeaders.TOPIC);
		MqttMessage mqttMessage = (MqttMessage) this.converter.fromMessage(message, MqttMessage.class);
		if (topic == null && this.defaultTopic == null) {
			throw new MessageHandlingException(message,
					"No '" + MqttHeaders.TOPIC + "' header and no default topic defined");
		}
		this.publish(topic == null ? this.defaultTopic : topic, mqttMessage);
	}

	protected abstract void connectIfNeeded();

	protected abstract void publish(String topic, Object mqttMessage) throws Exception;

	@Override
	public String getComponentType() {
		return "mqtt:outbound-channel-adapter";
	}

}
