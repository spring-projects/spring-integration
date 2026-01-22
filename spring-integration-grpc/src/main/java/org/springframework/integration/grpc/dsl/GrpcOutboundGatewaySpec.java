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

import java.util.function.Function;

import io.grpc.CallOptions;
import io.grpc.Channel;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.grpc.outbound.GrpcOutboundGateway;
import org.springframework.messaging.Message;

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

	protected GrpcOutboundGatewaySpec(Channel channel, Class<?> grpcServiceClass) {
		this.target = new GrpcOutboundGateway(channel, grpcServiceClass);
	}

	/**
	 * Set the name of the gRPC method to call.
	 * If method name is not provided, the default expression checks the
	 * {@link org.springframework.messaging.MessageHeaders} for
	 * {@link org.springframework.integration.grpc.GrpcHeaders#SERVICE_METHOD} or, in case a single service method,
	 * the name of that method is used.
	 * @param methodName the name of the gRPC method to call
	 * @return the spec
	 * @see GrpcOutboundGateway#setMethodName(String)
	 */
	public GrpcOutboundGatewaySpec methodName(String methodName) {
		this.target.setMethodName(methodName);
		return this;
	}

	/**
	 * Set the Spel expression for resolving the method name.
	 * @param methodNameSpelExpression the expression string
	 * @return the spec
	 * @see GrpcOutboundGateway#setMethodNameExpression(Expression)
	 */
	public GrpcOutboundGatewaySpec methodNameExpression(String methodNameSpelExpression) {
		return methodNameExpression(PARSER.parseExpression(methodNameSpelExpression));
	}

	/**
	 * Set the FunctionExpression for resolving the method name.
	 * @param methodNameFunction the function for a {@link FunctionExpression}
	 * @return the spec
	 * @see GrpcOutboundGateway#setMethodNameExpression(Expression)
	 */
	public GrpcOutboundGatewaySpec methodNameFunction(Function<Message<?>, String> methodNameFunction) {
		return methodNameExpression(new FunctionExpression<>(methodNameFunction));
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
