/*
 * Copyright 2025-present the original author or authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Spring Integration Outbound Gateway for gRPC client invocations.
 * <p>This component acts as a bridge between Spring Integration messaging and gRPC service calls,
 * supporting multiple invocation patterns based on the gRPC method type.
 * <p>Supported gRPC patterns:
 * <ul>
 *   <li>Unary: single request → single response (blocking or reactive)</li>
 *   <li>Server streaming: single request → Flux with multiple responses</li>
 *   <li>Client streaming: Flux request → single response</li>
 *   <li>Bidirectional streaming: Flux request → Flux with multiple responses</li>
 * </ul>
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class GrpcOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final GrpcDynamicInvoker invoker;

	private final MethodDescriptor<?, ?> methodDescriptor;

	private MethodDescriptor.MethodType methodType;

	private long asyncTimeoutSeconds = 10;

	/**
	 * Create a new {@code GrpcOutboundGateway} with the specified configuration.
	 * @param channel the gRPC channel to use for communication
	 * @param grpcServiceClass the gRPC service class (e.g., {@code SimpleGrpc.class})
	 * @param methodNameExpression the name of the gRPC method to invoke (case-insensitive)
	 */
	public GrpcOutboundGateway(Channel channel, Class<?> grpcServiceClass, Expression methodNameExpression) {
		this.methodDescriptor = getMethodDescriptor(methodNameExpression, grpcServiceClass);
		this.methodType = this.methodDescriptor.getType();
		this.invoker = new GrpcDynamicInvoker(channel);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Object request = requestMessage.getPayload();

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Invoking gRPC method '" + this.methodDescriptor.getBareMethodName() +
					"' with payload: " + request);
		}

		@SuppressWarnings("unchecked")
		Object grpcResponse = switch (this.methodType) {
			case BIDI_STREAMING, SERVER_STREAMING,
					CLIENT_STREAMING -> invokeAsyncStubMethod(request);
			case UNARY -> this.invoker.invokeBlocking(
					(MethodDescriptor<Object, Object>) this.methodDescriptor, request);
			default -> throw new UnsupportedOperationException("Unexpected method type: " + this.methodType);
		};

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Received gRPC response: " + grpcResponse);
		}

		return grpcResponse;
	}

	private Object invokeAsyncStubMethod(Object request) {
		Sinks.Many<Object> responseSink = Sinks.many().unicast().onBackpressureBuffer();

		StreamObserver<Object> responseObserver = new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				Sinks.EmitResult result = responseSink.tryEmitNext(value);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn("Failed to emit value to sink: " + result);
				}
			}

			@Override
			public void onError(Throwable ex) {
				Sinks.EmitResult result = responseSink.tryEmitError(ex);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.error(ex, "Failed to emit error to sink: " + result);
				}
			}

			@Override
			public void onCompleted() {
				Sinks.EmitResult result = responseSink.tryEmitComplete();
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn("Failed to emit completion to sink: " + result);
				}
			}
		};

		if (request instanceof Flux<?> requestFlux) {
			// Client or Bidirectional streaming
			StreamObserver<Object> requestObserver =
					this.invoker.invokeBiDirectional(this.methodDescriptor, responseObserver);

			@SuppressWarnings("unchecked")
			Flux<Object> typedRequestFlux = (Flux<Object>) requestFlux;

			typedRequestFlux.subscribe(
					requestObserver::onNext,
					ex -> {
						requestObserver.onError(ex);
						responseObserver.onError(ex);
					},
					requestObserver::onCompleted
			);
		}
		else {
			// Unary or Server-streaming
			this.invoker.invoke(this.methodDescriptor, request, responseObserver);
		}

		return responseSink.asFlux().timeout(java.time.Duration.ofSeconds(this.asyncTimeoutSeconds));
	}

	/**
	 * Set the timeout for async gRPC calls.
	 * <p>Defaults to 10 seconds.
	 * @param timeout the timeout value
	 * @param unit the time unit of the timeout
	 */
	public void setAsyncTimeout(long timeout, TimeUnit unit) {
		this.asyncTimeoutSeconds = unit.toSeconds(timeout);
	}

	private MethodDescriptor<?, ?> getMethodDescriptor(Expression methodNameExpression, Class<?> grpcServiceClass) {
		try {
			Method getServiceDescriptor = grpcServiceClass.getMethod("getServiceDescriptor");
			ServiceDescriptor serviceDescriptor = (ServiceDescriptor) getServiceDescriptor.invoke(null);
			Object methodNameObject = methodNameExpression.getValue();
			String methodName;
			if (methodNameObject != null) {
				methodName = methodNameObject.toString();
				Assert.state(!methodName.isEmpty(), "methodNameExpression cannot resolve to an empty string");
			}
			else {
				throw new IllegalArgumentException("methodNameExpression cannot resolve to a null");
			}
			for (MethodDescriptor<?, ?> method : serviceDescriptor.getMethods()) {
				if (methodName.equalsIgnoreCase(method.getBareMethodName())) {
					return method;
				}
			}

			throw new IllegalArgumentException("Method '" + methodNameExpression.getValue() +
					"' not found in service class: " + grpcServiceClass.getName());
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Failed to retrieve service descriptor from " +
					grpcServiceClass.getName(), ex);
		}
	}

	private static class GrpcDynamicInvoker {

		private final Channel channel;

		private final CallOptions callOptions;

		GrpcDynamicInvoker(Channel channel) {
			this(channel, CallOptions.DEFAULT);
		}

		GrpcDynamicInvoker(Channel channel, CallOptions callOptions) {
			this.channel = channel;
			this.callOptions = callOptions;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		<ReqT, RespT> void invoke(MethodDescriptor method, ReqT request,
				StreamObserver<RespT> responseObserver) {

			ClientCall<ReqT, RespT> call = this.channel.newCall(method, this.callOptions);

			switch (method.getType()) {
				case UNARY -> ClientCalls.asyncUnaryCall(call, request, responseObserver);
				case SERVER_STREAMING -> ClientCalls.asyncServerStreamingCall(call, request, responseObserver);
				case CLIENT_STREAMING, BIDI_STREAMING ->
						throw new UnsupportedOperationException("Use invokeBiDirectional() for streaming methods");
				default -> throw new IllegalStateException("Unknown method type: " + method.getType());
			}
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		<ReqT, RespT> StreamObserver<ReqT> invokeBiDirectional(MethodDescriptor method,
				StreamObserver<RespT> responseObserver) {

			ClientCall<ReqT, RespT> call = this.channel.newCall(method, this.callOptions);

			return switch (method.getType()) {
				case CLIENT_STREAMING -> ClientCalls.asyncClientStreamingCall(call, responseObserver);
				case BIDI_STREAMING -> ClientCalls.asyncBidiStreamingCall(call, responseObserver);
				case UNARY, SERVER_STREAMING ->
						throw new UnsupportedOperationException("Use invoke() for unary/server-streaming methods");
				default -> throw new IllegalStateException("Unknown method type: " + method.getType());
			};
		}

		<ReqT, RespT> RespT invokeBlocking(MethodDescriptor<ReqT, RespT> method, ReqT request) {
			if (method.getType() != MethodDescriptor.MethodType.UNARY) {
				throw new IllegalArgumentException("Blocking invocation only supports UNARY methods, " +
						"but method type is: " + method.getType());
			}
			return ClientCalls.blockingUnaryCall(this.channel, method, this.callOptions, request);
		}

	}
}

