/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.grpc.dsl;

import io.grpc.BindableService;

import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.grpc.inbound.GrpcInboundGateway;

/**
 * A {@link MessagingGatewaySpec} for a {@link GrpcInboundGateway}.
 * <p>
 * This spec provides a fluent API for configuring gRPC inbound gateways in Spring Integration DSL flows.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class GrpcInboundGatewaySpec extends MessagingGatewaySpec<GrpcInboundGatewaySpec, GrpcInboundGateway> {

	protected GrpcInboundGatewaySpec(Class<? extends BindableService> grpcServiceClass) {
		super(new GrpcInboundGateway(grpcServiceClass));
	}

}
