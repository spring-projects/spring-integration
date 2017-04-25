/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.http.outbound;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.time.Duration;

import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.ReactiveChannel;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * @author Shiliang Li
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ReactiveHttpRequestExecutingMessageHandlerTests {

	@Test
	public void testReactiveReturn() throws Throwable {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.OK);
			return Mono.empty()
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
		ReactiveHttpRequestExecutingMessageHandler reactiveHandler =
				new ReactiveHttpRequestExecutingMessageHandler(destinationUri, webClient);

		ReactiveChannel ackChannel = new ReactiveChannel();
		reactiveHandler.setOutputChannel(ackChannel);
		reactiveHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());

		Message<?> ack = Mono.from(ackChannel).block(Duration.ofSeconds(10));

		assertNotNull(ack);
		assertNotNull(ack.getHeaders());
		assertEquals(ack.getHeaders().get(HttpHeaders.STATUS_CODE), HttpStatus.OK);
	}

	@Test
	public void testReactiveErrorOneWay() throws Throwable {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return Mono.empty()
					.then(Mono.defer(response::setComplete));
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
		ReactiveHttpRequestExecutingMessageHandler reactiveHandler =
				new ReactiveHttpRequestExecutingMessageHandler(destinationUri, webClient);
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
	public void testReactiveConnectErrorOneWay() throws Throwable {
		ClientHttpConnector httpConnector = new HttpHandlerConnector((request, response) -> {
			throw new RuntimeException("Intentional connection error");
		});

		WebClient webClient = WebClient.builder()
				.clientConnector(httpConnector)
				.build();

		String destinationUri = "http://www.springsource.org/spring-integration";
		ReactiveHttpRequestExecutingMessageHandler reactiveHandler =
				new ReactiveHttpRequestExecutingMessageHandler(destinationUri, webClient);
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


}
