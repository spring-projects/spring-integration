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

import io.grpc.BindableService;
import io.grpc.Channel;

/**
 * Factory class for gRPC components.
 * <p>
 * Provides static factory methods for creating gRPC component specifications for Spring Integration DSL flows.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public final class Grpc {

	/**
	 * Create a {@link GrpcOutboundGatewaySpec} for an outbound gateway.
	 * @param channel the gRPC channel to use for communication
	 * @param grpcServiceClass the gRPC service class
	 * @return the {@link GrpcOutboundGatewaySpec}
	 */
	public static GrpcOutboundGatewaySpec outboundGateway(Channel channel, Class<?> grpcServiceClass) {
		return new GrpcOutboundGatewaySpec(channel, grpcServiceClass);
	}

	/**
	 * Create a {@link GrpcInboundGatewaySpec} for an inbound gateway.
	 * @param grpcServiceClass the gRPC service class
	 * @return the {@link GrpcInboundGatewaySpec}
	 */
	public static GrpcInboundGatewaySpec inboundGateway(Class<? extends BindableService> grpcServiceClass) {
		return new GrpcInboundGatewaySpec(grpcServiceClass);
	}

	private Grpc() {
	}

}
