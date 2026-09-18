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

package org.springframework.integration.mqtt.client.inbound;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import com.hivemq.client.mqtt.mqtt3.message.unsubscribe.Mqtt3Unsubscribe;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
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
public class Mqtt3MessageDrivenChannelAdapter extends AbstractMqttMessageDrivenChannelAdapter<Mqtt3Client> {

	@SuppressWarnings("NullAway.Init")
	private List<Mqtt3Subscription> subscriptions;

	public Mqtt3MessageDrivenChannelAdapter(ClientManager<Mqtt3Client> mqttClientManager, String... topics) {
		super(mqttClientManager);
		this.topics = topics;
	}

	public Mqtt3MessageDrivenChannelAdapter(ClientManager<Mqtt3Client> mqttClientManager, Mqtt3Subscription... subscriptions) {
		super(mqttClientManager);
		this.subscriptions = Arrays.stream(subscriptions).toList();
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.subscriptions == null) {
			Assert.notEmpty(this.topics, "topics must not be empty when subscriptions are not provided");
			this.subscriptions = Arrays.stream(this.topics)
					.map(topic -> Mqtt3Subscription.builder()
							.topicFilter(topic)
							.qos(this.qos)
							.build())
					.toList();
		}
		Assert.notEmpty(this.subscriptions, "subscriptions must not be empty");
	}

	@Override
	protected void doStart() {
		super.doStart();
		if (this.isConnected() && this.isSubscribed.compareAndSet(false, true)) {
			subscribe();
		}
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.isConnected() && this.isSubscribed.compareAndSet(true, false)) {
			unsubscribe();
		}
	}

	@Override
	public void onClientConnected(MqttClientConnectedContext mqttClientConnectedContext) {
		// this adapter may not active yet when triggered from ClientManager, so subscriptions are needed in doStart.
		// Retain this code to handle scenarios where initial connection fails but later reconnection succeeds.
		if (isActive() && this.isSubscribed.compareAndSet(false, true)) {
			subscribe();
		}
	}

	private void subscribe() {
		Mqtt3Subscribe mqtt3Subscribe = Mqtt3Subscribe.builder()
				.addSubscriptions(this.subscriptions)
				.build();
		// since subscribe method is called from the onConnected callback,
		// to avoid Netty thread freeze, do not use blocking subscribe.
		CompletableFuture<Mqtt3SubAck> subscribeFuture;
		if (this.executor != null) {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt3Subscribe, this::processMessage, this.executor, this.manualAck);
		}
		else {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt3Subscribe, this::processMessage, this.manualAck);
		}
		subscribeFuture.whenComplete((subAck, throwable) -> {
			if (throwable == null) {
				this.isSubscribed.set(true);
				String msg = "MQTT client subscribe to: " + this.subscriptions;
				this.applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, msg));
			}
			else {
				this.isSubscribed.set(false);
				logger.error(throwable, "MQTT client failed to subscribe: " + this.subscriptions);
				this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
			}
		});
	}

	private void processMessage(Mqtt3Publish mqttMessage) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(MqttHeaders.RECEIVED_QOS, mqttMessage.getQos());
		headers.put(MqttHeaders.RECEIVED_RETAINED, mqttMessage.isRetain());
		headers.put(MqttHeaders.RECEIVED_TOPIC, mqttMessage.getTopic().toString());

		if (this.manualAck) {
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

	private void unsubscribe() {
		Mqtt3Unsubscribe mqtt3Unsubscribe = Mqtt3Unsubscribe.builder()
				.addTopicFilters(this.subscriptions.stream().map(Mqtt3Subscription::getTopicFilter))
				.build();
		this.mqttClient.toAsync().unsubscribe(mqtt3Unsubscribe).whenComplete((Void, throwable) -> {
			if (throwable == null) {
				this.isSubscribed.set(false);
			}
			else {
				this.isSubscribed.set(true);
				logger.error(throwable, () -> "Error unsubscribing from " + this.subscriptions);
			}
		});
	}

}
