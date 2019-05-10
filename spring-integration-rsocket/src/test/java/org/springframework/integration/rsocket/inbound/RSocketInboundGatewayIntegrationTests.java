/*
 * Copyright 2019 the original author or authors.
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
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.RSocketConnectedEvent;
import org.springframework.integration.rsocket.ServerRSocketConnector;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.netty.tcp.TcpServer;
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

	private static int port;

	private static ServerConfig serverConfig;

	private static PollableChannel serverFireAndForgetChannelChannel;

	;

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
		public RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.builder()
					.decoder(StringDecoder.allMimeTypes())
					.encoder(CharSequenceEncoder.allMimeTypes())
					.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
					.build();
		}

		@Bean
		public PollableChannel fireAndForgetChannelChannel() {
			return new QueueChannel();
		}

		@Bean
		public RSocketInboundGateway rsocketInboundGatewayFireAndForget() {
			RSocketInboundGateway rsocketInboundGateway = new RSocketInboundGateway("receive");
			rsocketInboundGateway.setRSocketStrategies(rsocketStrategies());
			rsocketInboundGateway.setRequestChannel(fireAndForgetChannelChannel());
			return rsocketInboundGateway;
		}

		@Bean
		public RSocketInboundGateway rsocketInboundGatewayRequestReply() {
			RSocketInboundGateway rsocketInboundGateway = new RSocketInboundGateway("echo");
			rsocketInboundGateway.setRSocketStrategies(rsocketStrategies());
			rsocketInboundGateway.setRequestChannel(requestReplyChannel());
			return rsocketInboundGateway;
		}

		@Bean
		public FluxMessageChannel requestReplyChannel() {
			return new FluxMessageChannel();
		}

		@Transformer(inputChannel = "requestReplyChannel")
		public Mono<String> echoTransformation(Flux<String> payload) {
			return payload.next().map(String::toUpperCase);
		}

	}

	@Configuration
	@EnableIntegration
	static class ServerConfig extends CommonConfig implements ApplicationListener<RSocketConnectedEvent> {

		final MonoProcessor<RSocketRequester> clientRequester = MonoProcessor.create();

		@Override
		public void onApplicationEvent(RSocketConnectedEvent event) {
			this.clientRequester.onNext(event.getRequester());
		}

		@Bean
		public ServerRSocketConnector serverRSocketConnector() {
			TcpServer tcpServer =
					TcpServer.create().port(0)
							.doOnBound(server -> port = server.port());
			ServerRSocketConnector serverRSocketConnector =
					new ServerRSocketConnector(TcpServerTransport.create(tcpServer));
			serverRSocketConnector.setRSocketStrategies(rsocketStrategies());
			serverRSocketConnector.setFactoryConfigurer((factory) -> factory.frameDecoder(PayloadDecoder.ZERO_COPY));
			return serverRSocketConnector;
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig extends CommonConfig {

		@Bean
		public ClientRSocketConnector clientRSocketConnector() {
			ClientRSocketConnector clientRSocketConnector = new ClientRSocketConnector("localhost", port);
			clientRSocketConnector.setFactoryConfigurer((factory) -> factory.frameDecoder(PayloadDecoder.ZERO_COPY));
			clientRSocketConnector.setRSocketStrategies(rsocketStrategies());
			clientRSocketConnector.setConnectRoute("clientConnect");
			return clientRSocketConnector;
		}

	}

}
