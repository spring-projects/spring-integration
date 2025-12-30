/*
 * Copyright 2025-present the original author or authors.
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.grpc.proto.HelloReply;
import org.springframework.integration.grpc.proto.HelloRequest;
import org.springframework.integration.grpc.proto.TestHelloWorldGrpc;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.1
 */
class GrpcClientOutboundGatewayTests {

	private Server server;

	private ManagedChannel channel;

	private final String serverName = InProcessServerBuilder.generateName();

	private GenericApplicationContext applicationContext;

	@BeforeEach
	void setUp() throws IOException {
		// Create application context for Spring Integration
		applicationContext = new GenericApplicationContext();
		applicationContext.refresh();

		// Create and start in-process gRPC server
		server = InProcessServerBuilder
				.forName(serverName)
				.directExecutor()
				.addService(new SimpleServiceImpl())
				.build()
				.start();

		// Create in-process channel
		channel = InProcessChannelBuilder
				.forName(serverName)
				.directExecutor()
				.build();
	}

	@AfterEach
	void tearDown() throws InterruptedException {
		if (channel != null) {
			channel.shutdownNow();
			channel.awaitTermination(5, TimeUnit.SECONDS);
		}
		if (server != null) {
			server.shutdownNow();
			server.awaitTermination(5, TimeUnit.SECONDS);
		}
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	// ==================== SimpleBlockingStub Tests ====================

	@Test
	void testSayHelloWithBlockingStub() {
		GrpcOutboundGateway gateway = createGateway("SayHello");
		validateBlockingStub(gateway, "SayHello");
	}

	@Test
	void testSayHelloMixedCapitalizationWithBlockingStub() {
		GrpcOutboundGateway gateway = createGateway("SaYHellO");
		validateBlockingStub(gateway, "SayHello");
	}

	/**
	 * Create and configure a GrpcOutboundGateway.
	 */
	private GrpcOutboundGateway createGateway(String methodName) {
		GrpcOutboundGateway gateway = new GrpcOutboundGateway(this.channel, TestHelloWorldGrpc.class, new LiteralExpression(methodName));
		gateway.setBeanFactory(applicationContext);
		gateway.setApplicationContext(applicationContext);
		gateway.setAsyncTimeout(5, TimeUnit.SECONDS);
		gateway.afterPropertiesSet();
		return gateway;
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

	@Test
	void testStreamHelloWithAsyncStub() {
		GrpcOutboundGateway gateway = createGateway("StreamSayHello");

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

		// Verify we got 3 responses
		assertThat(replies).isNotNull();
		assertThat(replies).hasSize(3);
		assertThat(replies.get(0).getMessage()).contains("Stream");
	}

	// ==================== SimpleStub (Async) Tests ====================

	@Test
	void testBidirectionalStreamingWithAsyncStub() {
		GrpcOutboundGateway gateway = createGateway("BidiStreamHello");

		// Create request flux with multiple items
		HelloRequest request1 = HelloRequest.newBuilder().setName("Alice").build();
		HelloRequest request2 = HelloRequest.newBuilder().setName("Bob").build();
		HelloRequest request3 = HelloRequest.newBuilder().setName("Charlie").build();

		Flux<HelloRequest> requestFlux = Flux.just(request1, request2, request3);
		Message<?> requestMessage = MessageBuilder.withPayload(requestFlux).
				build();

		// Invoke gateway - returns Flux for bidirectional streaming
		Object response = gateway.handleRequestMessage(requestMessage);
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		// Verify responses - should echo back the names
		assertThat(replies).isNotNull();
		assertThat(replies).hasSize(3);
		assertThat(replies.get(0).getMessage()).isEqualTo("Hello Alice");
		assertThat(replies.get(1).getMessage()).isEqualTo("Hello Bob");
		assertThat(replies.get(2).getMessage()).isEqualTo("Hello Charlie");
	}

	@Test
	void testServerStreamingWithAsyncStub() {
		GrpcOutboundGateway gateway = createGateway("StreamSayHello");

		// Create request
		HelloRequest request = HelloRequest.newBuilder()
				.setName("StreamAsync")
				.build();

		Message<?> requestMessage = MessageBuilder.withPayload(request).
				build();

		// Invoke gateway - returns Flux for server streaming
		Object response = gateway.handleRequestMessage(requestMessage);
		assertThat(response).isInstanceOf(Flux.class);

		@SuppressWarnings("unchecked")
		Flux<HelloReply> fluxResponse = (Flux<HelloReply>) response;
		List<HelloReply> replies = fluxResponse.collectList().block();

		// Verify response - should receive 3 streamed responses
		assertThat(replies).isNotNull();
		assertThat(replies).hasSize(3);
		assertThat(replies.get(0).getMessage()).isEqualTo("Hello, StreamAsync! (1/3)");
		assertThat(replies.get(1).getMessage()).isEqualTo("Hello, StreamAsync! (2/3)");
		assertThat(replies.get(2).getMessage()).isEqualTo("Hello, StreamAsync! (3/3)");
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
			// Special case: if name is "single", return only one response
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

	}

}

