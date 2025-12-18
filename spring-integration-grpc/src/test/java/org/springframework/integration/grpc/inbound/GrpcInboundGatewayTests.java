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

package org.springframework.integration.grpc.inbound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.grpc.GrpcHeaders;
import org.springframework.integration.grpc.TestInProcessConfiguration;
import org.springframework.integration.grpc.proto.HelloReply;
import org.springframework.integration.grpc.proto.HelloRequest;
import org.springframework.integration.grpc.proto.TestHelloWorldGrpc;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
class GrpcInboundGatewayTests {

	@Autowired
	TestHelloWorldGrpc.TestHelloWorldBlockingStub testHelloWorldBlockingStub;

	@Autowired
	TestHelloWorldGrpc.TestHelloWorldFutureStub testHelloWorldFutureStub;

	@Autowired
	TestHelloWorldGrpc.TestHelloWorldStub testHelloWorldStub;

	@Test
	void unary() {
		HelloReply reply = this.testHelloWorldBlockingStub.sayHello(newHelloRequest("World"));
		assertThat(reply).extracting(HelloReply::getMessage).isEqualTo("Hello World");
	}

	@Test
	void unaryFuture() throws ExecutionException, InterruptedException {
		ListenableFuture<HelloReply> reply = this.testHelloWorldFutureStub.sayHello(newHelloRequest("Future World"));
		assertThat(reply.get()).extracting(HelloReply::getMessage).isEqualTo("Hello Future World");
	}

	@Test
	void unaryAsync() throws InterruptedException {
		AtomicReference<HelloReply> reply = new AtomicReference<>();
		CountDownLatch replyLatch = new CountDownLatch(1);
		this.testHelloWorldStub.sayHello(newHelloRequest("Observed World"), newReplyObserver(reply::set, replyLatch));

		assertThat(replyLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(reply.get()).extracting(HelloReply::getMessage).isEqualTo("Hello Observed World");
	}

	@Test
	void serverStreaming() throws InterruptedException {
		List<HelloReply> replies = new ArrayList<>();
		CountDownLatch replyLatch = new CountDownLatch(1);
		this.testHelloWorldStub.streamSayHello(newHelloRequest("Stream World"),
				newReplyObserver(replies::add, replyLatch));

		assertThat(replyLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(replies.get(0)).extracting(HelloReply::getMessage).isEqualTo("Hello Stream World");
		assertThat(replies.get(1)).extracting(HelloReply::getMessage).isEqualTo("Hello again!");
	}

	@Test
	void clientStreaming() throws InterruptedException {
		AtomicReference<HelloReply> reply = new AtomicReference<>();
		CountDownLatch replyLatch = new CountDownLatch(1);

		StreamObserver<HelloRequest> requestStreamObserver =
				this.testHelloWorldStub.helloToEveryOne(newReplyObserver(reply::set, replyLatch));

		String[] names = {"Anna", "Bill", "Tom"};

		for (String name : names) {
			requestStreamObserver.onNext(newHelloRequest(name));
		}
		requestStreamObserver.onCompleted();

		assertThat(replyLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(reply.get()).extracting(HelloReply::getMessage).isEqualTo("Hello " + String.join(", ", names));
	}

	@Test
	void bidiStreaming() throws InterruptedException {
		List<HelloReply> replies = new ArrayList<>();
		CountDownLatch replyLatch = new CountDownLatch(1);

		StreamObserver<HelloRequest> requestStreamObserver =
				this.testHelloWorldStub.bidiStreamHello(newReplyObserver(replies::add, replyLatch));

		String[] names = {"Sofia", "Mark", "Paul", "Martha"};

		for (String name : names) {
			requestStreamObserver.onNext(newHelloRequest(name));
		}
		requestStreamObserver.onCompleted();

		assertThat(replyLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(replies)
				.extracting(HelloReply::getMessage)
				.containsAll(Arrays.stream(names).map("Hello "::concat).toList());
	}

	@Test
	void errorFromServer() {
		assertThatExceptionOfType(StatusRuntimeException.class)
				.isThrownBy(() -> this.testHelloWorldBlockingStub.errorOnHello(newHelloRequest("Error")))
				.satisfies(e -> {
					assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
					assertThat(e.getStatus().getDescription())
							.contains("Failed to transform Message in bean " +
									"'grpcIntegrationFlow.subFlow#4.method-invoking-transformer#1'");
				});
	}

	private static HelloRequest newHelloRequest(String message) {
		return HelloRequest.newBuilder().setName(message).build();
	}

	private static <R> StreamObserver<R> newReplyObserver(Consumer<R> replyConsumer,
			CountDownLatch completionLatch) {

		return new StreamObserver<>() {

			@Override
			public void onNext(R value) {
				replyConsumer.accept(value);
			}

			@Override
			public void onError(Throwable t) {

			}

			@Override
			public void onCompleted() {
				completionLatch.countDown();
			}

		};
	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestInProcessConfiguration.class)
	@EnableIntegration
	static class TestConfig {

		@Bean
		TestHelloWorldGrpc.TestHelloWorldBlockingStub testHelloWorldBlockingStub(ManagedChannel grpcChannel) {
			return TestHelloWorldGrpc.newBlockingStub(grpcChannel);
		}

		@Bean
		TestHelloWorldGrpc.TestHelloWorldFutureStub testHelloWorldFutureStub(ManagedChannel grpcChannel) {
			return TestHelloWorldGrpc.newFutureStub(grpcChannel);
		}

		@Bean
		TestHelloWorldGrpc.TestHelloWorldStub testHelloWorldStub(ManagedChannel grpcChannel) {
			return TestHelloWorldGrpc.newStub(grpcChannel);
		}

		@Bean
		GrpcInboundGateway helloWorldService() {
			return new GrpcInboundGateway(TestHelloWorldGrpc.TestHelloWorldImplBase.class);
		}

		@Bean
		IntegrationFlow grpcIntegrationFlow(GrpcInboundGateway helloWorldService) {
			return IntegrationFlow.from(helloWorldService)
					.route(Message.class, message ->
									message.getHeaders().get(GrpcHeaders.SERVICE_METHOD, String.class),
							router -> router

									.subFlowMapping("SayHello", flow -> flow
											.transform(this::requestReply))

									.subFlowMapping("StreamSayHello", flow -> flow
											.transform(this::streamReply))

									.subFlowMapping("HelloToEveryOne", flow -> flow
											.transformWith(transformSpec -> transformSpec
													.transformer(this::streamRequest)
													.async(true)))

									.subFlowMapping("BidiStreamHello", flow -> flow
											.transform(this::requestReply))

									.subFlowMapping("ErrorOnHello", flow -> flow
											.transform(p -> {
												throw Status.UNAVAILABLE.withDescription("intentional")
														.asRuntimeException();
											}))
					)
					.get();
		}

		private HelloReply requestReply(HelloRequest helloRequest) {
			return newHelloReply("Hello " + helloRequest.getName());
		}

		private Flux<HelloReply> streamReply(HelloRequest helloRequest) {
			return Flux.just(
					newHelloReply("Hello " + helloRequest.getName()),
					newHelloReply("Hello again!"));
		}

		private Mono<HelloReply> streamRequest(Flux<HelloRequest> request) {
			return request
					.map(HelloRequest::getName)
					.collectList()
					.map(names -> StringUtils.collectionToDelimitedString(names, ", "))
					.map("Hello "::concat)
					.map(TestConfig::newHelloReply);
		}

		private static HelloReply newHelloReply(String message) {
			return HelloReply.newBuilder().setMessage(message).build();
		}

	}

}
