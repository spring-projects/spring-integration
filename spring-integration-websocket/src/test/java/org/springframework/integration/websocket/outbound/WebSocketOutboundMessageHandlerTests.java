/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.websocket.outbound;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.JettyWebSocketTestServer;
import org.springframework.integration.websocket.TestServerConfig;
import org.springframework.integration.websocket.support.SubProtocolHandlerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class WebSocketOutboundMessageHandlerTests {

	@Autowired
	@Qualifier("webSocketOutboundMessageHandler")
	private MessageHandler messageHandler;

	@Value("#{server.serverContext.getBean('clientInboundChannel')}")
	private QueueChannel clientInboundChannel;

	@Test
	public void testWebSocketOutboundMessageHandler() throws Exception {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setMessageId("mess0");
		headers.setSubscriptionId("sub0");
		headers.setDestination("/foo");
		byte[] payload = "Hello World".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

		this.messageHandler.handleMessage(message);

		Message<?> received = this.clientInboundChannel.receive(10000);
		assertNotNull(received);

		StompHeaderAccessor receivedHeaders = StompHeaderAccessor.wrap(received);
		assertEquals("mess0", receivedHeaders.getMessageId());
		assertEquals("sub0", receivedHeaders.getSubscriptionId());
		assertEquals("/foo", receivedHeaders.getDestination());

		Object receivedPayload = received.getPayload();
		assertThat(receivedPayload, instanceOf(byte[].class));
		assertArrayEquals((byte[]) receivedPayload, payload);
	}


	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public JettyWebSocketTestServer server() {
			return new JettyWebSocketTestServer(TestServerConfig.class);
		}

		@Bean
		public IntegrationWebSocketContainer clientWebSocketContainer() {
			ClientWebSocketContainer container =
					new ClientWebSocketContainer(new JettyWebSocketClient(), server().getWsBaseUrl() + "/ws");
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
					 new SubProtocolHandlerContainer(stompSubProtocolHandler()));
		}

	}

}
