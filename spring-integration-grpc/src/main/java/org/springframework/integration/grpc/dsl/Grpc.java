/*
 * Copyright 2026-present the original author or authors.
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
