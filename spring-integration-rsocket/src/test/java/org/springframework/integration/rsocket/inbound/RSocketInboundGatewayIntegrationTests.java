/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.rsocket.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.RSocketConnectedEvent;
import org.springframework.integration.rsocket.ServerRSocketConnector;
import org.springframework.integration.rsocket.ServerRSocketMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig(RSocketInboundGatewayIntegrationTests.ClientConfig.class)
@DirtiesContext
public class RSocketInboundGatewayIntegrationTests {

	private static AnnotationConfigApplicationContext serverContext;

	private static ServerConfig serverConfig;

	private static PollableChannel serverFireAndForgetChannelChannel;

	@Autowired
	private ClientRSocketConnector clientRSocketConnector;

	@Autowired
	private PollableChannel fireAndForgetChannelChannel;

	private RSocketRequester serverRsocketRequester;

	private RSocketRequester clientRsocketRequester;

	@BeforeAll
	static void setup() {
		serverContext = new AnnotationConfigApplicationContext(ServerConfig.class);
		serverConfig = serverContext.getBean(ServerConfig.class);
		serverFireAndForgetChannelChannel = serverContext.getBean("fireAndForgetChannelChannel", PollableChannel.class);
	}

	@AfterAll
	static void tearDown() {
		serverContext.close();
	}

	@BeforeEach
	void setupTest(TestInfo testInfo) {
		if (testInfo.getDisplayName().startsWith("server")) {
			this.serverRsocketRequester = serverConfig.clientRequester.block(Duration.ofSeconds(10));
		}
		else {
			this.clientRsocketRequester =
					this.clientRSocketConnector.getRSocketRequester().block(Duration.ofSeconds(10));
		}
	}

	@Test
	void clientFireAndForget() {
		fireAndForget(serverFireAndForgetChannelChannel, this.clientRsocketRequester);
	}

	@Test
	void serverFireAndForget() {
		fireAndForget(this.fireAndForgetChannelChannel, this.serverRsocketRequester);
	}

	private void fireAndForget(PollableChannel inputChannel, RSocketRequester rsocketRequester) {
		rsocketRequester.route("receive")
				.data("Hello")
				.send()
				.subscribe();

		Message<?> receive = inputChannel.receive(10_000);
		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("Hello");
	}

	@Test
	void clientEcho() {
		echo(this.clientRsocketRequester);
	}

	@Test
	void serverEcho() {
		echo(this.serverRsocketRequester);
	}

	private void echo(RSocketRequester rsocketRequester) {
		Flux<String> result =
				Flux.range(1, 3)
						.concatMap(i ->
								rsocketRequester.route("echo")
										.data("hello " + i)
										.retrieveMono(String.class));

		StepVerifier.create(result)
				.expectNext("HELLO 1", "HELLO 2", "HELLO 3")
				.expectComplete()
				.verify(Duration.ofSeconds(10));
	}


	private abstract static class CommonConfig {

		@Bean
		public PollableChannel fireAndForgetChannelChannel() {
			return new QueueChannel();
		}

		@Bean
		public RSocketInboundGateway rsocketInboundGatewayFireAndForget() {
			RSocketInboundGateway rsocketInboundGateway = new RSocketInboundGateway("receive");
			rsocketInboundGateway.setRequestChannel(fireAndForgetChannelChannel());
			return rsocketInboundGateway;
		}

		@Bean
		public RSocketInboundGateway rsocketInboundGatewayRequestReply() {
			RSocketInboundGateway rsocketInboundGateway = new RSocketInboundGateway("echo");
			rsocketInboundGateway.setRequestChannelName("requestReplyChannel");
			return rsocketInboundGateway;
		}

		@Transformer(inputChannel = "requestReplyChannel")
		public Mono<String> echoTransformation(Flux<String> payload) {
			return payload.next().map(String::toUpperCase);
		}

	}

	@Configuration
	@EnableIntegration
	static class ServerConfig extends CommonConfig {

		final MonoProcessor<RSocketRequester> clientRequester = MonoProcessor.create();

		@Bean
		public CloseableChannel rsocketServer() {
			return RSocketServer.create()
					.payloadDecoder(PayloadDecoder.ZERO_COPY)
					.acceptor(serverRSocketMessageHandler().responder())
					.bind(TcpServerTransport.create("localhost", 0))
					.block();
		}

		@Bean
		public ServerRSocketMessageHandler serverRSocketMessageHandler() {
			return new ServerRSocketMessageHandler(true);
		}

		@Bean
		public ServerRSocketConnector serverRSocketConnector(ServerRSocketMessageHandler serverRSocketMessageHandler) {
			return new ServerRSocketConnector(serverRSocketMessageHandler);
		}

		@EventListener
		public void onApplicationEvent(RSocketConnectedEvent event) {
			this.clientRequester.onNext(event.getRequester());
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig extends CommonConfig {

		@Bean
		public ClientRSocketConnector clientRSocketConnector() {
			ClientRSocketConnector clientRSocketConnector =
					new ClientRSocketConnector("localhost", serverConfig.rsocketServer().address().getPort());
			clientRSocketConnector.setSetupRoute("clientConnect/{user}");
			clientRSocketConnector.setSetupRouteVariables("myUser");
			return clientRSocketConnector;
		}

	}

}
