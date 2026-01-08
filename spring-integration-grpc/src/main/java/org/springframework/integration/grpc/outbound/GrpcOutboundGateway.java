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

package org.springframework.integration.grpc.outbound;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Outbound Gateway for gRPC client invocations.
 * <p>
 * This component acts as a bridge between Spring Integration messaging and gRPC service calls,
 * supporting multiple invocation patterns based on the gRPC method type.
 * <p>
 * Supported gRPC patterns:
 * <ul>
 *   <li>Unary: single request → single response (blocking call returns the message directly)</li>
 *   <li>Server streaming: single request → {@link Flux} with multiple responses</li>
 *   <li>Client streaming: {@link Flux} of requests → {@link reactor.core.publisher.Mono Mono} with single response</li>
 *   <li>Bidirectional streaming: {@link Flux} of requests → {@link Flux} with multiple responses</li>
 * </ul>
 * @author Glenn Renfro
 * @since 7.1
 */
public class GrpcOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final Channel channel;

	private final Class<?> grpcServiceClass;

	private final ServiceDescriptor serviceDescriptor;

	@SuppressWarnings("NullAway.Init")
	private @Nullable MethodDescriptor<?, ?> methodDescriptor;

	@SuppressWarnings("NullAway.Init")
	private MethodDescriptor.MethodType methodType;

	private @Nullable Expression methodNameExpression;

	/**
	 * Create a new {@code GrpcOutboundGateway} with the specified configuration.
	 * @param channel the gRPC channel to use for communication
	 * @param grpcServiceClass the gRPC service class (e.g., {@code SimpleGrpc.class})
	 */
	public GrpcOutboundGateway(Channel channel, Class<?> grpcServiceClass) {
		this.channel = channel;
		this.grpcServiceClass = grpcServiceClass;
		Method getServiceDescriptor = ClassUtils.getMethod(grpcServiceClass, "getServiceDescriptor");
		this.serviceDescriptor = (ServiceDescriptor) Objects.requireNonNull(ReflectionUtils.invokeMethod(getServiceDescriptor, null));
	}

	/**
	 * Handle the request message by invoking the configured gRPC method.
	 * @param requestMessage the message containing the gRPC request payload
	 * @return the gRPC response: direct message object for Unary, {@code Mono} for Client streaming,
	 * or {@code Flux} for Server streaming and Bidi Streaming
	 * @throws UnsupportedOperationException if the method type is not recognized
	 * @throws IllegalArgumentException if Client streaming is invoked without a {@code Flux} payload
	 */
	@SuppressWarnings("NullAway")
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Object request = requestMessage.getPayload();

		this.logger.debug("Invoking gRPC method '" + this.methodDescriptor.getBareMethodName() +
				"' with payload: " + request);

		@SuppressWarnings("unchecked")
		Object grpcResponse = switch (this.methodType) {
			case BIDI_STREAMING, SERVER_STREAMING -> invokeAsyncStubMethod(request);
			case CLIENT_STREAMING -> invokeClientStreaming(request);
			case UNARY -> invokeBlocking(
					(MethodDescriptor<Object, Object>) this.methodDescriptor, request);
			default -> throw new UnsupportedOperationException("Unexpected method type: " + this.methodType);
		};

		return grpcResponse;
	}

	@Override
	protected void doInit() {
		super.doInit();
		String methodName = null;
		if (this.methodNameExpression != null) {
			Object methodNameObject = this.methodNameExpression.getValue();
			if (methodNameObject != null) {
				methodName = methodNameObject.toString();
				Assert.state(!methodName.isEmpty(), "MethodNameExpression must not resolve to an empty string");
			}
			else {
				throw new IllegalArgumentException("MethodNameExpression must not resolve to null");
			}
		}
		else {
			Collection<MethodDescriptor<?, ?>> methods = this.serviceDescriptor.getMethods();
			if (methods.size() == 1) {
				Iterator<MethodDescriptor<?, ?>> methodIterator = methods.iterator();
				methodName = Objects.requireNonNull(methodIterator.next().getBareMethodName());
			}
		}
		Assert.state(StringUtils.hasText(methodName), "Method name must not be null or empty");

		this.methodDescriptor = getMethodDescriptor(methodName, this.grpcServiceClass);
		this.methodType = this.methodDescriptor.getType();
	}

	/**
	 * Set an {@link Expression} to resolve the gRPC method name at runtime.
	 * If not provided, the gateway will auto-detect the method when the service
	 * descriptor contains exactly one method.
	 * @param methodNameExpression the expression to resolve the method name
	 */
	public void setMethodNameExpression(Expression methodNameExpression) {
		this.methodNameExpression = methodNameExpression;
	}

	@SuppressWarnings("NullAway")
	private Object invokeClientStreaming(Object request) {
		Sinks.One<Object> responseSink = Sinks.one();

		StreamObserver<Object> responseObserver = new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				Sinks.EmitResult result = responseSink.tryEmitValue(value);
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
			}
		};

		if (request instanceof Flux<?> requestFlux) {
			StreamObserver<Object> requestObserver =
					invokeBiDirectional(this.methodDescriptor, responseObserver);

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
			throw new IllegalArgumentException(
					"Client streaming requires Flux payload, but got: " + request.getClass().getName());
		}

		return responseSink.asMono();
	}

	@SuppressWarnings("NullAway")
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
					invokeBiDirectional(this.methodDescriptor, responseObserver);

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
			invoke(this.methodDescriptor, request, responseObserver);
		}

		return responseSink.asFlux();
	}

	private MethodDescriptor<?, ?> getMethodDescriptor(String methodName, Class<?> grpcServiceClass) {

		for (MethodDescriptor<?, ?> method : this.serviceDescriptor.getMethods()) {
			if (methodName.equalsIgnoreCase(method.getBareMethodName())) {
				return method;
			}
		}

		throw new IllegalArgumentException("Method '" + methodName +
				"' not found in service class: " + grpcServiceClass.getName());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <ReqT, RespT> void invoke(MethodDescriptor method, ReqT request,
			StreamObserver<RespT> responseObserver) {

		ClientCall<ReqT, RespT> call = this.channel.newCall(method, CallOptions.DEFAULT);

		switch (method.getType()) {
			case SERVER_STREAMING -> ClientCalls.asyncServerStreamingCall(call, request, responseObserver);
			case CLIENT_STREAMING, BIDI_STREAMING ->
					throw new UnsupportedOperationException("Use invokeBiDirectional() for streaming methods");
			default -> throw new IllegalStateException("Unknown method type: " + method.getType());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <ReqT, RespT> StreamObserver<ReqT> invokeBiDirectional(MethodDescriptor method,
			StreamObserver<RespT> responseObserver) {

		ClientCall<ReqT, RespT> call = this.channel.newCall(method, CallOptions.DEFAULT);

		return switch (method.getType()) {
			case CLIENT_STREAMING -> ClientCalls.asyncClientStreamingCall(call, responseObserver);
			case BIDI_STREAMING -> ClientCalls.asyncBidiStreamingCall(call, responseObserver);
			case UNARY, SERVER_STREAMING ->
					throw new UnsupportedOperationException("Use invoke() for unary/server-streaming methods");
			default -> throw new IllegalStateException("Unknown method type: " + method.getType());
		};
	}

	private <ReqT, RespT> RespT invokeBlocking(MethodDescriptor<ReqT, RespT> method, ReqT request) {
		if (method.getType() != MethodDescriptor.MethodType.UNARY) {
			throw new IllegalArgumentException("Blocking invocation only supports UNARY methods, " +
					"but method type is: " + method.getType());
		}
		return ClientCalls.blockingUnaryCall(this.channel, method, CallOptions.DEFAULT, request);
	}

}

