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
