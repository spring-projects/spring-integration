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

package org.springframework.integration.rsocket.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.MessageHandlerAcceptor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.tcp.TcpServer;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
public class RSocketOutboundGatewayIntegrationTests {

	private static int PORT;

	private static final String ROUTE_HEADER = "rsocket_route";

	private static final String COMMAND_HEADER = "rsocket_command";

	private static AnnotationConfigApplicationContext context;

	private static CloseableChannel server;

	@Autowired
	private FluxMessageChannel inputChannel;

	@Autowired
	private FluxMessageChannel resultChannel;

	@Autowired
	private PollableChannel errorChannel;

	@BeforeAll
	static void setup() {
		context = new AnnotationConfigApplicationContext(ServerConfig.class);
		TcpServer tcpServer =
				TcpServer.create().port(0)
						.doOnBound(server -> PORT = server.port());
		server = RSocketFactory.receive()
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.acceptor(context.getBean(MessageHandlerAcceptor.class))
				.transport(TcpServerTransport.create(tcpServer))
				.start()
				.block();
	}

	@AfterAll
	static void tearDown() {
		context.close();
		server.dispose();
	}

	@Test
	void fireAndForget() {
		Disposable disposable = Flux.from(this.resultChannel).subscribe();
		this.inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "receive")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.fireAndForget)
						.build());

		StepVerifier.create(context.getBean(ServerController.class).fireForgetPayloads)
				.expectNext("Hello")
				.thenCancel()
				.verify();

		disposable.dispose();
	}

	@Test
	void echo() {
		this.inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "echo")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestResponse)
						.build());

		StepVerifier.create(
				Flux.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(String.class))
				.expectNext("Hello")
				.thenCancel()
				.verify();
	}

	@Test
	void echoAsync() {
		this.inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "echo-async")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestResponse)
						.build());

		StepVerifier.create(
				Flux.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(String.class))
				.expectNext("Hello async")
				.thenCancel()
				.verify();
	}

	@Test
	void echoStream() {
		this.inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "echo-stream")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestStreamOrChannel)
						.build());

		Message<?> resultMessage =
				Flux.from(this.resultChannel)
						.blockFirst();

		assertThat(resultMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<String> resultStream = (Flux<String>) resultMessage.getPayload();
		StepVerifier.create(resultStream)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify();

	}

	@Test
	void echoChannel() {
		this.inputChannel.send(
				MessageBuilder.withPayload(Flux.range(1, 10).map(i -> "Hello " + i))
						.setHeader(ROUTE_HEADER, "echo-channel")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestStreamOrChannel)
						.build());

		Message<?> resultMessage =
				Flux.from(this.resultChannel)
						.blockFirst();

		assertThat(resultMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<String> resultStream = (Flux<String>) resultMessage.getPayload();
		StepVerifier.create(resultStream)
				.expectNext("Hello 1 async").expectNextCount(8).expectNext("Hello 10 async")
				.thenCancel()
				.verify();
	}

	@Test
	void voidReturnValue() {
		this.inputChannel.send(
				MessageBuilder.withPayload("Hello")
						.setHeader(ROUTE_HEADER, "void-return-value")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestStreamOrChannel)
						.build());

		Message<?> resultMessage =
				Flux.from(this.resultChannel)
						.blockFirst();

		assertThat(resultMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(Flux.class);

		Flux<?> resultStream = (Flux<?>) resultMessage.getPayload();
		StepVerifier.create(resultStream)
				.expectComplete()
				.verify();
	}

	@Test
	void voidReturnValueFromExceptionHandler() {
		this.inputChannel.send(
				MessageBuilder.withPayload("bad")
						.setHeader(ROUTE_HEADER, "void-return-value")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestStreamOrChannel)
						.build());

		Message<?> resultMessage =
				Flux.from(this.resultChannel)
						.blockFirst();

		assertThat(resultMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(Flux.class);

		Flux<?> resultStream = (Flux<?>) resultMessage.getPayload();
		StepVerifier.create(resultStream)
				.expectComplete()
				.verify();
	}

	@Test
	void handleWithThrownException() {
		this.inputChannel.send(
				MessageBuilder.withPayload("a")
						.setHeader(ROUTE_HEADER, "thrown-exception")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestResponse)
						.build());

		StepVerifier.create(
				Flux.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(String.class))
				.expectNext("Invalid input error handled")
				.thenCancel()
				.verify();
	}

	@Test
	void handleWithErrorSignal() {
		this.inputChannel.send(
				MessageBuilder.withPayload("a")
						.setHeader(ROUTE_HEADER, "error-signal")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestResponse)
						.build());

		StepVerifier.create(
				Flux.from(this.resultChannel)
						.map(Message::getPayload)
						.cast(String.class))
				.expectNext("Invalid input error handled")
				.thenCancel()
				.verify();
	}

	@Test
	void noMatchingRoute() {
		Disposable disposable = Flux.from(this.resultChannel).subscribe();
		this.inputChannel.send(
				MessageBuilder.withPayload("anything")
						.setHeader(ROUTE_HEADER, "invalid")
						.setHeader(COMMAND_HEADER, RSocketOutboundGateway.Command.requestResponse)
						.build());

		Message<?> errorMessage = errorChannel.receive(10_000);

		assertThat(errorMessage).isNotNull()
				.isInstanceOf(ErrorMessage.class)
				.extracting(Message::getPayload)
				.isInstanceOf(MessageHandlingException.class)
				.satisfies((ex) -> assertThat((Exception) ex)
						.hasMessageContaining("io.rsocket.exceptions.ApplicationErrorException: " +
								"No handler for destination 'invalid'"));

		disposable.dispose();
	}

	@Configuration
	@EnableIntegration
	public static class ClientConfig {

		@Bean
		public MessageHandler rsocketOutboundGateway() {
			RSocketOutboundGateway rsocketOutboundGateway =
					new RSocketOutboundGateway(TcpClientTransport.create(PORT),
							new FunctionExpression<Message<?>>((m) -> m.getHeaders().get(ROUTE_HEADER)));
			rsocketOutboundGateway.setCommandExpression(
					new FunctionExpression<Message<?>>((m) -> m.getHeaders().get(COMMAND_HEADER)));
			rsocketOutboundGateway.setFactoryConfigurer((factory) -> factory.frameDecoder(PayloadDecoder.ZERO_COPY));
			rsocketOutboundGateway.setStrategiesConfigurer((strategies) ->
					strategies.decoder(StringDecoder.allMimeTypes())
							.encoder(CharSequenceEncoder.allMimeTypes())
							.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT)));
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
	static class ServerConfig {

		@Bean
		public ServerController controller() {
			return new ServerController();
		}

		@Bean
		public MessageHandlerAcceptor messageHandlerAcceptor() {
			MessageHandlerAcceptor acceptor = new MessageHandlerAcceptor();
			acceptor.setRSocketStrategies(rsocketStrategies());
			return acceptor;
		}

		@Bean
		public RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.builder()
					.decoder(StringDecoder.allMimeTypes())
					.encoder(CharSequenceEncoder.allMimeTypes())
					.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
					.build();
		}

	}

	@Controller
	static class ServerController {

		final ReplayProcessor<String> fireForgetPayloads = ReplayProcessor.create();

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

	}

}
