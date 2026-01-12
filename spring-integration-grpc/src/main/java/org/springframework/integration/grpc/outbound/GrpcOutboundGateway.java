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
import java.util.stream.Stream;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.grpc.GrpcHeaders;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

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
 *
 * @since 7.1
 */
public class GrpcOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final FunctionExpression<Message<?>> defaultFunctionExpression = new FunctionExpression<>(
		msg -> Objects.requireNonNull(msg.getHeaders().get(GrpcHeaders.SERVICE_METHOD)).toString());

	private final Channel channel;

	private final Class<?> grpcServiceClass;

	private final ServiceDescriptor serviceDescriptor;

	private Expression methodNameExpression = this.defaultFunctionExpression;

	/**
	 * Create a new {@code GrpcOutboundGateway} with the specified configuration.
	 * @param channel the gRPC channel to use for communication
	 * @param grpcServiceClass the gRPC service class (e.g., {@code SimpleGrpc.class})
	 */
	public GrpcOutboundGateway(Channel channel, Class<?> grpcServiceClass) {
		this.channel = channel;
		this.grpcServiceClass = grpcServiceClass;
		Method getServiceDescriptor = ClassUtils.getMethod(this.grpcServiceClass, "getServiceDescriptor");
		this.serviceDescriptor = (ServiceDescriptor) Objects.requireNonNull(ReflectionUtils.invokeMethod(getServiceDescriptor, null));
	}

	@Override
	protected void doInit() {
		super.doInit();
		if (this.methodNameExpression.equals(this.defaultFunctionExpression)) {
			Collection<MethodDescriptor<?, ?>> methods = this.serviceDescriptor.getMethods();
			if (methods.size() == 1) {
				Iterator<MethodDescriptor<?, ?>> methodIterator = methods.iterator();
				String methodName = Objects.requireNonNull(methodIterator.next().getBareMethodName());
				this.methodNameExpression = new LiteralExpression(methodName);
			}
		}
	}

	/**
	 * Set an {@link Expression} to resolve the gRPC method name at runtime.
	 * <p>
	 * If not provided, the gateway will auto-detect the method when the service
	 * descriptor contains exactly one method.
	 * @param methodNameExpression the expression to resolve the method name
	 */
	public void setMethodNameExpression(Expression methodNameExpression) {
		this.methodNameExpression = methodNameExpression;
	}

	/**
	 * Set the method name of the gRPC method.
	 * <p>
	 * If not provided or the method name expression, the gateway will auto-detect the method when the service
	 * descriptor contains exactly one method.
	 * @param methodName the method name of the gRPC method
	 */
	public void setMethodName(String methodName) {
		this.methodNameExpression = new LiteralExpression(methodName);
	}

	/**
	 * Handle the request message by invoking the configured gRPC method.
	 * <p>
	 * This method performs the following steps:
	 * <ol>
	 *   <li>Evaluates the {@code methodNameExpression} to determine which gRPC method to invoke.
	 *       The method name can be resolved from the message headers (via {@link org.springframework.integration.grpc.GrpcHeaders#SERVICE_METHOD}),
	 *       a literal expression, or a custom expression.</li>
	 *   <li>Retrieves the {@link MethodDescriptor} and determines the method type (Unary, Server Streaming,
	 *       Client Streaming, or Bidirectional Streaming).</li>
	 *   <li>Invokes the appropriate gRPC call based on the method type.</li>
	 * </ol>
	 * <p>
	 * <b>Request Payload Handling:</b>
	 * <ul>
	 *   <li><b>Unary:</b> Accepts a single request object</li>
	 *   <li><b>Server Streaming:</b> Accepts a single request object</li>
	 *   <li><b>Client Streaming:</b> Accepts {@link Flux}, {@link Mono}, {@link java.util.stream.Stream Stream},
	 *       {@link java.util.Collection Collection}, or a single object</li>
	 *   <li><b>Bidirectional Streaming:</b> Accepts {@link Flux}, {@link Mono}, {@link java.util.stream.Stream Stream},
	 *       {@link java.util.Collection Collection}, or a single object</li>
	 * </ul>
	 * <p>
	 * <b>Return Type by Method Type:</b>
	 * <ul>
	 *   <li><b>Unary:</b> Returns the response message object directly (blocking call)</li>
	 *   <li><b>Server Streaming:</b> Returns a {@link Flux} of response messages</li>
	 *   <li><b>Client Streaming:</b> Returns a {@link Mono} with the single response</li>
	 *   <li><b>Bidirectional Streaming:</b> Returns a {@link Flux} of response messages</li>
	 * </ul>
	 * @param requestMessage the message containing the gRPC request payload. The payload type depends on the
	 * gRPC method type (see above for details).
	 * @return the gRPC response: direct message object for Unary, {@link Mono} for Client streaming,
	 * or {@link Flux} for Server streaming and Bidirectional streaming
	 * @throws IllegalArgumentException if the method name expression resolves to null or an empty string,
	 * or if the method name is not found in the service descriptor
	 * @throws UnsupportedOperationException if the method type is not recognized
	 */
	@SuppressWarnings("NullAway")
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		MethodDescriptor.MethodType methodType;
		MethodDescriptor<?, ?> methodDescriptor;

		Object methodNameObject = this.methodNameExpression.getValue(requestMessage, String.class);
		String methodName;
		Assert.state(methodNameObject != null, "MethodNameExpression must not resolve to null");
		methodName = methodNameObject.toString();
		Assert.state(!methodName.isEmpty(), "MethodNameExpression must not resolve to an empty string");

		methodDescriptor = getMethodDescriptor(methodName, this.grpcServiceClass);
		methodType = methodDescriptor.getType();

		Object request = requestMessage.getPayload();

		this.logger.debug(() -> "Invoking gRPC method '" + methodDescriptor.getBareMethodName() +
				"' with payload: " + request);

		@SuppressWarnings("unchecked")
		Object grpcResponse = switch (methodType) {
			case BIDI_STREAMING -> invokeBidirectionalStreaming(request, methodDescriptor);
			case SERVER_STREAMING -> invokeServerStreaming(request, methodDescriptor);
			case CLIENT_STREAMING -> invokeClientStreaming(request, methodDescriptor);
			case UNARY -> invokeBlocking(
					(MethodDescriptor<Object, Object>) methodDescriptor, request);
			default -> throw new UnsupportedOperationException("Unexpected method type: " + methodType);
		};

		return grpcResponse;
	}

	private Object invokeClientStreaming(Object request, MethodDescriptor<?, ?> methodDescriptor) {
		Sinks.One<Object> responseSink = Sinks.one();

		StreamObserver<Object> responseObserver = new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				Sinks.EmitResult result = responseSink.tryEmitValue(value);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn(() -> "Failed to emit value to sink: " + result);
				}
			}

			@Override
			public void onError(Throwable ex) {
				Sinks.EmitResult result = responseSink.tryEmitError(ex);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.error(ex, () -> "Failed to emit error to sink: " + result);
				}
			}

			@Override
			public void onCompleted() {
			}

		};

		this.subscribeToFlux(request, responseObserver, methodDescriptor);

		return responseSink.asMono();
	}

	@SuppressWarnings({"NullAway", "unchecked"})
	private void subscribeToFlux(Object request, StreamObserver<Object> responseObserver,
			MethodDescriptor<?, ?> methodDescriptor) {
		Flux<Object> typedRequestFlux = this.getFluxFromRequest(request);

		StreamObserver<Object> requestObserver =
				this.invokeBiDirectional(methodDescriptor, responseObserver);

		typedRequestFlux.subscribe(
				requestObserver::onNext,
				ex -> {
					requestObserver.onError(ex);
					responseObserver.onError(ex);
				},
				requestObserver::onCompleted
		);
	}

	@SuppressWarnings("unchecked")
	private Flux<Object> getFluxFromRequest(Object request) {
		Flux<Object> typedRequestFlux;
		if (request instanceof Flux<?> requestFlux) {
			typedRequestFlux = (Flux<Object>) requestFlux;
		}
		else if (request instanceof Mono<?> requestMono) {
			typedRequestFlux = Flux.from(requestMono);
		}
		else if (request instanceof Stream<?> requestStream) {
			typedRequestFlux = Flux.fromStream(requestStream);
		}
		else if (request instanceof Collection<?> requestCollection) {
			typedRequestFlux = Flux.fromIterable(requestCollection);
		}
		else {
			typedRequestFlux = Flux.just(request);
		}
		return typedRequestFlux;
	}

	private Object invokeBidirectionalStreaming(Object request, MethodDescriptor<?, ?> methodDescriptor) {
		Sinks.Many<Object> responseSink = Sinks.many().unicast().onBackpressureBuffer();

		StreamObserver<Object> responseObserver = new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				Sinks.EmitResult result = responseSink.tryEmitNext(value);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn(() -> "Failed to emit value to sink: " + result);
				}
			}

			@Override
			public void onError(Throwable ex) {
				Sinks.EmitResult result = responseSink.tryEmitError(ex);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.error(ex, () -> "Failed to emit error to sink: " + result);
				}
			}

			@Override
			public void onCompleted() {
				Sinks.EmitResult result = responseSink.tryEmitComplete();
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn(() -> "Failed to emit completion to sink: " + result);
				}
			}

		};

		this.subscribeToFlux(request, responseObserver, methodDescriptor);

		return responseSink.asFlux();
	}

	@SuppressWarnings("NullAway")
	private Object invokeServerStreaming(Object request, MethodDescriptor<?, ?> methodDescriptor) {
		Sinks.Many<Object> responseSink = Sinks.many().unicast().onBackpressureBuffer();

		StreamObserver<Object> responseObserver = new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				Sinks.EmitResult result = responseSink.tryEmitNext(value);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn(() -> "Failed to emit value to sink: " + result);
				}
			}

			@Override
			public void onError(Throwable ex) {
				Sinks.EmitResult result = responseSink.tryEmitError(ex);
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.error(ex, () -> "Failed to emit error to sink: " + result);
				}
			}

			@Override
			public void onCompleted() {
				Sinks.EmitResult result = responseSink.tryEmitComplete();
				if (result.isFailure()) {
					GrpcOutboundGateway.this.logger.warn(() -> "Failed to emit completion to sink: " + result);
				}
			}

		};

		// Unary or Server-streaming
		this.invoke(methodDescriptor, request, responseObserver);

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
	private void invoke(MethodDescriptor method, Object request,
			StreamObserver responseObserver) {

		ClientCall call = this.channel.newCall(method, CallOptions.DEFAULT);
		ClientCalls.asyncServerStreamingCall(call, request, responseObserver);

	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private StreamObserver invokeBiDirectional(MethodDescriptor method,
			StreamObserver responseObserver) {

		ClientCall call = this.channel.newCall(method, CallOptions.DEFAULT);

		return switch (method.getType()) {
			case CLIENT_STREAMING -> ClientCalls.asyncClientStreamingCall(call, responseObserver);
			case BIDI_STREAMING -> ClientCalls.asyncBidiStreamingCall(call, responseObserver);
			default -> throw new IllegalStateException("Unknown method type: " + method.getType());
		};
	}

	private <ReqT, RespT> RespT invokeBlocking(MethodDescriptor<ReqT, RespT> method, ReqT request) {
		return ClientCalls.blockingUnaryCall(this.channel, method, CallOptions.DEFAULT, request);
	}

}

