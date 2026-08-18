/*
 * Copyright 2026-present the original author or authors.
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

package com.springframework.integration.mqtt.client.inbound;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import com.springframework.integration.mqtt.client.core.MqttClientManager;
import com.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;
import com.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import com.springframework.integration.mqtt.client.support.MqttClientBuilderHelper;
import com.springframework.integration.mqtt.client.support.MqttHeaders;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMqttMessageDrivenChannelAdapter} implementation for MQTT v3.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt3MessageDrivenChannelAdapter
		extends AbstractMqttMessageDrivenChannelAdapter<Mqtt3Client, Mqtt3ClientBuilder>
		implements MqttClientConnectedListener {

	private Mqtt3Connect mqttConnect = Mqtt3ConnectView.DEFAULT;

	public Mqtt3MessageDrivenChannelAdapter(Mqtt3ClientBuilder mqttClientBuilder, String topic) {
		super(mqttClientBuilder, topic);
		this.mqttClient = MqttClientBuilderHelper.clone(mqttClientBuilder)
				.addConnectedListener(Mqtt3MessageDrivenChannelAdapter.this)
				.build();

		if (this.mqttClient.getConfig().getAutomaticReconnect().isEmpty()) {
			logger.warn("it is recommended to enable 'automaticReconnect' when set this `mqttClient`. " +
					"Otherwise connection check and reconnect should be done manually." +
					"e.g. via handling 'MqttConnectionFailedEvent' on client disconnection.");
		}
	}

	public Mqtt3MessageDrivenChannelAdapter(MqttClientManager<Mqtt3Client> mqttClientManager, String topic) {
		super(mqttClientManager, topic);
		this.mqttClient = mqttClientManager.getClient();
	}

	/**
	 * Set the Connect message.
	 * @param mqttConnect the mqttConnect
	 */
	public void setMqttConnect(Mqtt3Connect mqttConnect) {
		Assert.notNull(mqttConnect, "'mqttConnect' must not be null");
		this.mqttConnect = mqttConnect;
	}

	@Override
	protected void onInit() {
		super.onInit();
	}

	@Override
	protected void doStart() {
		super.doStart();
		if (this.mqttClientBuilder != null && !this.isConnected()) {
			try {
				this.mqttClient.toBlocking().connect(this.mqttConnect);
			}
			catch (RuntimeException ex) {
				this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
				logger.error(ex, "MQTT client failed to connect.");
			}
		}
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.mqttClientBuilder != null && this.isConnected()) {
			try {
				this.mqttClient.toBlocking().disconnect();
			}
			catch (RuntimeException ex) {
				logger.error(ex, "Could not disconnect from the client");
			}
		}
	}

	@Override
	public void onConnected(MqttClientConnectedContext context) {
		onClientConnected(context);
	}

	@Override
	public void onClientConnected(MqttClientConnectedContext mqttClientConnectedContext) {
		if (isActive() && !this.isSubscribed) {
			subscribe();
		}
	}

	private void subscribe() {
		Mqtt3Subscribe mqtt3Subscribe = Mqtt3Subscribe.builder()
				.topicFilter(topic)
				.qos(qos)
				.build();
		// since subscribe method is called from the onConnected callback,
		// to avoid Netty thread freeze, do not use blocking subscribe.
		CompletableFuture<Mqtt3SubAck> subscribeFuture;
		if (executor != null) {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt3Subscribe, this::messageListener, this.executor, this.manualAck);
		}
		else {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt3Subscribe, this::messageListener, this.manualAck);
		}
		subscribeFuture.whenComplete((subAck, throwable) -> {
			if (throwable == null) {
				this.isSubscribed = true;
				String msg = "MQTT client subscribe topic: " + topic;
				applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, msg));
			}
			else {
				this.isSubscribed = false;
				logger.error(throwable, "MQTT client failed to subscribe topic : " + topic);
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
			}
		});
	}

	private void messageListener(Mqtt3Publish mqttMessage) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(MqttHeaders.RECEIVED_QOS, mqttMessage.getQos());
		headers.put(MqttHeaders.RECEIVED_RETAINED, mqttMessage.isRetain());
		headers.put(MqttHeaders.RECEIVED_TOPIC, mqttMessage.getTopic().toString());

		if (manualAck) {
			headers.put(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, mqttMessage);
		}

		Object payload = Mqtt3Publish.class.isAssignableFrom(this.payloadType)
				? mqttMessage
				: mqttMessage.getPayloadAsBytes();

		Message<?> message;
		if (Mqtt3Publish.class.isAssignableFrom(this.payloadType) || byte[].class.isAssignableFrom(this.payloadType)) {
			message = new GenericMessage<>(payload, headers);
		}
		else {
			Message<?> messageToConvert = new GenericMessage<>(payload, headers);
			Object convertedPayload = this.messageConverter.fromMessage(messageToConvert, this.payloadType);
			if (convertedPayload == null) {
				throw new MessageConversionException(messageToConvert, "Failed to convert from MQTT Message");
			}
			message = new GenericMessage<>(convertedPayload, headers);
		}

		sendMessage(message);
	}

}
