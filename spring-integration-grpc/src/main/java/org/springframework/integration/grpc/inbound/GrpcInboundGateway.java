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

package org.springframework.integration.grpc.inbound;

import java.lang.reflect.Method;
import java.util.Arrays;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.gateway.DefaultMethodInvokingMethodInterceptor;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.grpc.GrpcHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * The {@link MessagingGatewaySupport} implementation for gRPC {@link BindableService}.
 * An instance of this class requires a {@link BindableService} class from the gRPC service definition.
 * Only standard 'grpc' services are supported which implements a generated {@code AsyncService} interface.
 * This gateway is a {@link BindableService} by itself to be registered with the gRPC server.
 * An internal proxy is created to intercept gRPC method calls and convert them to Spring Integration messages.
 * A reply from the downstream flow is produced back to the gRPC response payload.
 * The request payload is a Proto message from gRPC request.
 * The reply payload must be a Proto message for gRPC response.
 * <p>
 * This gateway supports all the gRPC {@link MethodDescriptor.MethodType} types.
 * All the requests are produced to downstream flow in a reactive manner via {@link #sendAndReceiveMessageReactive(Object)}.
 * The {@link MethodDescriptor.MethodType#UNARY} and {@link MethodDescriptor.MethodType#BIDI_STREAMING}
 * are same from the downstream handling logic perspective.
 * The {@link MethodDescriptor.MethodType#CLIENT_STREAMING} produces a {@link Flux} of gRPC request payloads.
 * The {@link MethodDescriptor.MethodType#SERVER_STREAMING} reply can be a single entity or a {@link Flux} of them.
 * <p>
 * For convenience, the {@link GrpcHeaders} are populated into a request message.
 * Such information can be used, for example, in downstream flow for routing.
 *
 * @author Artem Bilan
 *
 * @since 7.1
 */
public class GrpcInboundGateway extends MessagingGatewaySupport implements BindableService {

	private final Class<? extends BindableService> grpcServiceClass;

	@SuppressWarnings("NullAway.Init")
	private Object asyncService;

	@SuppressWarnings("NullAway.Init")
	private ServerServiceDefinition serverServiceDefinition;

	public GrpcInboundGateway(Class<? extends BindableService> grpcServiceClass) {
		this.grpcServiceClass = grpcServiceClass;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Class<?>[] serviceInterfaces =
				ClassUtils.getAllInterfacesForClass(this.grpcServiceClass, getApplicationContext().getClassLoader());

		for (Class<?> serviceInterface : serviceInterfaces) {
			if ("AsyncService".equals(serviceInterface.getSimpleName())) {
				createServiceProxyAndServerDefinition(serviceInterface);
				break;
			}
		}

		Assert.state(this.asyncService != null,
				"Only standard 'grpc' service are supported providing an 'AsyncService' contract.");
	}

	@SuppressWarnings("NullAway")
	private void createServiceProxyAndServerDefinition(Class<?> serviceInterface) {
		ProxyFactory proxyFactory = new ProxyFactory(serviceInterface, (MethodInterceptor) this::interceptGrpc);
		proxyFactory.addAdvice(new DefaultMethodInvokingMethodInterceptor());
		this.asyncService = proxyFactory.getProxy(getApplicationContext().getClassLoader());
		Method bindServiceMethod =
				ClassUtils.getStaticMethod(this.grpcServiceClass.getEnclosingClass(), "bindService", serviceInterface);

		this.serverServiceDefinition =
				(ServerServiceDefinition) ReflectionUtils.invokeMethod(bindServiceMethod, null, this.asyncService);
	}

	@Override
	public ServerServiceDefinition bindService() {
		return this.serverServiceDefinition;
	}

	@SuppressWarnings({"unchecked", "NullAway"})
	private @Nullable Object interceptGrpc(MethodInvocation invocation) {
		Object[] arguments = invocation.getArguments();

		String fullMethodName =
				this.serverServiceDefinition.getServiceDescriptor().getName() +
						'/' +
						StringUtils.capitalize(invocation.getMethod().getName());

		MethodDescriptor<?, ?> serviceMethod =
				this.serverServiceDefinition.getMethod(fullMethodName)
						.getMethodDescriptor();

		logger.debug(LogMessage.format("gRPC request for [%s] with arguments %s",
				fullMethodName, Arrays.toString(arguments)));

		switch (serviceMethod.getType()) {
			case UNARY -> {
				unary(serviceMethod, arguments[0], (StreamObserver<Object>) arguments[1]);
				return null;
			}
			case SERVER_STREAMING -> {
				serverStreaming(serviceMethod, arguments[0], (StreamObserver<Object>) arguments[1]);
				return null;
			}
			case CLIENT_STREAMING -> {
				return clientStreaming(serviceMethod, (StreamObserver<Object>) arguments[0]);
			}
			case BIDI_STREAMING -> {
				return bidiStreaming(serviceMethod, (StreamObserver<Object>) arguments[0]);
			}
			default -> throw new IllegalStateException("Unknown gRPC method type: " + serviceMethod.getType());
		}
	}

	private void unary(MethodDescriptor<?, ?> methodDescriptor, Object requestPayload,
			StreamObserver<Object> responseObserver) {

		sendRequestAndProduceReply(methodDescriptor, requestPayload)
				.subscribe(responseObserver::onNext,
						t -> responseObserver.onError(toGrpcStatusException(t)),
						responseObserver::onCompleted);
	}

	private void serverStreaming(MethodDescriptor<?, ?> methodDescriptor, Object requestPayload,
			StreamObserver<Object> responseObserver) {

		sendRequestAndProduceReply(methodDescriptor, requestPayload)
				.flatMapMany(payload -> payload instanceof Flux<?> flux ? flux : Flux.just(payload))
				.subscribe(responseObserver::onNext,
						t -> responseObserver.onError(toGrpcStatusException(t)),
						responseObserver::onCompleted);
	}

	private StreamObserver<?> clientStreaming(MethodDescriptor<?, ?> methodDescriptor,
			StreamObserver<Object> responseObserver) {

		Sinks.Many<Object> requestPayload = Sinks.many().unicast().onBackpressureBuffer();

		return new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				requestPayload.tryEmitNext(value);
			}

			@Override
			public void onError(Throwable t) {
				throw toGrpcStatusException(t, "gRPC request [" + methodDescriptor.getFullMethodName() + "] has failed");
			}

			@Override
			public void onCompleted() {
				requestPayload.tryEmitComplete();
				sendRequestAndProduceReply(methodDescriptor, requestPayload.asFlux())
						.subscribe(responseObserver::onNext,
								t -> responseObserver.onError(toGrpcStatusException(t)),
								responseObserver::onCompleted);
			}

		};
	}

	private StreamObserver<?> bidiStreaming(MethodDescriptor<?, ?> methodDescriptor,
			StreamObserver<Object> responseObserver) {

		return new StreamObserver<>() {

			@Override
			public void onNext(Object value) {
				sendRequestAndProduceReply(methodDescriptor, value)
						.subscribe(responseObserver::onNext,
								t -> responseObserver.onError(toGrpcStatusException(t)));
			}

			@Override
			public void onError(Throwable t) {
				throw toGrpcStatusException(t, "gRPC request [" + methodDescriptor.getFullMethodName() + "] has failed");
			}

			@Override
			public void onCompleted() {
				responseObserver.onCompleted();
			}

		};
	}

	private Mono<?> sendRequestAndProduceReply(MethodDescriptor<?, ?> serviceMethod, Object requestPayload) {
		Message<?> requestMessage =
				getMessageBuilderFactory()
						.withPayload(requestPayload)
						.setHeader(GrpcHeaders.SERVICE, serviceMethod.getServiceName())
						.setHeader(GrpcHeaders.SERVICE_METHOD, serviceMethod.getBareMethodName())
						.setHeader(GrpcHeaders.METHOD_TYPE, serviceMethod.getType())
						.setHeader(GrpcHeaders.SCHEMA_DESCRIPTOR, serviceMethod.getSchemaDescriptor())
						.build();

		return sendAndReceiveMessageReactive(requestMessage)
				.map(Message::getPayload);
	}

	private static StatusRuntimeException toGrpcStatusException(Throwable throwable) {
		return toGrpcStatusException(throwable, throwable.getMessage());
	}

	private static StatusRuntimeException toGrpcStatusException(Throwable throwable, @Nullable String description) {
		return Status.fromThrowable(throwable)
				.withDescription(description)
				.asRuntimeException();
	}

}
