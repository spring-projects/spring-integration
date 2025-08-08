/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket.outbound;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.TestServerConfig;
import org.springframework.integration.websocket.TomcatWebSocketTestServer;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class WebSocketOutboundMessageHandlerTests {

	@Autowired
	@Qualifier("webSocketOutboundMessageHandler")
	private MessageHandler messageHandler;

	@Value("#{server.serverContext.getBean('clientInboundChannel')}")
	private QueueChannel clientInboundChannel;

	@Test
	public void testWebSocketOutboundMessageHandler() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setMessageId("mess0");
		headers.setSubscriptionId("sub0");
		headers.setDestination("/foo");
		String payload = "Hello World";
		Message<String> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

		this.messageHandler.handleMessage(message);

		Message<?> received = this.clientInboundChannel.receive(10000);
		assertThat(received).isNotNull();

		StompHeaderAccessor receivedHeaders = StompHeaderAccessor.wrap(received);
		assertThat(receivedHeaders.getMessageId()).isEqualTo("mess0");
		assertThat(receivedHeaders.getSubscriptionId()).isEqualTo("sub0");
		assertThat(receivedHeaders.getDestination()).isEqualTo("/foo");

		Object receivedPayload = received.getPayload();
		assertThat(receivedPayload).isInstanceOf(byte[].class);
		assertThat(payload.getBytes()).isEqualTo(receivedPayload);
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
			return new SockJsClient(Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient())));
		}

		@Bean
		public IntegrationWebSocketContainer clientWebSocketContainer() {
			ClientWebSocketContainer container =
					new ClientWebSocketContainer(webSocketClient(), server().getWsBaseUrl() + "/ws");
			container.setAutoStartup(true);
			return container;
		}

		@Bean
		public SubProtocolHandler stompSubProtocolHandler() {
			return new StompSubProtocolHandler();
		}

		@Bean
		public MessageHandler webSocketOutboundMessageHandler() {
			return new WebSocketOutboundMessageHandler(clientWebSocketContainer(),
					new SubProtocolHandlerRegistry(stompSubProtocolHandler()));
		}

	}

}
