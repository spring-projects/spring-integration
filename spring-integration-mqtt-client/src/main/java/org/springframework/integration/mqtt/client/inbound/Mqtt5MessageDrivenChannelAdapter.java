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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.Mqtt5HeaderMapper;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMqttMessageDrivenChannelAdapter} implementation for MQTT v5.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt5MessageDrivenChannelAdapter extends AbstractMqttMessageDrivenChannelAdapter<Mqtt5Client> {

	private HeaderMapper<Mqtt5Publish> headerMapper = new Mqtt5HeaderMapper();

	private boolean noLocal = Mqtt5Subscription.DEFAULT_NO_LOCAL;

	private Mqtt5RetainHandling retainHandling = Mqtt5Subscription.DEFAULT_RETAIN_HANDLING;

	private boolean retainAsPublished = Mqtt5Subscription.DEFAULT_RETAIN_AS_PUBLISHED;

	public Mqtt5MessageDrivenChannelAdapter(ClientManager<Mqtt5Client> mqttClientManager, String topic) {
		super(mqttClientManager, topic);
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
	public void onClientConnected(MqttClientConnectedContext context) {
		// this adapter may not active yet when triggered from ClientManager, so subscriptions are needed in doStart.
		// Retain this code to handle scenarios where initial connection fails but later reconnection succeeds.
		if (isActive() && !this.isSubscribed.compareAndSet(false, true)) {
			subscribe();
		}
	}

	private void subscribe() {
		Mqtt5Subscribe mqtt5Subscribe = Mqtt5Subscribe.builder()
				.topicFilter(this.topic)
				.qos(this.qos)
				.noLocal(this.noLocal)
				.retainHandling(this.retainHandling)
				.retainAsPublished(this.retainAsPublished)
				.build();
		// since subscribe method is called from the onConnected callback,
		// to avoid Netty thread freeze, do not use blocking subscribe.
		CompletableFuture<Mqtt5SubAck> subscribeFuture;
		if (this.executor != null) {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt5Subscribe, this::processMessage, this.executor, this.manualAck);
		}
		else {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt5Subscribe, this::processMessage, this.manualAck);
		}
		subscribeFuture.whenComplete(((mqtt5SubAck, throwable) -> {
			if (throwable == null) {
				this.isSubscribed.set(true);
				String msg = "MQTT client subscribe topic: " + this.topic;
				this.applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, msg));
			}
			else {
				this.isSubscribed.set(false);
				logger.error(throwable, "MQTT client failed to subscribe topic: " + this.topic);
				this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
			}
		}));
	}

	private void processMessage(Mqtt5Publish mqtt5Publish) {
		Map<String, Object> headers = this.headerMapper.toHeaders(mqtt5Publish);

		headers.put(MqttHeaders.RECEIVED_QOS, mqtt5Publish.getQos());
		headers.put(MqttHeaders.RECEIVED_RETAINED, mqtt5Publish.isRetain());
		headers.put(MqttHeaders.RECEIVED_TOPIC, mqtt5Publish.getTopic().toString());

		if (this.manualAck) {
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
		Mqtt5Unsubscribe mqtt5Unsubscribe = Mqtt5Unsubscribe.builder()
				.topicFilter(this.topic)
				.build();
		this.mqttClient.toAsync().unsubscribe(mqtt5Unsubscribe).whenComplete((mqtt5UnsubAck, throwable) -> {
			if (throwable == null) {
				this.isSubscribed.set(false);
			}
			else {
				this.isSubscribed.set(true);
				logger.error(throwable, () -> "Error unsubscribing from " + this.topic);
			}
		});
	}

}
