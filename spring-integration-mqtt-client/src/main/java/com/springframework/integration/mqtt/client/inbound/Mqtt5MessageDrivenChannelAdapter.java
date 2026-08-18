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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.hivemq.client.internal.mqtt.message.connect.MqttConnect;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.internal.mqtt.message.subscribe.MqttSubscription;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.springframework.integration.mqtt.client.core.MqttClientManager;
import com.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;
import com.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import com.springframework.integration.mqtt.client.support.Mqtt5HeaderMapper;
import com.springframework.integration.mqtt.client.support.MqttClientBuilderHelper;
import com.springframework.integration.mqtt.client.support.MqttHeaders;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.mapping.HeaderMapper;
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
public class Mqtt5MessageDrivenChannelAdapter
		extends AbstractMqttMessageDrivenChannelAdapter<Mqtt5Client, Mqtt5ClientBuilder>
		implements MqttClientConnectedListener {

	private HeaderMapper<Mqtt5Publish> headerMapper = new Mqtt5HeaderMapper();

	private Mqtt5Connect mqttConnect = MqttConnect.DEFAULT;

	private Mqtt5Disconnect mqttDisConnect = MqttDisconnect.DEFAULT;

	// [Start] Additional MQTT v5 subscription options

	private boolean noLocal = MqttSubscription.DEFAULT_NO_LOCAL;

	private Mqtt5RetainHandling retainHandling = MqttSubscription.DEFAULT_RETAIN_HANDLING;

	private boolean retainAsPublished = MqttSubscription.DEFAULT_RETAIN_AS_PUBLISHED;

	// [End]

	@SuppressWarnings("this-escape")
	public Mqtt5MessageDrivenChannelAdapter(Mqtt5ClientBuilder mqttClientBuilder, String topic) {
		super(mqttClientBuilder, topic);
		this.mqttClient = MqttClientBuilderHelper.clone(mqttClientBuilder)
				.addConnectedListener(Mqtt5MessageDrivenChannelAdapter.this)
				.build();

		if (this.mqttClient.getConfig().getAutomaticReconnect().isEmpty()) {
			logger.warn("it is recommended to enable 'automaticReconnect' when set this `mqttClient`. " +
					"Otherwise connection check and reconnect should be done manually." +
					"e.g. via handling 'MqttConnectionFailedEvent' on client disconnection.");
		}
	}

	public Mqtt5MessageDrivenChannelAdapter(MqttClientManager<Mqtt5Client> mqttClientManager, String topic) {
		super(mqttClientManager, topic);
		this.mqttClient = mqttClientManager.getClient();
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
	public void setMqttConnect(Mqtt5Connect mqttConnect) {
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
	public void setMqttDisconnect(Mqtt5Disconnect mqttDisconnect) {
		Assert.notNull(mqttDisconnect, "'mqttDisconnect' must not be null.");
		this.mqttDisConnect = mqttDisconnect;
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
				this.mqttClient.toBlocking().disconnect(this.mqttDisConnect);
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
	public void onClientConnected(MqttClientConnectedContext context) {
		if (isActive() && !this.isSubscribed) {
			subscribe();
		}
	}

	private void subscribe() {
		Mqtt5Subscribe mqtt5Subscribe = Mqtt5Subscribe.builder()
				.topicFilter(topic)
				.qos(qos)
				.noLocal(this.noLocal)
				.retainHandling(this.retainHandling)
				.retainAsPublished(this.retainAsPublished)
				.build();
		// since subscribe method is called from the onConnected callback,
		// to avoid Netty thread freeze, do not use blocking subscribe.
		CompletableFuture<Mqtt5SubAck> subscribeFuture;
		if (executor != null) {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt5Subscribe, this::messageListener, this.executor, this.manualAck);
		}
		else {
			subscribeFuture = this.mqttClient.toAsync()
					.subscribe(mqtt5Subscribe, this::messageListener, this.manualAck);
		}
		subscribeFuture.whenComplete(((mqtt5SubAck, throwable) -> {
			if (throwable == null) {
				this.isSubscribed = true;
				String msg = "MQTT client subscribe topic: " + topic;
				applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, msg));
			}
			else {
				this.isSubscribed = false;
				logger.error(throwable, "MQTT client failed to subscribe topic: " + topic);
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, throwable));
			}
		}));
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
