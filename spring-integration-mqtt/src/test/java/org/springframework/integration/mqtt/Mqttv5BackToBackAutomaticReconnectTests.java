/*
 * Copyright 2022-2024 the original author or authors.
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

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Lucas Bowler
 * @author Artem Bilan
 *
 * @since 5.5.13
 */
@SpringJUnitConfig
@DirtiesContext
public class Mqttv5BackToBackAutomaticReconnectTests implements MosquittoContainerTest {

	@Autowired
	@Qualifier("mqttOutFlow.input")
	private MessageChannel mqttOutFlowInput;

	@Autowired
	private PollableChannel fromMqttChannel;

	@Autowired
	private MqttConnectionOptions connectionOptions;

	@Autowired
	Config config;

	@Test
	public void testReconnectionWhenFirstConnectionFails() throws InterruptedException {
		Message<String> testMessage =
				MessageBuilder.withPayload("testPayload")
						.setHeader(MqttHeaders.TOPIC, "siTest")
						.build();

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.mqttOutFlowInput.send(testMessage))
				.withCauseExactlyInstanceOf(MqttException.class)
				.withRootCauseExactlyInstanceOf(UnknownHostException.class);

		connectionOptions.setServerURIs(new String[] {MosquittoContainerTest.mqttUrl()});

		assertThat(this.config.subscribeLatch.await(10, TimeUnit.SECONDS)).isTrue();

		this.mqttOutFlowInput.send(testMessage);

		Message<?> receive = this.fromMqttChannel.receive(10_000);

		assertThat(receive).isNotNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		CountDownLatch subscribeLatch = new CountDownLatch(1);

		@Bean
		public MqttConnectionOptions mqttConnectOptions() {
			return new MqttConnectionOptionsBuilder()
					.serverURI("wss://badMqttUrl")
					.automaticReconnect(true)
					.connectionTimeout(1)
					.build();
		}

		@Bean
		public IntegrationFlow mqttOutFlow() {
			Mqttv5PahoMessageHandler messageHandler =
					new Mqttv5PahoMessageHandler(mqttConnectOptions(), "mqttv5SIout");

			return f -> f.handle(messageHandler);
		}

		@Bean
		public IntegrationFlow mqttInFlow() {
			Mqttv5PahoMessageDrivenChannelAdapter messageProducer =
					new Mqttv5PahoMessageDrivenChannelAdapter(mqttConnectOptions(), "mqttv5SIin", "siTest");
			messageProducer.setPayloadType(String.class);

			return IntegrationFlow.from(messageProducer)
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

		@EventListener(MqttSubscribedEvent.class)
		void mqttEvents() {
			this.subscribeLatch.countDown();
		}

	}

}
