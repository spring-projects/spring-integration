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

import org.junit.jupiter.api.Test;

import org.springframework.integration.grpc.inbound.GrpcInboundGateway;
import org.springframework.integration.grpc.proto.TestHelloWorldGrpc;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class GrpcInboundGatewaySpecTests {

	@Test
	void testBasicConfiguration() {
		GrpcInboundGatewaySpec spec = Grpc.inboundGateway(TestHelloWorldGrpc.TestHelloWorldImplBase.class);

		GrpcInboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getGrpcServiceClass(gateway)).isEqualTo(TestHelloWorldGrpc.TestHelloWorldImplBase.class);
	}

	@Test
	void testRequestTimeoutConfiguration() {
		GrpcInboundGatewaySpec spec = Grpc.inboundGateway(TestHelloWorldGrpc.TestHelloWorldImplBase.class)
				.requestTimeout(5000L);

		GrpcInboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getRequestTimeout(gateway)).isEqualTo(5000L);
	}

	@Test
	void testReplyTimeoutConfiguration() {
		GrpcInboundGatewaySpec spec = Grpc.inboundGateway(TestHelloWorldGrpc.TestHelloWorldImplBase.class)
				.replyTimeout(3000L);

		GrpcInboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(getReplyTimeout(gateway)).isEqualTo(3000L);
	}

	@Test
	void testFluentApiChaining() {
		GrpcInboundGatewaySpec spec = Grpc.inboundGateway(TestHelloWorldGrpc.TestHelloWorldImplBase.class)
				.requestTimeout(5000L)
				.replyTimeout(3000L)
				.autoStartup(false);

		GrpcInboundGateway gateway = spec.getObject();
		assertThat(gateway).isNotNull();
		assertThat(gateway.isAutoStartup()).isFalse();
		assertThat(getRequestTimeout(gateway)).isEqualTo(5000L);
		assertThat(getReplyTimeout(gateway)).isEqualTo(3000L);
	}

	private Class<?> getGrpcServiceClass(GrpcInboundGateway gateway) {
		Field grpcServiceClassField = ReflectionUtils.findField(GrpcInboundGateway.class, "grpcServiceClass");
		grpcServiceClassField.setAccessible(true);
		return (Class<?>) ReflectionUtils.getField(grpcServiceClassField, gateway);
	}

	private long getRequestTimeout(GrpcInboundGateway gateway) {
		return getMessagingTemplate(gateway).getSendTimeout();
	}

	private long getReplyTimeout(GrpcInboundGateway gateway) {
		return getMessagingTemplate(gateway).getReceiveTimeout();
	}

	private GenericMessagingTemplate getMessagingTemplate(GrpcInboundGateway gateway) {
		Field messagingTemplateField = ReflectionUtils.findField(GrpcInboundGateway.class, "messagingTemplate");
		messagingTemplateField.setAccessible(true);
		return (GenericMessagingTemplate) ReflectionUtils.getField(messagingTemplateField, gateway);
	}

}
