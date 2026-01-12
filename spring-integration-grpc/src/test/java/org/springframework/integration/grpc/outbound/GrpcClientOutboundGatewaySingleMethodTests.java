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
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.grpc.proto.HelloReply;
import org.springframework.integration.grpc.proto.HelloRequest;
import org.springframework.integration.grpc.proto.TestSingleHelloWorldGrpc;
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
@DirtiesContext
public class GrpcClientOutboundGatewaySingleMethodTests {

	private static final String serverName = InProcessServerBuilder.generateName();

	@Autowired
	private ManagedChannel channel;

	@Autowired
	private GrpcOutboundGateway grpcOutboundGateway;

	// ==================== SimpleBlockingStub Tests ====================

	@Test
	void testSingleMethodProto() {
		validateBlockingStub(this.grpcOutboundGateway, "SayHello");
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

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		Server server() throws Exception {
			return InProcessServerBuilder
					.forName(serverName)
					.directExecutor()
					.addService(new GrpcClientOutboundGatewaySingleMethodTests.SimpleServiceImpl())
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
			return new GrpcOutboundGateway(channel, TestSingleHelloWorldGrpc.class);
		}
	}
}
