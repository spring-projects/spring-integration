/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.websocket.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

import javax.websocket.DeploymentException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.integration.websocket.TomcatWebSocketTestServer;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@SpringJUnitConfig(classes = WebSocketDslTests.ClientConfig.class)
@DirtiesContext
public class WebSocketDslTests {

	@Autowired
	TomcatWebSocketTestServer server;

	@Autowired
	WebSocketClient webSocketClient;

	@Autowired
	IntegrationFlowContext integrationFlowContext;

	@Test
	void testDynamicServerEndpointRegistration() {
		// Dynamic server flow
		AnnotationConfigWebApplicationContext serverContext = this.server.getServerContext();
		IntegrationFlowContext serverIntegrationFlowContext = serverContext.getBean(IntegrationFlowContext.class);
		ServerWebSocketContainer serverWebSocketContainer =
				new ServerWebSocketContainer("/dynamic")
						.setHandshakeHandler(serverContext.getBean(HandshakeHandler.class));

		WebSocketInboundChannelAdapter webSocketInboundChannelAdapter =
				new WebSocketInboundChannelAdapter(serverWebSocketContainer);

		QueueChannel dynamicRequestsChannel = new QueueChannel();

		IntegrationFlow serverFlow =
				IntegrationFlows.from(webSocketInboundChannelAdapter)
						.channel(dynamicRequestsChannel)
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration dynamicServerFlow =
				serverIntegrationFlowContext.registration(serverFlow)
						.addBean(serverWebSocketContainer)
						.register();

		// Dynamic client flow
		ClientWebSocketContainer clientWebSocketContainer =
				new ClientWebSocketContainer(this.webSocketClient, this.server.getWsBaseUrl() + "/dynamic");
		clientWebSocketContainer.setAutoStartup(true);
		WebSocketOutboundMessageHandler webSocketOutboundMessageHandler =
				new WebSocketOutboundMessageHandler(clientWebSocketContainer);

		IntegrationFlow clientFlow = flow -> flow.handle(webSocketOutboundMessageHandler);

		IntegrationFlowContext.IntegrationFlowRegistration dynamicClientFlow =
				this.integrationFlowContext.registration(clientFlow)
						.addBean(clientWebSocketContainer)
						.register();

		dynamicClientFlow.getInputChannel().send(new GenericMessage<>("dynamic test"));

		Message<?> result = dynamicRequestsChannel.receive(10_000);
		assertThat(result).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("dynamic test");

		dynamicServerFlow.destroy();

		await() // Looks like endpoint is removed on the server side somewhat async
				.untilAsserted(() ->
						assertThatExceptionOfType(MessageHandlingException.class)
								.isThrownBy(() ->
										dynamicClientFlow.getInputChannel().send(new GenericMessage<>("another test")))
								.withCauseInstanceOf(DeploymentException.class)
								.withMessageContaining("The HTTP response from the server [404]"));

		dynamicClientFlow.destroy();
	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig {

		@Bean
		public TomcatWebSocketTestServer server() {
			return new TomcatWebSocketTestServer(WebSocketDslTests.ServerConfig.class);
		}

		@Bean
		public WebSocketClient webSocketClient() {
			return new StandardWebSocketClient();
		}

	}


	@Configuration
	@EnableIntegration
	static class ServerConfig {

		@Bean
		public DefaultHandshakeHandler handshakeHandler() {
			return new DefaultHandshakeHandler(new TomcatRequestUpgradeStrategy());
		}

	}

}
