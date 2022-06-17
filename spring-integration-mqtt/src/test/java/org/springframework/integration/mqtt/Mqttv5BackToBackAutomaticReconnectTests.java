/*
 * Copyright 2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mqtt.event.MqttIntegrationEvent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaderMapper;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;


/**
 * @author Lucas Bowler
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
	private Config config;

	@Autowired
	private MqttConnectionOptions connectionOptions;


	@Test //GH-3822
	public void testReconnectionWhenFirstConnectionFails() throws InterruptedException {
		String testPayload = "foo";

		MessageHandlingException messageHandlingException = Assertions.assertThrows(MessageHandlingException.class, () ->
				this.mqttOutFlowInput.send(
						MessageBuilder.withPayload(testPayload)
								.setHeader(MqttHeaders.TOPIC, "siTest")
								.setHeader("foo", "bar")
								.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
								.build())
		);
		Assertions.assertInstanceOf(MqttException.class, messageHandlingException.getCause());
		Assertions.assertInstanceOf(UnknownHostException.class, messageHandlingException.getRootCause());

		connectionOptions.setServerURIs(new String[] {MosquittoContainerTest.mqttUrl()});
		Thread.sleep(2_500);

		this.mqttOutFlowInput.send(
				MessageBuilder.withPayload(testPayload)
						.setHeader(MqttHeaders.TOPIC, "siTest")
						.setHeader("foo", "bar")
						.setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
						.build());

		Message<?> receive = this.fromMqttChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(testPayload);
		assertThat(receive.getHeaders())
				.containsEntry("foo", "bar")
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsKey(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);

		receive.getHeaders()
				.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, SimpleAcknowledgment.class)
				.acknowledge();

		assertThat(this.config.events)
				.isNotEmpty()
				.hasAtLeastOneElementOfType(MqttMessageSentEvent.class)
				.hasAtLeastOneElementOfType(MqttMessageDeliveredEvent.class)
				.hasAtLeastOneElementOfType(MqttSubscribedEvent.class);
	}


	@Configuration
	@EnableIntegration
	public static class Config {

		List<MqttIntegrationEvent> events = new ArrayList<>();

		@EventListener
		void mqttEvents(MqttIntegrationEvent event) {
			this.events.add(event);
		}

		@Bean
		public SmartMessageConverter mqttStringToBytesConverter() {
			return new AbstractMessageConverter() {

				@Override
				protected boolean supports(Class<?> clazz) {
					return true;
				}

				@Override
				protected Object convertFromInternal(Message<?> message, Class<?> targetClass,
						Object conversionHint) {

					return message.getPayload().toString().getBytes(StandardCharsets.UTF_8);
				}

				@Override
				protected Object convertToInternal(Object payload, MessageHeaders headers,
						Object conversionHint) {

					return new String((byte[]) payload);
				}

			};
		}


		@Bean
		public MqttConnectionOptions mqttConnectOptions() {
			MqttConnectionOptionsBuilder builder = new MqttConnectionOptionsBuilder();
			builder.serverURI("wss://badMqttUrl");
			builder.automaticReconnect(true);
			return builder.build();
		}

		@Bean
		public IntegrationFlow mqttOutFlow() {

			Mqttv5PahoMessageHandler messageHandler =
					new Mqttv5PahoMessageHandler(mqttConnectOptions(), "mqttv5SIout");
			MqttHeaderMapper mqttHeaderMapper = new MqttHeaderMapper();
			mqttHeaderMapper.setOutboundHeaderNames("foo", MessageHeaders.CONTENT_TYPE);
			messageHandler.setHeaderMapper(mqttHeaderMapper);
			messageHandler.setAsync(true);
			messageHandler.setAsyncEvents(true);
			messageHandler.setConverter(mqttStringToBytesConverter());

			return f -> f.handle(messageHandler);
		}


		@Bean
		public IntegrationFlow mqttInFlow() {
			Mqttv5PahoMessageDrivenChannelAdapter messageProducer =
					new Mqttv5PahoMessageDrivenChannelAdapter(mqttConnectOptions(), "mqttv5SIin", "siTest");
			messageProducer.setPayloadType(String.class);
			messageProducer.setMessageConverter(mqttStringToBytesConverter());
			messageProducer.setManualAcks(true);

			return IntegrationFlows.from(messageProducer)
					.channel(c -> c.queue("fromMqttChannel"))
					.get();
		}

	}

}
