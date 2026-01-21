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

import io.grpc.CallOptions;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.grpc.GrpcHeaders;
import org.springframework.integration.grpc.outbound.GrpcOutboundGateway;

/**
 * A {@link MessageHandlerSpec} for a {@link GrpcOutboundGateway}.
 * <p>
 * This spec provides a fluent API for configuring gRPC outbound gateways in Spring Integration DSL flows.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class GrpcOutboundGatewaySpec extends MessageHandlerSpec<GrpcOutboundGatewaySpec, GrpcOutboundGateway> {

	protected GrpcOutboundGatewaySpec(GrpcOutboundGateway gateway) {
		this.target = gateway;
	}

	/**
	 * Set the name of the gRPC method to call.
	 * If method name is not provided, the default expression checks the
	 * {@link org.springframework.messaging.MessageHeaders} for {@link GrpcHeaders#SERVICE_METHOD} or, in case a single
	 * service method, the name of that method is used.
	 * @param methodName the name of the gRPC method to call
	 * @return the spec
	 * @see GrpcOutboundGateway#setMethodName(String)
	 */
	public GrpcOutboundGatewaySpec methodName(String methodName) {
		this.target.setMethodName(methodName);
		return this;
	}

	/**
	 * Set the {@link org.springframework.expression.spel.standard.SpelExpression} to resolve the gRPC method name at
	 * runtime. If not provided, the default expression checks the {@link org.springframework.messaging.MessageHeaders}
	 * for a {@link GrpcHeaders#SERVICE_METHOD}. If the expression is not set and the service has only one method,
	 * then the gateway will set the expression to use the name of that method.
	 * @param methodNameSpelExpression the expression string
	 * @return the spec
	 * @see GrpcOutboundGateway#setMethodNameExpression(Expression)
	 */
	public GrpcOutboundGatewaySpec methodNameExpression(String methodNameSpelExpression) {
		this.target.setMethodNameExpression(PARSER.parseExpression(methodNameSpelExpression));
		return this;
	}

	/**
	 * Set an {@link Expression} to resolve the gRPC method name at runtime.
	 * <p>
	 * The expression is evaluated against the request message.
	 * @param methodNameExpression the expression to resolve the method name
	 * @return the spec
	 * @see GrpcOutboundGateway#setMethodNameExpression(Expression)
	 */
	public GrpcOutboundGatewaySpec methodNameExpression(Expression methodNameExpression) {
		this.target.setMethodNameExpression(methodNameExpression);
		return this;
	}

	/**
	 * Set the {@link CallOptions} for the gRPC call.
	 * <p>
	 * This allows configuration of deadlines, credentials, compression, etc.
	 * Default is {@link CallOptions#DEFAULT}.
	 * @param callOptions the call options for the gateway
	 * @return the spec
	 * @see GrpcOutboundGateway#setCallOptions(CallOptions)
	 */
	public GrpcOutboundGatewaySpec callOptions(CallOptions callOptions) {
		this.target.setCallOptions(callOptions);
		return this;
	}

}
