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

package org.springframework.integration.websocket.client;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.JettyWebSocketTestServer;
import org.springframework.integration.websocket.TestServerConfig;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.stereotype.Component;
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
public class WebSocketClientTests {

	@Value("#{server.serverContext}")
	private ApplicationContext serverContext;

	@Autowired
	@Qualifier("webSocketOutputChannel")
	private MessageChannel webSocketOutputChannel;

	@Autowired
	@Qualifier("webSocketInputChannel")
	private QueueChannel webSocketInputChannel;

	@Test
	public void testWebSocketOutboundMessageHandler() throws Exception {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setMessageId("mess0");
		headers.setSubscriptionId("sub0");
		headers.setDestination("/dest0");
		byte[] payload = "Bob".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

		this.webSocketOutputChannel.send(message);

		Message<?> received = this.webSocketInputChannel.receive(10000);
		assertNotNull(received);
		StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(received);
		assertEquals(StompCommand.MESSAGE.getMessageType(), stompHeaderAccessor.getMessageType());

		Object receivedPayload = received.getPayload();
		assertThat(receivedPayload, instanceOf(byte[].class));
		assertEquals("Hello Bob", new String((byte[]) receivedPayload));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public JettyWebSocketTestServer server() {
			return new JettyWebSocketTestServer(ServerFlowConfig.class);
		}

		@Bean
		public IntegrationWebSocketContainer clientWebSocketContainer() {
			return new ClientWebSocketContainer(new JettyWebSocketClient(), server().getWsBaseUrl() + "/ws");
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
							new SubProtocolHandlerContainer(stompSubProtocolHandler()));
			webSocketInboundChannelAdapter.setOutputChannel(webSocketInputChannel());
			return webSocketInboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "webSocketOutputChannel")
		public MessageHandler webSocketOutboundMessageHandler() {
			return new WebSocketOutboundMessageHandler(clientWebSocketContainer(),
					new SubProtocolHandlerContainer(stompSubProtocolHandler()));
		}

	}

	@Configuration
	@EnableIntegration
	static class ServerFlowConfig extends TestServerConfig {

		@Bean
		@Transformer(inputChannel = "clientInboundChannel", outputChannel = "filterNativeHeadersChannel",
				poller = @Poller(fixedDelay = "100", maxMessagesPerPoll = "1"))
		public org.springframework.integration.transformer.Transformer objectToStringTransformer() {
			return new ObjectToStringTransformer();
		}

		@Bean
		public DirectChannel filterNativeHeadersChannel() {
			return new DirectChannel();
		}

		/*TODO StompEncoder doesn't override the 'nativeHeaders', hence we end up with two values for 'content-type'
		and with an exception:
		org.springframework.messaging.simp.stomp.StompConversionException: Frame must be terminated with a null octet*/
		@Bean
		@Transformer(inputChannel = "filterNativeHeadersChannel", outputChannel = "serviceChannel")
		public org.springframework.integration.transformer.Transformer headerFilterTransformer() {
			return new HeaderFilter(NativeMessageHeaderAccessor.NATIVE_HEADERS);
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
