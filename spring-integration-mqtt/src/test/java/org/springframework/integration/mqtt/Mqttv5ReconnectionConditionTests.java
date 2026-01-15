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

package org.springframework.integration.mqtt;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the reconnect feature if {@link MqttException} has reason code of "REASON_CODE_CONNECT_IN_PROGRESS"
 * in {@link Mqttv5PahoMessageHandler}.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class Mqttv5ReconnectionConditionTests {

	@Test
	public void testConnectAfterSingleInProgress() throws Exception {
		IMqttAsyncClient mockClient = mock();
		IMqttToken mockConnectToken = mock();
		IMqttToken mockPublishToken = mock();

		AtomicInteger connectAttempts = new AtomicInteger(0);
		AtomicBoolean connectionCompleted = new AtomicBoolean(false);

		// Simulate the race condition scenario:
		// 1. Initially disconnected
		// 2. After first connect attempt (which throws), another thread completes the connection
		// 3. Subsequent isConnected() calls return true
		willAnswer(invocation -> connectionCompleted.get()).given(mockClient).isConnected();

		willAnswer(invocation -> {
			if (connectAttempts.incrementAndGet() == 1) {
				// First attempt throws "connect already in progress" (REASON_CODE_CONNECT_IN_PROGRESS)
				// Simulate another thread completing the connection
				connectionCompleted.set(true);
				throw new MqttException(MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS);
			}
			return mockConnectToken;
		}).given(mockClient).connect(any(MqttConnectionOptions.class));

		willDoNothing().given(mockConnectToken).waitForCompletion(anyLong());

		given(mockClient.publish(anyString(), any(MqttMessage.class), any(), any()))
				.willReturn(mockPublishToken);

		willDoNothing().given(mockPublishToken).waitForCompletion(anyLong());
		given(mockPublishToken.getMessageId()).willReturn(1);

		Mqttv5PahoMessageHandler handler = createHandler(mockClient);

		Message<String> message = MessageBuilder.withPayload("test").build();

		handler.handleMessage(message);
		verify(mockClient).connect(any(MqttConnectionOptions.class));
		verify(mockClient).publish(anyString(), any(MqttMessage.class), any(), any());
	}

	@Test
	public void testNonRetryableReasonCode() throws Exception {
		Mqttv5PahoMessageHandler handler = getFailConnectMessageHandler(MqttClientException.REASON_CODE_CONNECTION_LOST);

		Message<String> message = MessageBuilder.withPayload("test").build();

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> {
					handler.handleMessage(message);
				})
				.withCause(new MqttException(MqttClientException.REASON_CODE_CONNECTION_LOST));
	}

	private Mqttv5PahoMessageHandler createHandler(IMqttAsyncClient mockClient) throws Exception {
		MqttConnectionOptions connectionOptions = new MqttConnectionOptions();
		connectionOptions.setServerURIs(new String[] {"tcp://localhost:1883"});

		Mqttv5PahoMessageHandler handler = new Mqttv5PahoMessageHandler(connectionOptions, "testClient");
		handler.setDefaultTopic("test/topic");

		BeanFactory beanFactory = mock(BeanFactory.class);
		MessageConverter converter = mock(MessageConverter.class);
		given(beanFactory.getBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
				MessageConverter.class)).willReturn(converter);
		handler.setBeanFactory(beanFactory);

		handler.afterPropertiesSet();

		ReflectionTestUtils.setField(handler, "mqttClient", mockClient);

		return handler;
	}

	private Mqttv5PahoMessageHandler getFailConnectMessageHandler(int reasonCode) throws Exception {
		IMqttAsyncClient mockClient = mock(IMqttAsyncClient.class);
		IMqttToken mockConnectToken = mock(IMqttToken.class);
		IMqttToken mockPublishToken = mock(IMqttToken.class);

		AtomicBoolean connectionCompleted = new AtomicBoolean(false);

		willAnswer(invocation -> connectionCompleted.get()).given(mockClient).isConnected();

		willAnswer(invocation -> {
				throw new MqttException(reasonCode);
		}).given(mockClient).connect(any(MqttConnectionOptions.class));

		willDoNothing().given(mockConnectToken).waitForCompletion(anyLong());

		given(mockClient.publish(anyString(), any(MqttMessage.class), any(), any()))
				.willReturn(mockPublishToken);

		willDoNothing().given(mockPublishToken).waitForCompletion(anyLong());
		given(mockPublishToken.getMessageId()).willReturn(1);

		return createHandler(mockClient);

	}

}
