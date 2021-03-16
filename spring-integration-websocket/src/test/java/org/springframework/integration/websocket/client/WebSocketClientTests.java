/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.websocket.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.websocket.Constants;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.TestServerConfig;
import org.springframework.integration.websocket.TomcatWebSocketTestServer;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig(classes = WebSocketClientTests.ClientConfig.class)
@DirtiesContext
public class WebSocketClientTests {

	@Autowired
	@Qualifier("webSocketOutputChannel")
	private MessageChannel webSocketOutputChannel;

	@Autowired
	@Qualifier("webSocketInputChannel")
	private QueueChannel webSocketInputChannel;

	@Test
	public void testWebSocketOutboundMessageHandler() {
		this.webSocketOutputChannel.send(new GenericMessage<>("Spring"));

		Message<?> received = this.webSocketInputChannel.receive(10000);
		assertThat(received).isNotNull();
		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(received);
		assertThat(stompHeaderAccessor.getMessageType()).isEqualTo(StompCommand.MESSAGE.getMessageType());

		Object receivedPayload = received.getPayload();
		assertThat(receivedPayload).isInstanceOf(String.class);
		assertThat(receivedPayload).isEqualTo("Hello Spring");
	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig {

		@Bean
		public TomcatWebSocketTestServer server() {
			return new TomcatWebSocketTestServer(ServerFlowConfig.class);
		}

		@Bean
		public WebSocketClient webSocketClient() {
			StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
			Map<String, Object> userProperties = new HashMap<>();
			userProperties.put(Constants.IO_TIMEOUT_MS_PROPERTY, "" + (Constants.IO_TIMEOUT_MS_DEFAULT * 6));
			webSocketClient.setUserProperties(userProperties);
			return new SockJsClient(Collections.singletonList(new WebSocketTransport(webSocketClient)));
		}

		@Bean
		public IntegrationWebSocketContainer clientWebSocketContainer() {
			return new ClientWebSocketContainer(webSocketClient(), server().getWsBaseUrl() + "/ws");
		}

		@Bean
		public SubProtocolHandler stompSubProtocolHandler() {
			return new StompSubProtocolHandler();
		}

		@Bean
		public MessageChannel webSocketInputChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel webSocketOutputChannel() {
			return new DirectChannel();
		}

		@Bean
		public MessageProducer webSocketInboundChannelAdapter() {
			WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
					new WebSocketInboundChannelAdapter(clientWebSocketContainer(),
							new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
			webSocketInboundChannelAdapter.setOutputChannel(webSocketInputChannel());
			return webSocketInboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "webSocketOutputChannel")
		public MessageHandler webSocketOutboundMessageHandler() {
			return new WebSocketOutboundMessageHandler(clientWebSocketContainer(),
					new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
		}

	}

	// WebSocket Server part

	@Configuration
	@EnableIntegration
	static class ServerFlowConfig extends TestServerConfig {

		@Bean
		@Transformer(inputChannel = "clientInboundChannel", outputChannel = "serviceChannel",
				poller = @Poller(fixedDelay = "100", maxMessagesPerPoll = "1"))
		public org.springframework.integration.transformer.Transformer objectToStringTransformer() {
			return new ObjectToStringTransformer();
		}

		@Bean
		public DirectChannel serviceChannel() {
			return new DirectChannel();
		}

		@Bean
		public TestService service() {
			return new TestService();
		}

		@Component
		public static class TestService {

			@ServiceActivator(inputChannel = "serviceChannel", outputChannel = "clientOutboundChannel")
			public byte[] handle(String payload) {
				return ("Hello " + payload).getBytes();
			}

		}

	}

}
