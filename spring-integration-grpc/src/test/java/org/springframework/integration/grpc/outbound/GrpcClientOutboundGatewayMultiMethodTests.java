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

import java.util.Arrays;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
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
		HelloRequest request = HelloRequest.newBuilder()
				.setName("Jane")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				setHeader(GrpcHeaders.SERVICE_METHOD, "SayHello").
				build();
		this.grpcOutboundGateway.setAsync(false);
		HelloReply response = (HelloReply) this.grpcOutboundGateway.handleRequestMessage(requestMessage);
		assertThat(response.getMessage()).isEqualTo("Hello, Jane!");
	}

	@Test
	void testSayHelloWithBlockingStub() {
		this.grpcOutboundGateway.setMethodName("SayHello");

		this.grpcOutboundGateway.setAsync(false);
		validateBlockingStub(this.grpcOutboundGateway, "SayHello");
	}

	@Test
	void testSayHelloWithMono() {
		this.grpcOutboundGateway.setMethodName("SayHello");
		HelloRequest request = HelloRequest.newBuilder()
				.setName("Jim")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		@SuppressWarnings("unchecked")
		Mono<HelloReply> monoResponse = (Mono<HelloReply>) this.grpcOutboundGateway.handleRequestMessage(requestMessage);
		HelloReply response = monoResponse.block();
		assertThat(response.getMessage()).isEqualTo("Hello, Jim!");
	}

	@Test
	void testSayHelloMixedCapitalizationWithBlockingStub() {
		this.grpcOutboundGateway.setMethodName("SaYHellO");
		this.grpcOutboundGateway.setAsync(false);
		validateBlockingStub(this.grpcOutboundGateway, "SayHello");
	}

	@Test
	void testStreamHelloWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("StreamSayHello");

		HelloRequest request = HelloRequest.newBuilder()
				.setName("Stream")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		assertThat(replies).isNotNull().hasSize(3);
		assertThat(replies.get(0).getMessage()).contains("Stream");
	}

	@Test
	void testBidirectionalWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("BidiStreamHello");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);

		verifyBiDirectional(MessageBuilder.withPayload(requestFlux).build());
	}

	@Test
	void testBidirectionalCollectionWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("BidiStreamHello");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		List<HelloRequest> requests = Arrays.asList(request1, request2, request3);

		verifyBiDirectional(MessageBuilder.withPayload(requests).build());
	}

	@Test
	void testBidirectionalPojoWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("BidiStreamHello");

		HelloRequest request = HelloRequest.newBuilder().setName("Alice").build();

		Object response = this.grpcOutboundGateway.handleRequestMessage(MessageBuilder.withPayload(request).build());
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		assertThat(replies).isNotNull().hasSize(1);
		assertThat(replies.get(0).getMessage()).isEqualTo("Hello Alice");
	}

	private void verifyBiDirectional(Message<?> requestMessage) {
		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		assertThat(replies).isNotNull().hasSize(3);
		assertThat(replies.get(0).getMessage()).isEqualTo("Hello Alice");
		assertThat(replies.get(1).getMessage()).isEqualTo("Hello Bob");
		assertThat(replies.get(2).getMessage()).isEqualTo("Hello Charlie");
	}

	@Test
	void testServerStreamingWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("StreamSayHello");

		HelloRequest request = HelloRequest.newBuilder()
				.setName("StreamAsync")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		assertThat(replies).isNotNull().hasSize(3);
		assertThat(replies.get(0).getMessage()).isEqualTo("Hello, StreamAsync! (1/3)");
		assertThat(replies.get(1).getMessage()).isEqualTo("Hello, StreamAsync! (2/3)");
		assertThat(replies.get(2).getMessage()).isEqualTo("Hello, StreamAsync! (3/3)");
	}

	@Test
	void testClientStreamingWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);
		Message<?> requestMessage = MessageBuilder.withPayload(requestFlux).build();

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage));

	}

	@Test
	void testClientStreamingArrayWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		HelloRequest[] requests = { request1, request2, request3 };
		Message<?> requestMessage = MessageBuilder.withPayload(requests).build();

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage));
	}

	@Test
	void testClientStreamingPOJOWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request = HelloRequest.newBuilder().setName("Alice").build();
		Message<?> requestMessage = MessageBuilder.withPayload(request).build();

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage), "Hello to Alice!");

	}

	@Test
	void testClientStreamingCollectionWithAsyncStub() {
		this.grpcOutboundGateway.setMethodName("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		List<HelloRequest> requestList = Arrays.asList(request1, request2, request3);
		Message<?> requestMessage = MessageBuilder.withPayload(requestList).build();

		verifyClientStreamingWithAsyncStub(this.grpcOutboundGateway.handleRequestMessage(requestMessage));
	}

	private static void verifyClientStreamingWithAsyncStub(Object response) {
		verifyClientStreamingWithAsyncStub(response, "Hello to Alice, Bob, Charlie!");
	}

	private static void verifyClientStreamingWithAsyncStub(Object response, String expectedResult) {
		assertThat(response).isInstanceOf(reactor.core.publisher.Mono.class);

		@SuppressWarnings("unchecked")
		Mono<HelloReply> monoResponse = (reactor.core.publisher.Mono<HelloReply>) response;
		HelloReply reply = monoResponse.block();

		assertThat(reply).isNotNull();
		assertThat(reply.getMessage()).isEqualTo(expectedResult);
	}

	@SuppressWarnings("unchecked")
	private static void validateBlockingStub(GrpcOutboundGateway gateway, String name) {
		HelloRequest request = HelloRequest.newBuilder()
				.setName(name)
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		Object response = gateway.handleRequestMessage(requestMessage);

		assertThat(response).isInstanceOf(HelloReply.class);
		HelloReply reply = (HelloReply) response;
		assertThat(reply.getMessage()).isEqualTo("Hello, " + name + "!");
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

