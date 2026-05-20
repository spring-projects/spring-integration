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

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link Mqttv5PahoMessageHandler}, focusing on the outbound payload
 * conversion path.
 *
 * @author Deng Pan
 *
 * @since 6.5.9
 */
class Mqttv5HandlerTests {

	@Test
	void publishPojoWithDefaultConverter() throws Exception {
		IMqttAsyncClient mockClient = mock();
		IMqttToken token = mock();
		given(mockClient.isConnected()).willReturn(true);
		given(mockClient.publish(anyString(), any(MqttMessage.class), any(), any())).willReturn(token);

		Mqttv5PahoMessageHandler handler = createHandler(mockClient, new ConfigurableCompositeMessageConverter());

		TestPojo pojo = new TestPojo("test", 1);
		handler.handleMessage(MessageBuilder.withPayload(pojo)
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
				.build());

		// Capture the MqttMessage actually handed to paho and round-trip its payload bytes
		// through Jackson to confirm the POJO was serialized to JSON.
		ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
		verify(mockClient).publish(anyString(), captor.capture(), any(), any());
		TestPojo roundTripped = new ObjectMapper().readValue(captor.getValue().getPayload(), TestPojo.class);
		assertThat(roundTripped).isEqualTo(pojo);
	}

	@Test
	void publishPojoWithCustomConverter() throws Exception {
		IMqttAsyncClient mockClient = mock();
		IMqttToken token = mock();
		given(mockClient.isConnected()).willReturn(true);
		given(mockClient.publish(anyString(), any(MqttMessage.class), any(), any())).willReturn(token);

		MessageConverter converter = mock(MessageConverter.class);
		given(converter.toMessage(any(), any(MessageHeaders.class)))
				.willAnswer(invocation -> new GenericMessage<>("test"));

		Mqttv5PahoMessageHandler handler = createHandler(mockClient, converter);

		handler.handleMessage(MessageBuilder.withPayload(new TestPojo("test", 2)).build());

		// Custom converter returns a String; verify the handler applies the String -> bytes safeguard.
		ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
		verify(mockClient).publish(anyString(), captor.capture(), any(), any());
		assertThat(captor.getValue().getPayload()).isEqualTo("test".getBytes());
	}

	private static Mqttv5PahoMessageHandler createHandler(IMqttAsyncClient mockClient, MessageConverter converter) {
		ClientManager<IMqttAsyncClient, MqttConnectionOptions> clientManager = mock();
		given(clientManager.getUrl()).willReturn("tcp://localhost:1883");
		given(clientManager.getClientId()).willReturn("testClient");
		given(clientManager.getConnectionInfo()).willReturn(new MqttConnectionOptions());
		given(clientManager.getClient()).willReturn(mockClient);

		Mqttv5PahoMessageHandler handler = new Mqttv5PahoMessageHandler(clientManager);
		handler.setDefaultTopic("test/topic");
		handler.setConverter(converter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.start();
		return handler;
	}

	record TestPojo(String name, int value) { }
}
