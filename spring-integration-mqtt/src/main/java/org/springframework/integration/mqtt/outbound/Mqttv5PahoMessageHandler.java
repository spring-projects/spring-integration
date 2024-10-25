/*
 * Copyright 2021-2024 the original author or authors.
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

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.core.MqttComponent;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttProtocolErrorEvent;
import org.springframework.integration.mqtt.support.MqttHeaderMapper;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.mqtt.support.MqttUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMqttMessageHandler} implementation for MQTT v5.
 *
 * @author Artem Bilan
 * @author Lucas Bowler
 * @author Artem Vozhdayenko
 *
 * @since 5.5.5
 */
public class Mqttv5PahoMessageHandler extends AbstractMqttMessageHandler<IMqttAsyncClient, MqttConnectionOptions>
		implements MqttCallback, MqttComponent<MqttConnectionOptions> {

	private final MqttConnectionOptions connectionOptions;

	private final MqttActionListener mqttPublishActionListener = new MqttPublishActionListener();

	private IMqttAsyncClient mqttClient;

	@Nullable
	private MqttClientPersistence persistence;

	private HeaderMapper<MqttProperties> headerMapper = new MqttHeaderMapper();

	public Mqttv5PahoMessageHandler(String url, String clientId) {
		super(url, clientId);
		Assert.hasText(url, "'url' cannot be null or empty");
		this.connectionOptions = new MqttConnectionOptions();
		this.connectionOptions.setServerURIs(new String[] {url});
		this.connectionOptions.setAutomaticReconnect(true);
	}

	public Mqttv5PahoMessageHandler(MqttConnectionOptions connectionOptions, String clientId) {
		super(obtainServerUrlFromOptions(connectionOptions), clientId);
		this.connectionOptions = connectionOptions;
	}

	/**
	 * Use this constructor when you need to use a single {@link ClientManager}
	 * (for instance, to reuse an MQTT connection).
	 * @param clientManager The client manager.
	 * @since 6.0
	 */
	public Mqttv5PahoMessageHandler(ClientManager<IMqttAsyncClient, MqttConnectionOptions> clientManager) {
		super(clientManager);
		this.connectionOptions = clientManager.getConnectionInfo();
	}

	private static String obtainServerUrlFromOptions(MqttConnectionOptions connectionOptions) {
		Assert.notNull(connectionOptions, "'connectionOptions' must not be null");
		String[] serverURIs = connectionOptions.getServerURIs();
		Assert.notEmpty(serverURIs, "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		return serverURIs[0];
	}

	@Override
	public MqttConnectionOptions getConnectionInfo() {
		return this.connectionOptions;
	}

	public void setPersistence(@Nullable MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	public void setHeaderMapper(HeaderMapper<MqttProperties> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null");
		this.headerMapper = headerMapper;
	}

	@Override
	protected void onInit() {
		super.onInit();
		try {
			if (getClientManager() == null) {
				this.mqttClient = new MqttAsyncClient(getUrl(), getClientId(), this.persistence);
				this.mqttClient.setCallback(this);
				incrementClientInstance();
			}
		}
		catch (MqttException ex) {
			throw new BeanCreationException("Cannot create 'MqttAsyncClient' for: " + getComponentName(), ex);
		}
		if (getConverter() == null) {
			setConverter(getBeanFactory()
					.getBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
							MessageConverter.class));
		}
		else {
			Assert.state(!(getConverter() instanceof MqttMessageConverter),
					"MessageConverter must not be an MqttMessageConverter");
		}
	}

	@Override
	protected void doStart() {
		try {
			var clientManager = getClientManager();
			if (clientManager != null) {
				this.mqttClient = clientManager.getClient();
			}
			else {
				this.mqttClient.connect(this.connectionOptions).waitForCompletion(getCompletionTimeout());
			}
		}
		catch (MqttException ex) {
			logger.error(ex, "MQTT client failed to connect.");
		}
	}

	@Override
	protected void doStop() {
		try {
			if (getClientManager() == null) {
				this.mqttClient.disconnect().waitForCompletion(getDisconnectCompletionTimeout());
				if (getConnectionInfo().isAutomaticReconnect()) {
					MqttUtils.stopClientReconnectCycle(this.mqttClient);
				}
			}
		}
		catch (MqttException ex) {
			logger.error(ex, "Failed to disconnect 'MqttAsyncClient'");
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			if (getClientManager() == null) {
				this.mqttClient.close(true);
			}
		}
		catch (MqttException ex) {
			logger.error(ex, "Failed to close 'MqttAsyncClient'");
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		MqttMessage mqttMessage;
		Object payload = message.getPayload();
		if (payload instanceof MqttMessage) {
			mqttMessage = (MqttMessage) payload;
		}
		else {
			mqttMessage = buildMqttMessage(message);
		}

		publish(obtainTopicToPublish(message), mqttMessage, message);
	}

	private String obtainTopicToPublish(Message<?> message) {
		String topic = getTopicProcessor().processMessage(message);
		if (topic == null) {
			topic = getDefaultTopic();
		}
		Assert.state(topic != null,
				() -> "No topic could be determined from the '" + message + "' and no default topic defined");
		return topic;
	}

	private MqttMessage buildMqttMessage(Message<?> message) {
		Object payload = message.getPayload();
		byte[] body;
		if (payload instanceof byte[]) {
			body = (byte[]) payload;
		}
		else if (payload instanceof String) {
			body = ((String) payload).getBytes(StandardCharsets.UTF_8);
		}
		else {
			MessageConverter converter = getConverter();
			body = (byte[]) converter.fromMessage(message, byte[].class);
			Assert.state(body != null,
					() -> "The MQTT payload cannot be null. The '" + converter + "' returned null for: " + message);
		}

		MqttMessage mqttMessage = new MqttMessage();
		mqttMessage.setPayload(body);
		Integer qos = getQosProcessor().processMessage(message);
		mqttMessage.setQos(qos == null ? getDefaultQos() : qos);
		Boolean retained = getRetainedProcessor().processMessage(message);
		mqttMessage.setRetained(retained == null ? getDefaultRetained() : retained);
		MqttProperties properties = new MqttProperties();
		this.headerMapper.fromHeaders(message.getHeaders(), properties);
		mqttMessage.setProperties(properties);
		return mqttMessage;
	}

	@Override
	protected void publish(String topic, Object mqttMessage, Message<?> message) {
		Assert.isInstanceOf(MqttMessage.class, mqttMessage, "The 'mqttMessage' must be an instance of 'MqttMessage'");
		long completionTimeout = getCompletionTimeout();
		try {
			if (!this.mqttClient.isConnected()) {
				this.lock.lock();
				try {
					if (!this.mqttClient.isConnected()) {
						this.mqttClient.connect(this.connectionOptions).waitForCompletion(completionTimeout);
					}
				}
				finally {
					this.lock.unlock();
				}
			}
			IMqttToken token =
					this.mqttClient.publish(topic, (MqttMessage) mqttMessage, null, this.mqttPublishActionListener);
			if (!isAsync()) {
				token.waitForCompletion(completionTimeout); // NOSONAR (sync)
			}
			else {
				messageSentEvent(message, topic, token.getMessageId());
			}
		}
		catch (MqttException ex) {
			throw new MessageHandlingException(message, "Failed to publish to MQTT in the [" + this + ']', ex);
		}
	}

	@Override
	public void deliveryComplete(IMqttToken token) {

	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		MqttException cause = disconnectResponse.getException();
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, cause));
		}
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(new MqttProtocolErrorEvent(this, exception));
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {

	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {

	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {

	}

	private final class MqttPublishActionListener implements MqttActionListener {

		MqttPublishActionListener() {
		}

		@Override
		public void onSuccess(IMqttToken asyncActionToken) {
			sendDeliveryCompleteEvent(asyncActionToken.getMessageId());
		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			sendFailedDeliveryEvent(asyncActionToken.getMessageId(), exception);
		}

	}

}
