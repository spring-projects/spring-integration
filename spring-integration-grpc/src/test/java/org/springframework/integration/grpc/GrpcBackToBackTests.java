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

package org.springframework.integration.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.grpc.dsl.Grpc;
import org.springframework.integration.grpc.inbound.GrpcInboundGateway;
import org.springframework.integration.grpc.outbound.GrpcOutboundGateway;
import org.springframework.integration.grpc.proto.HelloReply;
import org.springframework.integration.grpc.proto.HelloRequest;
import org.springframework.integration.grpc.proto.TestSingleHelloWorldGrpc;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * Verify gRPC exchange via {@link GrpcInboundGateway} and {@link GrpcOutboundGateway}.
 *
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 7.1
 */
@SpringJUnitConfig
@DirtiesContext
public class GrpcBackToBackTests {

	@Test
	void sendAndReceiveViaGrpcGateways(@Autowired @Qualifier("grpcOutboundFlow.input") MessageChannel inputChannel) {
		assertThat(requestReply("Jack", inputChannel))
				.extracting(Message::getPayload)
				.asInstanceOf(type(HelloReply.class))
				.extracting(HelloReply::getMessage)
				.isEqualTo("Hello Jack");
	}

	@Test
	void sendAndReceiveViaGrpcGatewaysWithCustomizations(
			@Autowired @Qualifier("grpcOutboundFlowWithCustomizations.input")
			MessageChannel inputChannel) {
		assertThat(requestReply("Jill", inputChannel))
				.extracting(Message::getPayload)
				.asInstanceOf(type(HelloReply.class))
				.extracting(HelloReply::getMessage)
				.isEqualTo("Hello Jill");
	}

	@Test
	void sendAndReceiveViaGrpcGatewaysWithMethodName(
			@Autowired @Qualifier("grpcOutboundFlowWithMethodName.input")
			MessageChannel inputChannel) {
		assertThat(requestReply("John", inputChannel))
				.extracting(Message::getPayload)
				.asInstanceOf(type(HelloReply.class))
				.extracting(HelloReply::getMessage)
				.isEqualTo("Hello John");
	}

	@Test
	void sendAndReceiveViaGrpcGatewaysWithFunction(
			@Autowired @Qualifier("grpcOutboundFlowWithFunction.input")
			MessageChannel inputChannel) {
		assertThat(requestReply("Jane", inputChannel))
				.extracting(Message::getPayload)
				.asInstanceOf(type(HelloReply.class))
				.extracting(HelloReply::getMessage)
				.isEqualTo("Hello Jane");
	}

	private Message<?> requestReply(String payload, MessageChannel inputChannel) {
		HelloRequest request = HelloRequest.newBuilder().setName(payload).build();
		QueueChannel replyChannel = new QueueChannel();
		Message<?> requestMessage = MessageBuilder.withPayload(request)
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(requestMessage);

		return replyChannel.receive(10_000);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestInProcessConfiguration.class)
	@EnableIntegration
	static class TestConfig {

		@Bean
		IntegrationFlow grpcOutboundFlow(ManagedChannel channel) {
			return f -> f
					.handle(Grpc.outboundGateway(channel, TestSingleHelloWorldGrpc.class));
		}

		@Bean
		IntegrationFlow grpcOutboundFlowWithCustomizations(ManagedChannel channel) {
			SpelExpressionParser parser = new SpelExpressionParser();
			CallOptions customCallOptions = CallOptions.DEFAULT
					.withDeadlineAfter(10, TimeUnit.SECONDS);

			return f -> f.handle(Grpc.outboundGateway(channel, TestSingleHelloWorldGrpc.class)
					.methodNameExpression(parser.parseExpression("'SayHello'"))
					.callOptions(customCallOptions));
		}

		@Bean
		IntegrationFlow grpcOutboundFlowWithMethodName(ManagedChannel channel) {
			return f -> f.handle(Grpc.outboundGateway(channel, TestSingleHelloWorldGrpc.class)
					.methodName("SayHello"));
		}

		@Bean
		IntegrationFlow grpcOutboundFlowWithFunction(ManagedChannel channel) {
			return f -> f.handle(Grpc.outboundGateway(channel, TestSingleHelloWorldGrpc.class)
					.methodNameFunction(message -> {
						String method = message.getHeaders().get("grpcMethod", String.class);
						return method != null ? method : "SayHello";
					}));
		}

		@Bean
		IntegrationFlow grpcInboundFlow() {
			return IntegrationFlow.from(
							Grpc.inboundGateway(TestSingleHelloWorldGrpc.TestSingleHelloWorldImplBase.class))
					.transform(this::requestReply)
					.get();
		}

		private HelloReply requestReply(HelloRequest helloRequest) {
			return HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build();
		}

	}

}
