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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hivemq.client.internal.mqtt.message.connect.MqttConnect;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.internal.mqtt.message.subscribe.MqttSubscription;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.springframework.integration.hivemq.MqttClientConnectionCoordinators;
import com.springframework.integration.hivemq.event.MqttConnectionFailedEvent;
import com.springframework.integration.hivemq.event.MqttSubscribedEvent;
import com.springframework.integration.hivemq.support.Mqtt5HeaderMapper;
import com.springframework.integration.hivemq.support.MqttHeaders;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMqttMessageDrivenChannelAdapter} implementation for MQTT v5.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt5MessageDrivenChannelAdapter extends
		AbstractMqttMessageDrivenChannelAdapter<Mqtt5AsyncClient> {

	private HeaderMapper<Mqtt5Publish> headerMapper = new Mqtt5HeaderMapper();

	private MqttConnect mqttConnect = MqttConnect.DEFAULT;

	private MqttDisconnect mqttDisConnect = MqttDisconnect.DEFAULT;

	// [Start] Additional MQTT v5 subscription Options

	private boolean noLocal = MqttSubscription.DEFAULT_NO_LOCAL;

	private Mqtt5RetainHandling retainHandling = MqttSubscription.DEFAULT_RETAIN_HANDLING;

	private boolean retainAsPublished = MqttSubscription.DEFAULT_RETAIN_AS_PUBLISHED;

	// [End]

	public Mqtt5MessageDrivenChannelAdapter(Mqtt5AsyncClient mqttClient, String topic) {
		super(mqttClient, topic);
	}

	@Override
	protected void onInit() {
		super.onInit();
	}

	/**
	 * Set the HeaderMapper to map the {@code Mqtt5Publish} optional data
	 * @param headerMapper the headMapper
	 */
	public void setHeaderMapper(HeaderMapper<Mqtt5Publish> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the Connect message.
	 * @param mqttConnect the mqttConnect
	 */
	public void setMqttConnect(MqttConnect mqttConnect) {
		Assert.notNull(mqttConnect, "'mqttConnect' must not be null.");
		this.mqttConnect = mqttConnect;
	}

	/**
	 * Set whether the client must not receive messages published by itself.
	 * @param noLocal whether the subscription is not local.
	 */
	public void setNoLocal(boolean noLocal) {
		this.noLocal = noLocal;
	}

	/**
	 * Set the handling of retained message for this Subscription.
	 * @param retainHandling the retain handling.
	 */
	public void setRetainHandling(Mqtt5RetainHandling retainHandling) {
		Assert.notNull(retainHandling, "'retainHandling' must not be null.");
		this.retainHandling = retainHandling;
	}

	/**
	 * Set whether the retain flag for incoming Publish messages must be set to its original value.
	 * @param retainAsPublished the retainAsPublished.
	 */
	public void setRetainAsPublished(boolean retainAsPublished) {
		this.retainAsPublished = retainAsPublished;
	}

	/**
	 * Set the Disconnect message.
	 * @param mqttDisconnect the mqttDisconnect
	 */
	public void setMqttDisconnect(MqttDisconnect mqttDisconnect) {
		Assert.notNull(mqttDisconnect, "'mqttDisconnect' must not be null.");
		this.mqttDisConnect = mqttDisconnect;
	}

	@Override
	protected void doStart() {
		super.doStart();
		MqttClientConnectionCoordinators.mqtt5AsyncClient().connect(mqttClient, this.mqttConnect)
				.whenComplete((connAck, throwable) -> {
					if (throwable != null) {
						MqttConnectionFailedEvent event = new MqttConnectionFailedEvent(this, throwable);
						applicationEventPublisher.publishEvent(event);
						logger.error(throwable, "MQTT client failed to connect. " + throwable.getMessage());
					}
					else {
						subscribe();
					}
				});
	}

	@Override
	protected void doStop() {
		super.doStop();
		MqttClientConnectionCoordinators.mqtt5AsyncClient().disconnect(mqttClient, this.mqttDisConnect)
				.whenComplete((unused, throwable) -> {
					if (throwable != null) {
						logger.error(throwable, "MQTT client failed to disconnect. " + throwable.getMessage());
					}
				});
	}

	private void subscribe() {
		Mqtt5Subscribe mqtt5Subscribe = Mqtt5Subscribe.builder()
				.topicFilter(topic)
				.qos(qos)
				.noLocal(this.noLocal)
				.retainHandling(this.retainHandling)
				.retainAsPublished(this.retainAsPublished)
				.build();
		CompletableFuture<Mqtt5SubAck> subscribeFuture;
		if (executor != null) {
			subscribeFuture = mqttClient.subscribe(mqtt5Subscribe, this::messageListener, executor, manualAck);
		}
		else {
			subscribeFuture = mqttClient.subscribe(mqtt5Subscribe, this::messageListener, manualAck);
		}
		subscribeFuture.whenComplete((subAck, throwable) -> {
			if (throwable != null) {
				logger.error(throwable, "MQTT client failed to subscribe topic: " + topic);
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
			}
			else {
				String msg = "MQTT client subscribe topic: " + topic;
				logger.debug(msg);
				applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, msg));
			}
		});
	}

	private void messageListener(Mqtt5Publish mqtt5Publish) {
		Map<String, Object> headers = this.headerMapper.toHeaders(mqtt5Publish);

		headers.put(MqttHeaders.RECEIVED_QOS, mqtt5Publish.getQos());
		headers.put(MqttHeaders.RECEIVED_RETAINED, mqtt5Publish.isRetain());
		headers.put(MqttHeaders.RECEIVED_TOPIC, mqtt5Publish.getTopic().toString());

		if (manualAck) {
			headers.put(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, mqtt5Publish);
		}

		Object payload = Mqtt5Publish.class.isAssignableFrom(this.payloadType)
				? mqtt5Publish
				: mqtt5Publish.getPayloadAsBytes();

		Message<?> message;
		if (Mqtt5Publish.class.isAssignableFrom(this.payloadType) || byte[].class.isAssignableFrom(this.payloadType)) {
			message = new GenericMessage<>(payload, headers);
		}
		else {
			message = this.messageConverter.toMessage(payload, new MessageHeaders(headers), this.payloadType);
		}

		sendMessage(message);
	}

}
