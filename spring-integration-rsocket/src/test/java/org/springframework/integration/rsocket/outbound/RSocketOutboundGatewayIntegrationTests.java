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

package org.springframework.integration.rsocket.outbound;

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
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.RSocketInteractionModel;
import org.springframework.integration.rsocket.ServerRSocketMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.messaging.rsocket.annotation.support.RSocketRequesterMethodArgumentResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig(RSocketOutboundGatewayIntegrationTests.ClientConfig.class)
@DirtiesContext
public class RSocketOutboundGatewayIntegrationTests {

	private static final String ROUTE_HEADER = "rsocket_route";

	private static final String INTERACTION_MODEL_HEADER = "interaction_model";

	private static AnnotationConfigApplicationContext serverContext;

	private static CloseableChannel server;

	private static FluxMessageChannel serverInputChannel;

	private static FluxMessageChannel serverResultChannel;

	private static PollableChannel serverErrorChannel;

	private static TestController serverController;

	@Autowired
	private FluxMessageChannel inputChannel;

	@Autowired
	private FluxMessageChannel resultChannel;

	@Autowired
	private PollableChannel errorChannel;

	@Autowired
	private TestController clientController;

	private RSocketRequester serverRsocketRequester;

	@BeforeAll
	static void setup() {
		serverContext = new AnnotationConfigApplicationContext(ServerConfig.class);
		server = RSocketServer.create()
				.payloadDecoder(PayloadDecoder.ZERO_COPY)
				.acceptor(serverContext.getBean(RSocketMessageHandler.class).responder())
				.bind(TcpServerTransport.create("localhost", 0))
				.block();

		serverController = serverContext.getBean(TestController.class);
		serverInputChannel = serverContext.getBean("inputChannel", FluxMessageChannel.class);
		serverResultChannel = serverContext.getBean("resultChannel", FluxMessageChannel.class);
		serverErrorChannel = serverContext.getBean("errorChannel", PollableChannel.class);
	}

	@AfterAll
	static void tearDown() {
		serverContext.close();
		server.dispose();
	}

	@BeforeEach
	void setupTest(TestInfo testInfo) {
		if (testInfo.getDisplayName().startsWith("server")) {
			this.serverRsocketRequester = serverController.clientRequester.block(Duration.ofSeconds(10));
		}
	}

	@Test
	void clientFireAndForget() {
		fireAndForget(this.inputChannel, this.resultChannel, serverController, null);
	}

	@Test
	void serverFireAndForget() {
		fireAndForget(serverInputChannel, serverResultChannel, this.clientController, this.serverRsocketRequester);
	}

	private void fireAndForget(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			TestController controller, RSocketRequester rsocketRequester) {

		Disposable disposable = Flux.from(resultChannel).subscribe();
		inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "receive")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.fireAndForget)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		StepVerifier.create(controller.fireForgetPayloads)
				.expectNext("Hello")
				.thenCancel()
				.verify();

		disposable.dispose();
	}


	@Test
	void clientEcho() {
		echo(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverEcho() {
		echo(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void echo(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		StepVerifier verifier =
				StepVerifier.create(
						Flux.from(resultChannel)
								.map(Message::getPayload)
								.cast(String.class))
						.expectNext("Hello")
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "echo")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientEchoAsync() {
		echoAsync(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverEchoAsync() {
		echoAsync(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void echoAsync(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		StepVerifier verifier =
				StepVerifier.create(
						Flux.from(resultChannel)
								.map(Message::getPayload)
								.cast(String.class))
						.expectNext("Hello async")
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "echo-async")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientEchoStream() {
		echoStream(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverEchoStream() {
		echoStream(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void echoStream(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		@SuppressWarnings("unchecked")
		StepVerifier verifier =
				StepVerifier.create(
						Flux.from(resultChannel)
								.next()
								.map(Message::getPayload)
								.flatMapMany((payload) -> (Flux<String>) payload))
						.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "echo-stream")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestStream)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientEchoChannel() {
		echoChannel(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverEchoChannel() {
		echoChannel(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void echoChannel(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		@SuppressWarnings("unchecked")
		StepVerifier verifier =
				StepVerifier.create(
						Flux.from(resultChannel)
								.next()
								.map(Message::getPayload)
								.flatMapMany((payload) -> (Flux<String>) payload))
						.expectNext("Hello 1 async").expectNextCount(8).expectNext("Hello 10 async")
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload(Flux.range(1, 10).map(i -> "Hello " + i))
						.setHeader(ROUTE_HEADER, "echo-channel")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestChannel)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}


	@Test
	void clientVoidReturnValue() {
		voidReturnValue(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverVoidReturnValue() {
		voidReturnValue(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void voidReturnValue(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		StepVerifier verifier =
				StepVerifier.create(resultChannel)
						.expectSubscription()
						.expectNoEvent(Duration.ofMillis(100))
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "void-return-value")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientVoidReturnValueFromExceptionHandler() {
		voidReturnValueFromExceptionHandler(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverVoidReturnValueFromExceptionHandler() {
		voidReturnValueFromExceptionHandler(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void voidReturnValueFromExceptionHandler(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		StepVerifier verifier =
				StepVerifier.create(resultChannel)
						.expectSubscription()
						.expectNoEvent(Duration.ofMillis(100))
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("bad")
						.setHeader(ROUTE_HEADER, "void-return-value")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientHandleWithThrownException() {
		handleWithThrownException(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverHandleWithThrownException() {
		handleWithThrownException(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void handleWithThrownException(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		StepVerifier verifier =
				StepVerifier.create(
						Flux.from(resultChannel)
								.map(Message::getPayload)
								.cast(String.class))
						.expectNext("Invalid input error handled")
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("a")
						.setHeader(ROUTE_HEADER, "thrown-exception")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientHandleWithErrorSignal() {
		handleWithErrorSignal(this.inputChannel, this.resultChannel, null);
	}

	@Test
	void serverHandleWithErrorSignal() {
		handleWithErrorSignal(serverInputChannel, serverResultChannel, this.serverRsocketRequester);
	}

	private void handleWithErrorSignal(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			RSocketRequester rsocketRequester) {

		StepVerifier verifier =
				StepVerifier.create(
						Flux.from(resultChannel)
								.map(Message::getPayload)
								.cast(String.class))
						.expectNext("Invalid input error handled")
						.thenCancel()
						.verifyLater();

		inputChannel.send(
				MessageBuilder.withPayload("a")
						.setHeader(ROUTE_HEADER, "error-signal")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		verifier.verify();
	}

	@Test
	void clientNoMatchingRoute() {
		noMatchingRoute(this.inputChannel, this.resultChannel, this.errorChannel, null);
	}

	@Test
	void serverNoMatchingRoute() {
		noMatchingRoute(serverInputChannel, serverResultChannel, serverErrorChannel, this.serverRsocketRequester);
	}

	private void noMatchingRoute(MessageChannel inputChannel, FluxMessageChannel resultChannel,
			PollableChannel errorChannel, RSocketRequester rsocketRequester) {

		Disposable disposable = Flux.from(resultChannel).subscribe();
		inputChannel.send(
				MessageBuilder.withPayload("anything")
						.setHeader(ROUTE_HEADER, "invalid")
						.setHeader(INTERACTION_MODEL_HEADER, RSocketInteractionModel.requestResponse)
						.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, rsocketRequester)
						.build());

		Message<?> errorMessage = errorChannel.receive(10_000);

		assertThat(errorMessage).isNotNull()
				.isInstanceOf(ErrorMessage.class)
				.extracting(Message::getPayload)
				.isInstanceOf(MessageHandlingException.class)
				.satisfies((ex) -> assertThat((Exception) ex)
						.hasMessageContaining(
								"ApplicationErrorException (0x201): No handler for destination 'invalid'"));

		disposable.dispose();
	}

	private abstract static class CommonConfig {

		@Bean
		public TestController controller() {
			return new TestController();
		}

		@Bean
		public RSocketOutboundGateway rsocketOutboundGateway() {
			RSocketOutboundGateway rsocketOutboundGateway =
					new RSocketOutboundGateway(
							new FunctionExpression<Message<?>>((m) ->
									m.getHeaders().get(ROUTE_HEADER)));
			rsocketOutboundGateway.setInteractionModelExpression(
					new FunctionExpression<Message<?>>((m) -> m.getHeaders().get(INTERACTION_MODEL_HEADER)));
			return rsocketOutboundGateway;
		}

		@Bean
		public IntegrationFlow rsocketOutboundFlow() {
			return IntegrationFlows.from(MessageChannels.flux("inputChannel"))
					.handle(rsocketOutboundGateway())
					.channel(c -> c.flux("resultChannel"))
					.get();
		}

		@Bean
		public PollableChannel errorChannel() {
			return new QueueChannel();
		}

	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig extends CommonConfig {

		@Bean(destroyMethod = "dispose")
		@Nullable
		public RSocket rsocketForServerRequests() {
			return RSocketRequester.builder()
					.setupRoute("clientConnect")
					.rsocketConnector(connector ->
							connector.acceptor(
									RSocketMessageHandler.responder(RSocketStrategies.create(), controller())))
					.connectTcp("localhost", server.address().getPort())
					.block()
					.rsocket();
		}

		@Bean
		public ClientRSocketConnector clientRSocketConnector() {
			return new ClientRSocketConnector("localhost", server.address().getPort());
		}

		@Override
		@Bean
		public RSocketOutboundGateway rsocketOutboundGateway() {
			RSocketOutboundGateway rsocketOutboundGateway = super.rsocketOutboundGateway();
			rsocketOutboundGateway.setClientRSocketConnector(clientRSocketConnector());
			return rsocketOutboundGateway;
		}

	}

	@Configuration
	@EnableIntegration
	static class ServerConfig extends CommonConfig {

		@Bean
		public RSocketMessageHandler messageHandler() {
			return new ServerRSocketMessageHandler(true);
		}

	}

	@Controller
	static class TestController {

		final ReplayProcessor<String> fireForgetPayloads = ReplayProcessor.create();

		final MonoProcessor<RSocketRequester> clientRequester = MonoProcessor.create();

		@MessageMapping("receive")
		void receive(String payload) {
			this.fireForgetPayloads.onNext(payload);
		}

		@MessageMapping("echo")
		String echo(String payload) {
			return payload;
		}

		@MessageMapping("echo-async")
		Mono<String> echoAsync(String payload) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> payload + " async");
		}

		@MessageMapping("echo-stream")
		Flux<String> echoStream(String payload) {
			return Flux.interval(Duration.ofMillis(10)).map(aLong -> payload + " " + aLong);
		}

		@MessageMapping("echo-channel")
		Flux<String> echoChannel(Flux<String> payloads) {
			return payloads.delayElements(Duration.ofMillis(10)).map(payload -> payload + " async");
		}

		@MessageMapping("thrown-exception")
		Mono<String> handleAndThrow(String payload) {
			throw new IllegalArgumentException("Invalid input error");
		}

		@MessageMapping("error-signal")
		Mono<String> handleAndReturnError(String payload) {
			return Mono.error(new IllegalArgumentException("Invalid input error"));
		}

		@MessageMapping("void-return-value")
		Mono<Void> voidReturnValue(String payload) {
			return !payload.equals("bad") ?
					Mono.delay(Duration.ofMillis(10)).then(Mono.empty()) :
					Mono.error(new IllegalStateException("bad"));
		}

		@MessageExceptionHandler
		Mono<String> handleException(IllegalArgumentException ex) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> ex.getMessage() + " handled");
		}

		@MessageExceptionHandler
		Mono<Void> handleExceptionWithVoidReturnValue(IllegalStateException ex) {
			return Mono.delay(Duration.ofMillis(10)).then(Mono.empty());
		}

		@ConnectMapping("clientConnect")
		void clientConnect(RSocketRequester requester) {
			this.clientRequester.onNext(requester);
		}

	}

}
