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

package org.springframework.integration.grpc.dsl;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.grpc.outbound.GrpcOutboundGateway;
import org.springframework.integration.grpc.proto.TestHelloWorldGrpc;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class GrpcOutboundGatewaySpecTests {

	private static final String TEST_SERVER_NAME = "test-server";

	ManagedChannel channel;

	@BeforeEach
	public void setup() {
		this.channel = createTestChannel();
	}

	@AfterEach
	void teardown() {
		if (this.channel != null) {
			this.channel.shutdown();
		}
	}

	@Test
	void testMethodNameConfiguration() {
		GrpcOutboundGatewaySpec spec = Grpc.outboundGateway(this.channel, TestHelloWorldGrpc.class)
				.methodName("SayHello");

		GrpcOutboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getMethodName(gateway)).isEqualTo("SayHello");
	}

	@Test
	void testMethodNameExpressionConfiguration() {
		SpelExpressionParser parser = new SpelExpressionParser();
		GrpcOutboundGatewaySpec spec = Grpc.outboundGateway(this.channel, TestHelloWorldGrpc.class)
				.methodNameExpression(parser.parseExpression("'SayHello'"));

		GrpcOutboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getMethodName(gateway)).isEqualTo("SayHello");
	}

	@Test
	void testFactoryMethodWithMethodName() {
		GrpcOutboundGatewaySpec spec = Grpc.outboundGateway(this.channel, TestHelloWorldGrpc.class, "SayHello");

		GrpcOutboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getMethodName(gateway)).isEqualTo("SayHello");
	}

	@Test
	void testFluentApiChaining() {
		CallOptions customCallOptions = CallOptions.DEFAULT
				.withDeadlineAfter(10, TimeUnit.SECONDS);

		GrpcOutboundGatewaySpec spec = Grpc.outboundGateway(this.channel, TestHelloWorldGrpc.class)
				.methodName("SayHello")
				.callOptions(customCallOptions);

		GrpcOutboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getMethodName(gateway)).isEqualTo("SayHello");
		CallOptions gatewayCallOptions = getCallOptions(gateway);
		assertThat(gatewayCallOptions).isNotNull();
		assertThat(gatewayCallOptions.getDeadline().timeRemaining(TimeUnit.SECONDS)).isGreaterThan(0);
	}

	@Test
	void testSpecReturnsCorrectType() {
		GrpcOutboundGatewaySpec spec = Grpc.outboundGateway(this.channel, TestHelloWorldGrpc.class);
		assertThat(spec).isInstanceOf(GrpcOutboundGatewaySpec.class);
		assertThat(spec.getObject()).isInstanceOf(GrpcOutboundGateway.class);
	}

	@Test
	void testMethodNameOverride() {
		GrpcOutboundGatewaySpec spec = Grpc.outboundGateway(this.channel, TestHelloWorldGrpc.class)
				.methodName("FirstMethod")
				.methodName("SecondMethod");

		GrpcOutboundGateway gateway = spec.getObject();
		assertThat(getMethodName(gateway)).isEqualTo("SecondMethod");
		assertThat(gateway).isNotNull();
	}

	private String getMethodName(GrpcOutboundGateway gateway) {
		Field methodNameExpressionField = ReflectionUtils.findField(GrpcOutboundGateway.class, "methodNameExpression");
		methodNameExpressionField.setAccessible(true);
		Expression expression = (Expression) ReflectionUtils.getField(methodNameExpressionField, gateway);
		return expression.getValue(String.class);
	}

	private CallOptions getCallOptions(GrpcOutboundGateway gateway) {
		Field callOptionsField = ReflectionUtils.findField(GrpcOutboundGateway.class, "callOptions");
		callOptionsField.setAccessible(true);
		return (CallOptions) ReflectionUtils.getField(callOptionsField, gateway);
	}

	private ManagedChannel createTestChannel() {
		return InProcessChannelBuilder.forName(TEST_SERVER_NAME)
				.usePlaintext()
				.build();
	}

}
