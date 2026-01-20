/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.grpc.outbound;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.grpc.GrpcHeaders;
import org.springframework.integration.grpc.TestInProcessConfiguration;
import org.springframework.integration.grpc.proto.HelloReply;
import org.springframework.integration.grpc.proto.HelloRequest;
import org.springframework.integration.grpc.proto.TestHelloWorldGrpc;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GrpcClientOutboundGatewayMultiMethodTests {

	@Autowired
	private GrpcOutboundGateway grpcOutboundGateway;

	@Test
	void testGrpcClientOutboundGatewayNoExpressionSet() {
		this.grpcOutboundGateway.setAsync(false);

		HelloRequest request = HelloRequest.newBuilder()
				.setName("Jane")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request)
				.setHeader(GrpcHeaders.SERVICE_METHOD, "SayHello")
				.build();

		HelloReply response = (HelloReply) this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response.getMessage()).isEqualTo("Hello, Jane!");
	}

	@Test
	void testSayHelloWithMono() {
		this.grpcOutboundGateway.setMethodName("SayHello");
		HelloRequest request = HelloRequest.newBuilder()
				.setName("Jim")
				.build();

		Message<?> requestMessage = new GenericMessage<>(request);

		@SuppressWarnings("unchecked")
		Mono<HelloReply> monoResponse = (Mono<HelloReply>) this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		StepVerifier.create(monoResponse.map(HelloReply::getMessage))
				.expectNext("Hello, Jim!")
				.verifyComplete();
	}

	@Test
	void testSayHelloMixedCapitalizationWithBlockingStub() {
		this.grpcOutboundGateway.setMethodName("SaYHellO");
		this.grpcOutboundGateway.setAsync(false);

		HelloRequest request = HelloRequest.newBuilder()
				.setName("SayHello")
				.build();

		Message<?> requestMessage = new GenericMessage<>(request);

		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response)
				.asInstanceOf(type(HelloReply.class))
				.extracting(HelloReply::getMessage)
				.isEqualTo("Hello, SayHello!");
	}

	@Test
	void testStreamHelloWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("StreamSayHello");

		HelloRequest request = HelloRequest.newBuilder()
				.setName("Stream")
				.build();

		Message<?> requestMessage = new GenericMessage<>(request);

		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;

		StepVerifier.create(fluxResponse.map(HelloReply::getMessage))
				.expectNext("Hello, Stream! (1/3)", "Hello, Stream! (2/3)", "Hello, Stream! (3/3)")
				.verifyComplete();
	}

	@Test
	void testBidirectionalWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("BidiStreamHello");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);

		verifyBiDirectional(new GenericMessage<>(requestFlux));
	}

	@Test
	void testBidirectionalCollectionWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("BidiStreamHello");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		List<HelloRequest> requests = List.of(request1, request2, request3);

		verifyBiDirectional(new GenericMessage<>(requests));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testBidirectionalPojoWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("BidiStreamHello");

		HelloRequest request = HelloRequest.newBuilder().setName("Alice").build();

		Object response = this.grpcOutboundGateway.handleRequestMessage(new GenericMessage<>(request));

		assertThat(response).isInstanceOf(Flux.class);
		StepVerifier.create(((Flux<HelloReply>) response).map(HelloReply::getMessage))
				.expectNext("Hello Alice")
				.verifyComplete();
	}

	private void verifyBiDirectional(Message<?> requestMessage) {
		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response).isInstanceOf(Flux.class);
		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;

		StepVerifier.create(fluxResponse.map(HelloReply::getMessage))
				.expectNext("Hello Alice", "Hello Bob", "Hello Charlie")
				.verifyComplete();
	}

	@Test
	void testServerStreamingWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("StreamSayHello");

		HelloRequest request = HelloRequest.newBuilder()
				.setName("StreamAsync")
				.build();

		Message<?> requestMessage = new GenericMessage<>(request);

		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;

		StepVerifier.create(fluxResponse.map(HelloReply::getMessage))
				.expectNext("Hello, StreamAsync! (1/3)", "Hello, StreamAsync! (2/3)", "Hello, StreamAsync! (3/3)")
				.verifyComplete();
	}

	@Test
	void testClientStreamingWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);
		Message<?> requestMessage = new GenericMessage<>(requestFlux);

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage));

	}

	@Test
	void testClientStreamingArrayWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		HelloRequest[] requests = {request1, request2, request3};
		Message<?> requestMessage = new GenericMessage<>(requests);

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage));
	}

	@Test
	void testClientStreamingPOJOWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request = HelloRequest.newBuilder().setName("Alice").build();
		Message<?> requestMessage = new GenericMessage<>(request);

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage),
				"Hello to Alice!");

	}

	@Test
	void testClientStreamingCollectionWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		List<HelloRequest> requestList = List.of(request1, request2, request3);
		Message<?> requestMessage = new GenericMessage<>(requestList);

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage));
	}

	private static void verifyClientStreamingWithAsyncStub(Object response) {
		verifyClientStreamingWithAsyncStub(response, "Hello to Alice, Bob, Charlie!");
	}

	private static void verifyClientStreamingWithAsyncStub(Object response, String expectedResult) {
		assertThat(response).isInstanceOf(Mono.class);

		@SuppressWarnings("unchecked")
		Mono<HelloReply> monoResponse = (Mono<HelloReply>) response;
		StepVerifier.create(monoResponse.map(HelloReply::getMessage))
				.expectNext(expectedResult)
				.verifyComplete();
	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestInProcessConfiguration.class)
	@EnableIntegration
	static class TestConfig {

		@Bean
		SimpleServiceImpl simpleService() {
			return new SimpleServiceImpl();
		}

		@Bean
		GrpcOutboundGateway grpcOutboundGateway(ManagedChannel channel) {
			return new GrpcOutboundGateway(channel, TestHelloWorldGrpc.class);
		}

		private static class SimpleServiceImpl extends TestHelloWorldGrpc.TestHelloWorldImplBase {

			@Override
			public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
				HelloReply reply = HelloReply.newBuilder()
						.setMessage("Hello, " + request.getName() + "!")
						.build();
				responseObserver.onNext(reply);
				responseObserver.onCompleted();
			}

			@Override
			public void streamSayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
				if ("single".equals(request.getName())) {
					HelloReply reply = HelloReply.newBuilder()
							.setMessage("Hello, " + request.getName() + "!")
							.build();
					responseObserver.onNext(reply);
					responseObserver.onCompleted();
					return;
				}

				for (int i = 1; i <= 3; i++) {
					HelloReply reply = HelloReply.newBuilder()
							.setMessage("Hello, " + request.getName() + "! (" + i + "/3)")
							.build();
					responseObserver.onNext(reply);
				}
				responseObserver.onCompleted();
			}

			@Override
			public StreamObserver<HelloRequest> bidiStreamHello(StreamObserver<HelloReply> responseObserver) {
				return new StreamObserver<>() {

					@Override
					public void onNext(HelloRequest value) {
						HelloReply reply = HelloReply.newBuilder()
								.setMessage("Hello " + value.getName())
								.build();
						responseObserver.onNext(reply);
					}

					@Override
					public void onError(Throwable t) {
						responseObserver.onError(t);
					}

					@Override
					public void onCompleted() {
						responseObserver.onCompleted();
					}

				};
			}

			@Override
			public StreamObserver<HelloRequest> helloToEveryOne(StreamObserver<HelloReply> responseObserver) {
				return new StreamObserver<>() {

					private final java.util.List<String> names = new java.util.ArrayList<>();

					@Override
					public void onNext(HelloRequest value) {
						names.add(value.getName());
					}

					@Override
					public void onError(Throwable t) {
						responseObserver.onError(t);
					}

					@Override
					public void onCompleted() {
						String allNames = String.join(", ", names);
						HelloReply reply = HelloReply.newBuilder()
								.setMessage("Hello to " + allNames + "!")
								.build();
						responseObserver.onNext(reply);
						responseObserver.onCompleted();
					}

				};
			}

		}

	}

}

