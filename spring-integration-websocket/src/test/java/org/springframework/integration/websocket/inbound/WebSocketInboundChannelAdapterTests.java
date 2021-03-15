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

package org.springframework.integration.websocket.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.TestServerConfig;
import org.springframework.integration.websocket.TomcatWebSocketTestServer;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class WebSocketInboundChannelAdapterTests {

	@Value("#{server.serverContext.getBean('subProtocolWebSocketHandler')}")
	private SubProtocolWebSocketHandler subProtocolWebSocketHandler;

	@Value("#{server.serverContext.getBean('clientOutboundChannel')}")
	private DirectChannel clientOutboundChannel;

	@Autowired
	IntegrationWebSocketContainer clientWebSocketContainer;

	@Autowired
	@Qualifier("webSocketChannel")
	private QueueChannel webSocketChannel;

	@Test
	@SuppressWarnings("unchecked")
	public void testWebSocketInboundChannelAdapter() {
		WebSocketSession session = clientWebSocketContainer.getSession(null);
		assertThat(session).isNotNull();
		assertThat(session.isOpen()).isTrue();
		assertThat(session.getAcceptedProtocol()).isEqualTo("v10.stomp");

		Map<String, WebSocketSession> sessions =
				TestUtils.getPropertyValue(this.subProtocolWebSocketHandler, "sessions", Map.class);


		assertThat(sessions.size()).isEqualTo(1);

		String sessionId = sessions.keySet().iterator().next();

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
		headers.setLeaveMutable(true);
		headers.setSessionId(sessionId);
		Message<byte[]> message =
				MessageBuilder.createMessage(ByteBuffer.allocate(0).array(), headers.getMessageHeaders());

		this.clientOutboundChannel.send(message);

		Message<?> received = this.webSocketChannel.receive(10000);
		assertThat(received).isNotNull();

		StompHeaderAccessor receivedHeaders = StompHeaderAccessor.wrap(received);
		assertThat(receivedHeaders.getCommand()).isEqualTo(StompCommand.MESSAGE);

		Object receivedPayload = received.getPayload();
		assertThat(receivedPayload).isInstanceOf(String.class);
		assertThat(receivedPayload).isEqualTo("");

	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public TomcatWebSocketTestServer server() {
			return new TomcatWebSocketTestServer(TestServerConfig.class);
		}

		@Bean
		public WebSocketClient webSocketClient() {
			return new SockJsClient(Collections.<Transport>singletonList(new WebSocketTransport(new StandardWebSocketClient())));
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
		public MessageChannel webSocketChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageProducer webSocketInboundChannelAdapter() {
			WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
					new WebSocketInboundChannelAdapter(clientWebSocketContainer(),
							new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
			webSocketInboundChannelAdapter.setOutputChannel(webSocketChannel());
			return webSocketInboundChannelAdapter;
		}

	}

}
