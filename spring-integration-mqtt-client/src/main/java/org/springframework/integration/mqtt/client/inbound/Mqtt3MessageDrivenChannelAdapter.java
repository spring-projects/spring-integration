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
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.event.MqttProtocolErrorEvent;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
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
		super(mqttClientManager, topics);
	}

	public Mqtt3MessageDrivenChannelAdapter(ClientManager<Mqtt3Client> mqttClientManager, Mqtt3Subscription... subscriptions) {
		super(mqttClientManager);
		this.subscriptions = Arrays.stream(subscriptions).toList();
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.subscriptions == null) {
			Assert.notEmpty(getTopics(), "topics must not be empty when subscriptions are not provided");
			this.subscriptions = Arrays.stream(getTopics())
					.map(topic -> Mqtt3Subscription.builder()
							.topicFilter(topic)
							.qos(getQos())
							.build())
					.toList();
		}
		Assert.notEmpty(this.subscriptions, "subscriptions must not be empty");
	}

	@Override
	protected void doStart() {
		super.doStart();
		if (isConnected() && !this.isSubscribed.get() && this.isSubscribing.compareAndSet(false, true)) {
			subscribe();
		}
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (isConnected() && this.isSubscribed.get()) {
			unsubscribe();
		}
	}

	@Override
	public void onClientConnected(MqttClientConnectedContext mqttClientConnectedContext) {
		// this adapter may not active yet when triggered from ClientManager, so subscriptions are needed in doStart.
		// Retain this code to handle scenarios where initial connection fails but later reconnection succeeds.
		if (isActive() && !this.isSubscribed.get() && this.isSubscribing.compareAndSet(false, true)) {
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
		if (getExecutor() != null) {
			subscribeFuture = getClient().toAsync()
					.subscribe(mqtt3Subscribe, this::processMessage, getExecutor(), isManualAck());
		}
		else {
			subscribeFuture = getClient().toAsync()
					.subscribe(mqtt3Subscribe, this::processMessage, isManualAck());
		}
		subscribeFuture.whenComplete((subAck, throwable) -> {
			this.isSubscribing.set(false);
			if (throwable == null) {
				this.isSubscribed.set(true);
				String msg = "MQTT client subscribe to: " + this.subscriptions;
				getApplicationEventPublisher().publishEvent(new MqttSubscribedEvent(this, msg));
			}
			else {
				this.isSubscribed.set(false);
				logger.error(throwable, "MQTT client failed to subscribe: " + this.subscriptions);
				getApplicationEventPublisher().publishEvent(new MqttProtocolErrorEvent(this, throwable));
			}
		});
	}

	private void processMessage(Mqtt3Publish mqtt3Publish) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(MqttHeaders.RECEIVED_QOS, mqtt3Publish.getQos());
		headers.put(MqttHeaders.RECEIVED_RETAINED, mqtt3Publish.isRetain());
		headers.put(MqttHeaders.RECEIVED_TOPIC, mqtt3Publish.getTopic().toString());

		if (isManualAck()) {
			headers.put(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, new AcknowledgmentImpl(mqtt3Publish));
		}

		Object payload = Mqtt3Publish.class.isAssignableFrom(getPayloadType())
				? mqtt3Publish
				: mqtt3Publish.getPayloadAsBytes();

		Message<?> message;
		if (Mqtt3Publish.class.isAssignableFrom(getPayloadType()) || byte[].class.isAssignableFrom(getPayloadType())) {
			message = getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}
		else {
			Message<?> messageToConvert = MutableMessageBuilder.withPayload(payload, false)
					.copyHeaders(headers)
					.build();
			Object convertedPayload = getMessageConverter().fromMessage(messageToConvert, getPayloadType());
			if (convertedPayload == null) {
				throw new MessageConversionException(messageToConvert, "Failed to convert from MQTT Message");
			}
			message = getMessageBuilderFactory()
					.withPayload(convertedPayload)
					.copyHeaders(messageToConvert.getHeaders())
					.build();
		}

		sendMessage(message);
	}

	private void unsubscribe() {
		Mqtt3Unsubscribe mqtt3Unsubscribe = Mqtt3Unsubscribe.builder()
				.addTopicFilters(this.subscriptions.stream().map(Mqtt3Subscription::getTopicFilter))
				.build();
		getClient().toAsync()
				.unsubscribe(mqtt3Unsubscribe)
				.whenComplete((Void, throwable) -> {
					if (throwable == null) {
						this.isSubscribed.set(false);
					}
					else {
						this.isSubscribed.set(true);
						logger.error(throwable, () -> "Error unsubscribing from " + this.subscriptions);
					}
				});
	}

	/**
	 * Used to complete message arrival when {@link #isManualAck()} is true.
	 */
	private record AcknowledgmentImpl(Mqtt3Publish mqtt3Publish) implements SimpleAcknowledgment {

		@Override
		public void acknowledge() {
			this.mqtt3Publish.acknowledge();
		}

	}

}
