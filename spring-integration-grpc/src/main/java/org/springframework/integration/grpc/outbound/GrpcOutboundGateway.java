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

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.grpc.GrpcHeaders;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * The Outbound Gateway for gRPC client invocations.
 * <p>
 * This component acts as a gRPC stub where the request is based on the input message,
 * the target gRPC service method to invoke is resolved at runtime based on a SpEL expression
 * against the input message.
 * <p>
 * Supported gRPC requests:
 * <ul>
 *   <li><b>Unary:</b> Accepts a single request object</li>
 *   <li><b>Server Streaming:</b> Accepts a single request object</li>
 *   <li><b>Client Streaming:</b> Accepts {@link Flux}, {@link Mono}, {@link java.util.stream.Stream Stream},
 *       {@link java.util.Collection Collection}, or a single object</li>
 *   <li><b>Bidirectional Streaming:</b> Accepts {@link Flux}, {@link Mono}, {@link java.util.stream.Stream Stream},
 *       {@link java.util.Collection Collection}, or a single object</li>
 * </ul>
 * The request object must be of an expected domain entity generated from the respective service Protobuf.
 * <p>
 *  <b>Return type for the requests from the gateway:</b>
 * 	<ul>
 * 	 <li><b>Unary:</b> Returns a {@link Mono} containing the response object by default.  If {@link #isAsync()} is set
 * 	 to false, it will return the response object.</li>
 * 	 <li><b>Server Streaming:</b> Returns a {@link Flux} of response messages</li>
 * 	 <li><b>Client Streaming:</b> Returns a {@link Mono} with the single response</li>
 * 	<li><b>Bidirectional Streaming:</b> Returns a {@link Flux} of response messages</li>
 *  </ul>
 * </p>
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
public class GrpcOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final Channel channel;

	private final Class<?> grpcServiceClass;

	private final ServiceDescriptor serviceDescriptor;

	@SuppressWarnings("NullAway")
	private Expression methodNameExpression =
			new FunctionExpression<Message<?>>((msg) -> msg.getHeaders().get(GrpcHeaders.SERVICE_METHOD));

	private boolean methodNameExpressionSet;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	private CallOptions callOptions = CallOptions.DEFAULT;

	/**
	 * Create a new {@code GrpcOutboundGateway} based on the gRPC service class and {@link Channel} for communication.
	 * The gateway is set to async mode by default.
	 * @param channel the gRPC channel to use for communication
	 * @param grpcServiceClass the gRPC service class (e.g., {@code SimpleGrpc.class})
	 */
	@SuppressWarnings("this-escape")
	public GrpcOutboundGateway(Channel channel, Class<?> grpcServiceClass) {
		this.channel = channel;
		this.grpcServiceClass = grpcServiceClass;
		Method getServiceDescriptor = ClassUtils.getMethod(this.grpcServiceClass, "getServiceDescriptor");
		this.serviceDescriptor =
				(ServiceDescriptor) Objects.requireNonNull(ReflectionUtils.invokeMethod(getServiceDescriptor,
						null));
		setAsync(true);
	}

	/**
	 * Set the {@link Expression} to resolve the gRPC method name at runtime.
	 * If not provided, the default expression checks the {@link org.springframework.messaging.MessageHeaders}
	 * for a {@link GrpcHeaders#SERVICE_METHOD}. If the expression is not set and the service has only one method,
	 * then the gateway will set the expression to use the name of that method.
	 * @param methodNameExpression the expression to resolve the method name
	 */
	public void setMethodNameExpression(Expression methodNameExpression) {
		this.methodNameExpression = methodNameExpression;
		this.methodNameExpressionSet = true;
	}

	/**
	 * Set the name of the gRPC method to call.
	 * If method name is not provided, the default expression checks the
	 * {@link org.springframework.messaging.MessageHeaders} for {@link GrpcHeaders#SERVICE_METHOD} or, in case a
	 * single service method, the name of that method is used.
	 * @param methodName the name of the gRPC method to call
	 */
	public void setMethodName(String methodName) {
		setMethodNameExpression(new LiteralExpression(methodName));
	}

	/**
	 * Set the {@link CallOptions} the RPC call.
	 * Default is {@link CallOptions#DEFAULT}
	 * @param callOptions the {@link CallOptions} for the gateway.
	 */
	public void setCallOptions(CallOptions callOptions) {
		this.callOptions = callOptions;
	}

	@Override
	protected void doInit() {
		super.doInit();
		if (!this.methodNameExpressionSet) {
			MethodDescriptor<?, ?>[] methods = this.serviceDescriptor.getMethods().toArray(MethodDescriptor[]::new);
			if (methods.length == 1) {
				String methodName = Objects.requireNonNull(methods[0].getBareMethodName());
				this.methodNameExpression = new LiteralExpression(methodName);
			}
		}
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String methodName = this.methodNameExpression.getValue(this.evaluationContext, requestMessage, String.class);
		Assert.state(StringUtils.hasText(methodName),
				"The 'methodNameExpression' must not resolve to an empty string");

		MethodDescriptor<Object, Object> methodDescriptor = getMethodDescriptor(methodName, this.grpcServiceClass);
		MethodDescriptor.MethodType methodType = methodDescriptor.getType();

		Object request = requestMessage.getPayload();

		this.logger.debug(() -> "Invoking gRPC method '" + methodDescriptor + "' for request: " + request);

		return switch (methodType) {
			case UNARY -> invokeUnary(request, methodDescriptor);
			case SERVER_STREAMING -> invokeServerStreaming(request, methodDescriptor);
			case CLIENT_STREAMING -> invokeClientStreaming(request, methodDescriptor);
			case BIDI_STREAMING -> invokeBidirectionalStreaming(request, methodDescriptor);
			default -> throw new UnsupportedOperationException("Unexpected method type: " + methodType);
		};
	}

	@SuppressWarnings("unchecked")
	private MethodDescriptor<Object, Object> getMethodDescriptor(String methodName, Class<?> grpcServiceClass) {
		for (MethodDescriptor<?, ?> method : this.serviceDescriptor.getMethods()) {
			if (methodName.equalsIgnoreCase(method.getBareMethodName())) {
				return (MethodDescriptor<Object, Object>) method;
			}
		}

		throw new IllegalArgumentException(
				"Method '" + methodName + "' not found in service class: " + grpcServiceClass.getName());
	}

	private Object invokeUnary(Object request, MethodDescriptor<Object, Object> methodDescriptor) {
		ClientCall<Object, Object> call = this.channel.newCall(methodDescriptor, this.callOptions);

		if (isAsync()) {
			Sinks.One<Object> responseSink = Sinks.one();
			StreamObserver<Object> responseObserver = createSinkOneResponseObserver(responseSink);
			ClientCalls.asyncUnaryCall(call, request, responseObserver);
			return responseSink.asMono();
		}

		return ClientCalls.blockingUnaryCall(call, request);
	}

	private Flux<?> invokeServerStreaming(Object request, MethodDescriptor<Object, Object> methodDescriptor) {
		Sinks.Many<Object> responseSink = Sinks.many().unicast().onBackpressureBuffer();

		StreamObserver<Object> responseObserver = createSinkManyResponseObserver(responseSink);

		ClientCall<Object, Object> call = this.channel.newCall(methodDescriptor, this.callOptions);
		ClientCalls.asyncServerStreamingCall(call, request, responseObserver);

		return responseSink.asFlux();
	}

	private Mono<?> invokeClientStreaming(Object request, MethodDescriptor<Object, Object> methodDescriptor) {
		Sinks.One<Object> responseSink = Sinks.one();

		StreamObserver<Object> responseObserver = createSinkOneResponseObserver(responseSink);

		handleRequestAsFlux(request, methodDescriptor, responseObserver);

		return responseSink.asMono();
	}

	private Flux<?> invokeBidirectionalStreaming(Object request, MethodDescriptor<Object, Object> methodDescriptor) {
		Sinks.Many<Object> responseSink = Sinks.many().unicast().onBackpressureBuffer();

		StreamObserver<Object> responseObserver = createSinkManyResponseObserver(responseSink);

		handleRequestAsFlux(request, methodDescriptor, responseObserver);

		return responseSink.asFlux();
	}

	private void handleRequestAsFlux(Object request, MethodDescriptor<Object, Object> methodDescriptor,
			StreamObserver<Object> responseObserver) {

		Flux<?> typedRequestFlux = requestToFlux(request);
		StreamObserver<Object> requestObserver = invokeStreamingCall(methodDescriptor, responseObserver);

		typedRequestFlux.subscribe(
				requestObserver::onNext,
				ex -> {
					requestObserver.onError(ex);
					responseObserver.onError(ex);
				},
				requestObserver::onCompleted
		);
	}

	private Flux<?> requestToFlux(Object request) {
		if (request instanceof Flux<?> requestFlux) {
			return requestFlux;
		}
		else if (request instanceof Mono<?> requestMono) {
			return Flux.from(requestMono);
		}
		else if (request instanceof Stream<?> requestStream) {
			return Flux.fromStream(requestStream);
		}
		else if (request instanceof Collection<?> requestCollection) {
			return Flux.fromIterable(requestCollection);
		}
		else if (ObjectUtils.isArray(request)) {
			return Flux.fromIterable(CollectionUtils.arrayToList(request));
		}

		return Flux.just(request);
	}

	private StreamObserver<Object> invokeStreamingCall(MethodDescriptor<Object, Object> method,
			StreamObserver<Object> responseObserver) {

		ClientCall<Object, Object> call = this.channel.newCall(method, this.callOptions);

		if (MethodDescriptor.MethodType.CLIENT_STREAMING.equals(method.getType())) {
			return ClientCalls.asyncClientStreamingCall(call, responseObserver);
		}

		return ClientCalls.asyncBidiStreamingCall(call, responseObserver);
	}

	private StreamObserver<Object> createSinkOneResponseObserver(Sinks.One<Object> responseSink) {
		return new StreamObserver<>() {

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
				responseSink.tryEmitEmpty();
			}

		};
	}

	private StreamObserver<Object> createSinkManyResponseObserver(Sinks.Many<Object> responseSink) {
		return new StreamObserver<>() {

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
	}

}

