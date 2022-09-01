/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.webflux.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.webflux.support.ClientHttpResponseBodyExtractor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Shiliang Li
 * @author Artem Bilan
 * @author David Graff
 *
 * @since 5.0
 */
class WebFluxRequestExecutingMessageHandlerTests {

	@Test
	void testReactiveReturn() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					response.setStatusCode(HttpStatus.OK);
					return Mono.defer(response::setComplete);
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		FluxMessageChannel ackChannel = new FluxMessageChannel();
		reactiveHandler.setOutputChannel(ackChannel);
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());

		StepVerifier.create(ackChannel, 2)
				.assertNext(m -> assertThat(m.getHeaders()).containsEntry(HttpHeaders.STATUS_CODE, HttpStatus.OK))
				.assertNext(m -> assertThat(m.getHeaders()).containsEntry(HttpHeaders.STATUS_CODE, HttpStatus.OK))
				.expectNoEvent(Duration.ofMillis(100))
				.thenCancel()
				.verify(Duration.ofSeconds(10));
	}

	@Test
	void testReactiveErrorOneWay() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					response.setStatusCode(HttpStatus.UNAUTHORIZED);
					return Mono.defer(response::setComplete);
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);
		reactiveHandler.setExpectReply(false);

		QueueChannel errorChannel = new QueueChannel();
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world")
				.setErrorChannel(errorChannel)
				.build());

		Message<?> errorMessage = errorChannel.receive(10000);

		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage).isInstanceOf(ErrorMessage.class);
		Throwable throwable = (Throwable) errorMessage.getPayload();
		assertThat(throwable).isInstanceOf(MessageHandlingException.class);
		assertThat(throwable.getCause()).isInstanceOf(WebClientResponseException.Unauthorized.class);
		assertThat(throwable).hasStackTraceContaining("401 Unauthorized");
	}

	@Test
	void testReactiveConnectErrorOneWay() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					throw new RuntimeException("Intentional connection error");
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);
		reactiveHandler.setExpectReply(false);

		QueueChannel errorChannel = new QueueChannel();
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world")
				.setErrorChannel(errorChannel)
				.build());

		Message<?> errorMessage = errorChannel.receive(10000);

		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage).isInstanceOf(ErrorMessage.class);
		Throwable throwable = (Throwable) errorMessage.getPayload();
		assertThat(throwable).hasStackTraceContaining("Intentional connection error");
	}

	@Test
	void testServiceUnavailableWithoutBody() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
					return Mono.defer(response::setComplete);
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel errorChannel = new QueueChannel();
		WebFluxRequestExecutingMessageHandler messageHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);
		messageHandler.setOutputChannel(replyChannel);

		Message<String> requestMessage =
				MessageBuilder.withPayload("test")
						.setErrorChannel(errorChannel)
						.build();

		messageHandler.handleMessage(requestMessage);

		Message<?> errorMessage = errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();

		Object payload = errorMessage.getPayload();
		assertThat(payload).isInstanceOf(MessageHandlingException.class);

		Exception exception = (Exception) payload;
		assertThat(exception).isInstanceOf(MessageHandlingException.class);
		assertThat(exception.getCause()).isInstanceOf(WebClientResponseException.ServiceUnavailable.class);
		assertThat(exception).hasStackTraceContaining("503 Service Unavailable");

		Message<?> replyMessage = errorChannel.receive(10);
		assertThat(replyMessage).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testFluxReply() {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

			DataBufferFactory bufferFactory = response.bufferFactory();

			Mono<DataBuffer> data = Mono.just(bufferFactory.wrap("foo\nbar\nbaz".getBytes()));

			return response.writeWith(data)
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		QueueChannel replyChannel = new QueueChannel();
		reactiveHandler.setOutputChannel(replyChannel);
		reactiveHandler.setExpectedResponseType(String.class);
		reactiveHandler.setReplyPayloadToFlux(true);

		reactiveHandler.handleMessage(MessageBuilder.withPayload(Mono.just("hello, world")).build());

		Message<?> receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull();

		assertThat(receive.getPayload()).isInstanceOf(Flux.class);

		Flux<String> flux = (Flux<String>) receive.getPayload();

		StepVerifier.create(flux)
				.expectNext("foo", "bar", "baz")
				.verifyComplete();
	}

	@Test
	void testClientHttpResponseAsReply() {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

			DataBufferFactory bufferFactory = response.bufferFactory();

			Flux<DataBuffer> data =
					Flux.just(bufferFactory.wrap("foo".getBytes()),
							bufferFactory.wrap("bar".getBytes()),
							bufferFactory.wrap("baz".getBytes()));

			return response.writeWith(data)
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		QueueChannel replyChannel = new QueueChannel();
		reactiveHandler.setOutputChannel(replyChannel);
		reactiveHandler.setBodyExtractor(new ClientHttpResponseBodyExtractor());

		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());

		Message<?> receive = replyChannel.receive(10_000);

		assertThat(receive).isNotNull();

		assertThat(receive.getPayload()).isInstanceOf(ClientHttpResponse.class);

		ClientHttpResponse response = (ClientHttpResponse) receive.getPayload();


		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);

		StepVerifier.create(
						response.getBody()
								.map(dataBuffer -> new String(dataBuffer.toByteBuffer().array())))
				.expectNext("foo", "bar", "baz")
				.verifyComplete();
	}

	@Test
	void testClientHttpResponseErrorAsReply() {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.NOT_FOUND);
			response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

			DataBufferFactory bufferFactory = response.bufferFactory();

			Flux<DataBuffer> data =
					Flux.just(
							bufferFactory.wrap("{".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("  \"error\": \"Not Found\",".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("  \"message\": \"404 NOT_FOUND\",".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("  \"path\": \"/spring-integration\",".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("  \"status\": 404,".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("  \"timestamp\": \"1970-01-01T00:00:00.000+00:00\",".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("  \"trace\": \"some really\nlong\ntrace\",".getBytes(StandardCharsets.UTF_8)),
							bufferFactory.wrap("}".getBytes(StandardCharsets.UTF_8))
					);

			return response.writeWith(data)
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		QueueChannel replyChannel = new QueueChannel();
		QueueChannel errorChannel = new QueueChannel();
		reactiveHandler.setOutputChannel(replyChannel);
		reactiveHandler.setBodyExtractor(new ClientHttpResponseBodyExtractor());

		final Message<?> message =
				MessageBuilder.withPayload("hello, world")
						.setErrorChannel(errorChannel)
						.build();
		reactiveHandler.handleMessage(message);

		Message<?> errorMessage = errorChannel.receive(10_000);

		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage).isInstanceOf(ErrorMessage.class);
		final Throwable throwable = (Throwable) errorMessage.getPayload();
		assertThat(throwable).isInstanceOf(MessageHandlingException.class);
		assertThat(throwable.getCause()).isInstanceOf(WebClientResponseException.NotFound.class);
		assertThat(throwable).hasStackTraceContaining("404 Not Found");
	}

	@Test
	void testMaxInMemorySizeExceeded() {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.OK);

			DataBufferFactory bufferFactory = response.bufferFactory();

			Mono<DataBuffer> data = Mono.just(bufferFactory.wrap("test".getBytes()));

			return response.writeWith(data)
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(clientCodecConfigurer -> clientCodecConfigurer
								.defaultCodecs()
								.maxInMemorySize(1))
						.build())
				.build();

		String destinationUri = "https://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		reactiveHandler.setExpectedResponseType(String.class);

		QueueChannel errorChannel = new QueueChannel();
		reactiveHandler.handleMessage(MessageBuilder.withPayload("").setErrorChannel(errorChannel).build());

		Message<?> errorMessage = errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();

		Object payload = errorMessage.getPayload();
		assertThat(payload).isInstanceOf(MessageHandlingException.class)
				.extracting("cause")
				.isInstanceOf(WebClientResponseException.class)
				.extracting("cause")
				.isInstanceOf(DataBufferLimitException.class)
				.extracting("message")
				.isEqualTo("Exceeded limit on max bytes to buffer : 1");
	}

}
