/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.webflux.outbound;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;

import java.util.List;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.webflux.support.ClientHttpResponseBodyExtractor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Shiliang Li
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class WebFluxRequestExecutingMessageHandlerTests {

	@Test
	public void testReactiveReturn() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					response.setStatusCode(HttpStatus.OK);
					return Mono.defer(response::setComplete);
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		FluxMessageChannel ackChannel = new FluxMessageChannel();
		reactiveHandler.setOutputChannel(ackChannel);
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());

		StepVerifier.create(ackChannel, 2)
				.assertNext(m -> assertThat(m, hasHeader(HttpHeaders.STATUS_CODE, HttpStatus.OK)))
				.assertNext(m -> assertThat(m, hasHeader(HttpHeaders.STATUS_CODE, HttpStatus.OK)))
				.then(() ->
						((Subscriber<?>) TestUtils.getPropertyValue(ackChannel, "subscribers", List.class).get(0))
								.onComplete())
				.verifyComplete();
	}

	@Test
	public void testReactiveErrorOneWay() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					response.setStatusCode(HttpStatus.UNAUTHORIZED);
					return Mono.defer(response::setComplete);
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);
		reactiveHandler.setExpectReply(false);

		QueueChannel errorChannel = new QueueChannel();
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world")
				.setErrorChannel(errorChannel)
				.build());

		Message<?> errorMessage = errorChannel.receive(10000);

		assertNotNull(errorMessage);
		assertThat(errorMessage, instanceOf(ErrorMessage.class));
		Throwable throwable = (Throwable) errorMessage.getPayload();
		assertThat(throwable.getMessage(), containsString("401 Unauthorized"));
	}

	@Test
	public void testReactiveConnectErrorOneWay() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					throw new RuntimeException("Intentional connection error");
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);
		reactiveHandler.setExpectReply(false);

		QueueChannel errorChannel = new QueueChannel();
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world")
				.setErrorChannel(errorChannel)
				.build());

		Message<?> errorMessage = errorChannel.receive(10000);

		assertNotNull(errorMessage);
		assertThat(errorMessage, instanceOf(ErrorMessage.class));
		Throwable throwable = (Throwable) errorMessage.getPayload();
		assertThat(throwable.getMessage(), containsString("Intentional connection error"));
	}

	@Test
	public void testServiceUnavailableWithoutBody() {
		ClientHttpConnector httpConnector =
				new HttpHandlerConnector((request, response) -> {
					response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
					return Mono.defer(response::setComplete);
				});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
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
		assertNotNull(errorMessage);

		Object payload = errorMessage.getPayload();
		assertThat(payload, instanceOf(MessageHandlingException.class));

		Exception exception = (Exception) payload;
		assertThat(exception.getCause(), instanceOf(WebClientResponseException.class));
		assertThat(exception.getMessage(), containsString("503 Service Unavailable"));

		Message<?> replyMessage = errorChannel.receive(10);
		assertNull(replyMessage);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFluxReply() {
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

		String destinationUri = "http://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		QueueChannel replyChannel = new QueueChannel();
		reactiveHandler.setOutputChannel(replyChannel);
		reactiveHandler.setExpectedResponseType(String.class);
		reactiveHandler.setReplyPayloadToFlux(true);

		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());

		Message<?> receive = replyChannel.receive(10_000);

		assertNotNull(receive);

		assertThat(receive.getPayload(), instanceOf(Flux.class));

		Flux<String> flux = (Flux<String>) receive.getPayload();

		StepVerifier.create(flux)
				.expectNext("foo", "bar", "baz")
				.verifyComplete();
	}

	@Test
	public void testClientHttpResponseAsReply() {
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

		String destinationUri = "http://www.springsource.org/spring-integration";
		WebFluxRequestExecutingMessageHandler reactiveHandler =
				new WebFluxRequestExecutingMessageHandler(destinationUri, webClient);

		QueueChannel replyChannel = new QueueChannel();
		reactiveHandler.setOutputChannel(replyChannel);
		reactiveHandler.setBodyExtractor(new ClientHttpResponseBodyExtractor());

		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());

		Message<?> receive = replyChannel.receive(10_000);

		assertNotNull(receive);

		assertThat(receive.getPayload(), instanceOf(ClientHttpResponse.class));

		ClientHttpResponse response = (ClientHttpResponse) receive.getPayload();


		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());

		StepVerifier.create(
				response.getBody()
						.map(dataBuffer -> new String(dataBuffer.asByteBuffer().array())))
				.expectNext("foo", "bar", "baz")
				.verifyComplete();
	}


}
