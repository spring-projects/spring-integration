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

package com.springframework.integration.hivemq.inbound;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import com.springframework.integration.hivemq.MqttClientConnectionCoordinators;
import com.springframework.integration.hivemq.event.MqttConnectionFailedEvent;
import com.springframework.integration.hivemq.event.MqttSubscribedEvent;
import com.springframework.integration.hivemq.support.MqttHeaders;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMqttMessageDrivenChannelAdapter} implementation for MQTT v3.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt3MessageDrivenChannelAdapter extends
		AbstractMqttMessageDrivenChannelAdapter<Mqtt3AsyncClient> {

	private Mqtt3ConnectView mqtt3ConnectView = Mqtt3ConnectView.DEFAULT;

	public Mqtt3MessageDrivenChannelAdapter(Mqtt3AsyncClient mqttClient, String topic) {
		super(mqttClient, topic);
	}

	/**
	 * Set the Connect message.
	 * @param mqtt3ConnectView the mqtt3ConnectView
	 */
	public void setMqtt3ConnectView(Mqtt3ConnectView mqtt3ConnectView) {
		Assert.notNull(mqtt3ConnectView, "'mqtt3ConnectView' must not be null");
		this.mqtt3ConnectView = mqtt3ConnectView;
	}

	@Override
	protected void onInit() {
		super.onInit();
	}

	@Override
	protected void doStart() {
		super.doStart();
		MqttClientConnectionCoordinators.mqtt3AsyncClient().connect(mqttClient, this.mqtt3ConnectView)
				.whenComplete((connAck, throwable) -> {
					if (throwable != null) {
						applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
						logger.error(throwable, "MQTT client failed to connect. " + mqttClient);
					}
					else {
						subscribe();
					}
				});
	}

	@Override
	protected void doStop() {
		super.doStop();
		MqttClientConnectionCoordinators.mqtt3AsyncClient().disconnect(mqttClient, null)
				.whenComplete((unused, throwable) -> {
					if (throwable != null) {
						logger.error(throwable, "MQTT client failed to disconnect." + mqttClient);
					}
				});
	}

	private void subscribe() {
		Mqtt3Subscribe mqtt3Subscribe = Mqtt3Subscribe.builder()
				.topicFilter(topic)
				.qos(qos)
				.build();
		CompletableFuture<Mqtt3SubAck> subscribeFuture;
		if (executor != null) {
			subscribeFuture = mqttClient.subscribe(mqtt3Subscribe, this::messageListener, executor, manualAck);
		}
		else {
			subscribeFuture = mqttClient.subscribe(mqtt3Subscribe, this::messageListener, manualAck);
		}
		subscribeFuture.whenComplete((subAck, throwable) -> {
			if (throwable != null) {
				logger.error(throwable, "MQTT client failed to subscribe topic : " + topic);
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
			}
			else {
				String msg = "MQTT client subscribe topic: " + topic;
				logger.debug(msg);
				applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, msg));
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
			message = this.messageConverter.toMessage(payload, new MessageHeaders(headers), this.payloadType);
		}

		sendMessage(message);
	}

}
