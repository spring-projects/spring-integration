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

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.grpc.TestInProcessConfiguration;
import org.springframework.integration.grpc.proto.HelloReply;
import org.springframework.integration.grpc.proto.HelloRequest;
import org.springframework.integration.grpc.proto.TestSingleHelloWorldGrpc;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
public class GrpcClientOutboundGatewaySingleMethodTests {

	@Autowired
	private GrpcOutboundGateway grpcOutboundGateway;

	@SuppressWarnings("unchecked")
	@Test
	void testSingleMethodAsync() {
		HelloRequest request = HelloRequest.newBuilder().setName("Jane").build();
		Message<?> requestMessage = new GenericMessage<>(request);

		Mono<HelloReply> monoResponse = (Mono<HelloReply>) this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		StepVerifier.create(monoResponse
						.map(HelloReply::getMessage))
				.expectNext("Hello, Jane!")
				.verifyComplete();
	}

	@Test
	void testSingleMethodBlocking() {
		this.grpcOutboundGateway.setAsync(false);

		HelloRequest request = HelloRequest.newBuilder().setName("Jane").build();
		Message<?> requestMessage = new GenericMessage<>(request);

		HelloReply response = (HelloReply) this.grpcOutboundGateway.handleRequestMessage(requestMessage);

		assertThat(response.getMessage()).isEqualTo("Hello, Jane!");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	@Import(TestInProcessConfiguration.class)
	static class TestConfig {

		@Bean
		SimpleServiceImpl simpleService() {
			return new SimpleServiceImpl();
		}

		@Bean
		GrpcOutboundGateway grpcOutboundGateway(ManagedChannel channel) {
			return new GrpcOutboundGateway(channel, TestSingleHelloWorldGrpc.class);
		}

		private static class SimpleServiceImpl extends TestSingleHelloWorldGrpc.TestSingleHelloWorldImplBase {

			@Override
			public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
				HelloReply reply = HelloReply.newBuilder()
						.setMessage("Hello, " + request.getName() + "!")
						.build();
				responseObserver.onNext(reply);
				responseObserver.onCompleted();
			}

		}

	}

}
