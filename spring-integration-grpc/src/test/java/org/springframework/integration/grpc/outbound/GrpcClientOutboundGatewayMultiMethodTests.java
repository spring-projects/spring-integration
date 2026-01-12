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
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.grpc.GrpcHeaders;
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
	private static final String serverName = InProcessServerBuilder.generateName();

	@Autowired
	private ManagedChannel channel;

	@Autowired
	private GrpcOutboundGateway grpcOutboundGateway;

	// ==================== SimpleBlockingStub Tests ====================

	@Test
	void testGrpcClientOutboundGatewayNoExpressionSet() {
		HelloRequest request = HelloRequest.newBuilder()
				.setName("Jane")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				setHeader(GrpcHeaders.SERVICE_METHOD, "SayHello").
				build();

		Object response = this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response).isInstanceOf(HelloReply.class);
		HelloReply reply = (HelloReply) response;
		assertThat(reply.getMessage()).isEqualTo("Hello, Jane!");
	}

	@Test
	void testSayHelloWithBlockingStub() {
		GrpcOutboundGateway gateway = setupGateway("SayHello");
		validateBlockingStub(gateway, "SayHello");
	}

	@Test
	void testSayHelloMixedCapitalizationWithBlockingStub() {
		GrpcOutboundGateway gateway = setupGateway("SaYHellO");
		validateBlockingStub(gateway, "SayHello");
	}

	@Test
	void testStreamHelloWithAsyncStub() {
		GrpcOutboundGateway gateway = setupGateway("StreamSayHello");

		// Create request
		HelloRequest request = HelloRequest.newBuilder()
				.setName("Stream")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		// Invoke gateway - async stub returns a Flux
		Object response = gateway.handleRequestMessage(requestMessage);

		// Verify response is a Flux
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		assertThat(replies).isNotNull().hasSize(3);
		assertThat(replies.get(0).getMessage()).contains("Stream");
	}

	// ==================== SimpleStub (Async) Tests ====================

	@Test
	void testBidirectionalWithAsyncStub() {
		setupGateway("BidiStreamHello");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);

		verifyBiDirectional(MessageBuilder.withPayload(requestFlux).build());
	}

	@Test
	void testBidirectionalCollectionWithAsyncStub() {
		setupGateway("BidiStreamHello");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();
		List<HelloRequest> requests = Arrays.asList(request1, request2, request3);

		verifyBiDirectional(MessageBuilder.withPayload(requests).build());
	}

	@Test
	void testBidirectionalPojoWithAsyncStub() {
		setupGateway("BidiStreamHello");

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
		GrpcOutboundGateway gateway = setupGateway("StreamSayHello");

		// Create request
		HelloRequest request = HelloRequest.newBuilder()
				.setName("StreamAsync")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		Object response = gateway.handleRequestMessage(requestMessage);
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
		GrpcOutboundGateway gateway = setupGateway("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);
		Message<?> requestMessage = MessageBuilder.withPayload(requestFlux).build();

		verifyClientStreamingWithAsyncStub(gateway.handleRequestMessage(requestMessage));

	}

	@Test
	void testClientStreamingPOJOWithAsyncStub() {
		GrpcOutboundGateway gateway = setupGateway("HelloToEveryOne");

		HelloRequest request = HelloRequest.newBuilder().setName("Alice").build();
		Message<?> requestMessage = MessageBuilder.withPayload(request).build();

		verifyClientStreamingWithAsyncStub(gateway.handleRequestMessage(requestMessage), "Hello to Alice!");

	}

	@Test
	void testClientStreamingCollectionWithAsyncStub() {
		GrpcOutboundGateway gateway = setupGateway("HelloToEveryOne");

		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		List<HelloRequest> requestList = Arrays.asList(request1, request2, request3);
		Message<?> requestMessage = MessageBuilder.withPayload(requestList).build();

		verifyClientStreamingWithAsyncStub(gateway.handleRequestMessage(requestMessage));
	}

	private void verifyClientStreamingWithAsyncStub(Object response) {
		verifyClientStreamingWithAsyncStub(response, "Hello to Alice, Bob, Charlie!");
	}

	private void verifyClientStreamingWithAsyncStub(Object response, String expectedResult) {
		assertThat(response).isInstanceOf(reactor.core.publisher.Mono.class);

		@SuppressWarnings("unchecked")
		reactor.core.publisher.Mono<HelloReply> monoResponse = (reactor.core.publisher.Mono<HelloReply>) response;
		HelloReply reply = monoResponse.block();

		assertThat(reply).isNotNull();
		assertThat(reply.getMessage()).isEqualTo(expectedResult);
	}

	/**
	 * Create and configure a GrpcOutboundGateway.
	 */
	private GrpcOutboundGateway setupGateway(String methodName) {
		this.grpcOutboundGateway.setMethodNameExpression(new LiteralExpression(methodName));
		return this.grpcOutboundGateway;
	}

	private void validateBlockingStub(GrpcOutboundGateway gateway, String name) {
		// Create request
		HelloRequest request = HelloRequest.newBuilder()
				.setName(name)
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		// Invoke gateway
		Object response = gateway.handleRequestMessage(requestMessage);

		// Verify response
		assertThat(response).isInstanceOf(HelloReply.class);
		HelloReply reply = (HelloReply) response;
		assertThat(reply.getMessage()).isEqualTo("Hello, " + name + "!");
	}

	// ==================== Mock gRPC Service Implementation ====================

	/**
	 * Simple implementation of the gRPC service for testing.
	 */
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

			// Normal case: return multiple responses
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
					// Echo back each request
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
					// Collect all names from the stream
					names.add(value.getName());
				}

				@Override
				public void onError(Throwable t) {
					responseObserver.onError(t);
				}

				@Override
				public void onCompleted() {
					// Send single aggregated response with all names
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

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		Server server() throws Exception {
			return InProcessServerBuilder
					.forName(serverName)
					.directExecutor()
					.addService(new SimpleServiceImpl())
					.build()
					.start();
		}

		@Bean
		ManagedChannel channel() {
			return InProcessChannelBuilder
					.forName(serverName)
					.directExecutor()
					.build();
		}

		@Bean
		GrpcOutboundGateway grpcOutboundGateway(ManagedChannel channel) {
			return new GrpcOutboundGateway(channel, TestHelloWorldGrpc.class);
		}
	}

}

