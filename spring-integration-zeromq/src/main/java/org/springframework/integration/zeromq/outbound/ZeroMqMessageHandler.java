/*
 * Copyright 2020-2024 the original author or authors.
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

package org.springframework.integration.zeromq.outbound;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import zmq.socket.pubsub.Pub;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.SupplierExpression;
import org.springframework.integration.handler.AbstractReactiveMessageHandler;
import org.springframework.integration.mapping.ConvertingBytesMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.zeromq.ZeroMqUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * The {@link AbstractReactiveMessageHandler} implementation for publishing messages over ZeroMq socket.
 * Only {@link SocketType#PAIR}, {@link SocketType#PUB} and {@link SocketType#PUSH} are supported.
 * This component can bind or connect the socket.
 * <p>
 * When the {@link SocketType#PUB} is used, the {@link #topicExpression} is evaluated against a
 * request message to inject a topic frame into a ZeroMq message if it is not {@code null}.
 * The subscriber side must receive the topic frame first before parsing the actual data.
 * <p>
 * When the payload of the request message is a {@link ZMsg}, no any conversion and topic extraction happen:
 * the {@link ZMsg} is sent into a socket as is, and it is not destroyed for possible further reusing.
 *
 * @author Artem Bilan
 * @author Alessio Matricardi
 *
 * @since 5.4
 */
public class ZeroMqMessageHandler extends AbstractReactiveMessageHandler
		implements ManageableLifecycle {

	private static final List<SocketType> VALID_SOCKET_TYPES =
			List.of(SocketType.PAIR, SocketType.PUSH, SocketType.PUB);

	private final AtomicBoolean running = new AtomicBoolean();

	private final Scheduler publisherScheduler = Schedulers.newSingle("zeroMqMessageHandlerScheduler");

	private volatile Mono<ZMQ.Socket> socketMono;

	private OutboundMessageMapper<byte[]> messageMapper;

	private Consumer<ZMQ.Socket> socketConfigurer = (socket) -> {
	};

	private Expression topicExpression = new SupplierExpression<>(() -> null);

	private EvaluationContext evaluationContext;

	private volatile boolean initialized;

	private volatile Disposable socketMonoSubscriber;

	private volatile boolean wrapTopic = true;

	private final ZContext context;

	private final SocketType socketType;

	private final AtomicInteger bindPort = new AtomicInteger();

	@Nullable
	private Supplier<String> connectUrl;

	/**
	 * Create an instance based on the provided {@link ZContext}.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @since 6.4
	 */
	public ZeroMqMessageHandler(ZContext context) {
		this(context, SocketType.PAIR);
	}

	/**
	 * Create an instance based on the provided {@link ZContext} and {@link SocketType}.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param socketType the {@link SocketType} to use;
	 *    only {@link SocketType#PAIR}, {@link SocketType#PUB} and {@link SocketType#PUSH} are supported.
	 */
	public ZeroMqMessageHandler(ZContext context, SocketType socketType) {
		Assert.notNull(context, "'context' must not be null");
		Assert.state(VALID_SOCKET_TYPES.contains(socketType),
				() -> "'socketType' can only be one of the: " + VALID_SOCKET_TYPES);
		this.context = context;
		this.socketType = socketType;
	}

	/**
	 * Create an instance based on the provided {@link ZContext} and connection string.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param connectUrl the URL to connect the socket to.
	 */
	public ZeroMqMessageHandler(ZContext context, String connectUrl) {
		this(context, connectUrl, SocketType.PAIR);
	}

	/**
	 * Create an instance based on the provided {@link ZContext} and binding port.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param port the port to bind ZeroMq socket to over TCP.
	 * @since 6.4
	 */
	public ZeroMqMessageHandler(ZContext context, int port) {
		this(context, port, SocketType.PAIR);
	}

	/**
	 * Create an instance based on the provided {@link ZContext} and connection string supplier.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param connectUrl the supplier for URL to connect the socket to.
	 * @since 5.5.9
	 */
	public ZeroMqMessageHandler(ZContext context, Supplier<String> connectUrl) {
		this(context, connectUrl, SocketType.PAIR);
	}

	/**
	 * Create an instance based on the provided {@link ZContext}, connection string and {@link SocketType}.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param connectUrl the URL to connect the socket to.
	 * @param socketType the {@link SocketType} to use;
	 *    only {@link SocketType#PAIR}, {@link SocketType#PUB} and {@link SocketType#PUSH} are supported.
	 */
	public ZeroMqMessageHandler(ZContext context, String connectUrl, SocketType socketType) {
		this(context, () -> connectUrl, socketType);
		Assert.hasText(connectUrl, "'connectUrl' must not be empty");
	}

	/**
	 * Create an instance based on the provided {@link ZContext}, binding port and {@link SocketType}.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param port the port to bind ZeroMq socket to over TCP.
	 * @param socketType the {@link SocketType} to use;
	 *    only {@link SocketType#PAIR}, {@link SocketType#PUB} and {@link SocketType#PUSH} are supported.
	 * @since 6.4
	 */
	public ZeroMqMessageHandler(ZContext context, int port, SocketType socketType) {
		this(context, socketType);
		Assert.isTrue(port > 0, "'port' must not be zero or negative");
		this.bindPort.set(port);
	}

	/**
	 * Create an instance based on the provided {@link ZContext}, connection string supplier and {@link SocketType}.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param connectUrl the supplier for URL to connect the socket to.
	 * @param socketType the {@link SocketType} to use;
	 *    only {@link SocketType#PAIR}, {@link SocketType#PUB} and {@link SocketType#PUSH} are supported.
	 * @since 5.5.9
	 */
	public ZeroMqMessageHandler(ZContext context, Supplier<String> connectUrl, SocketType socketType) {
		this(context, socketType);
		Assert.notNull(connectUrl, "'connectUrl' must not be null");
		this.connectUrl = connectUrl;
	}

	/**
	 * Provide an {@link OutboundMessageMapper} to convert a request message into {@code byte[]}
	 * for sending into ZeroMq socket.
	 * Ignored when {@link Message#getPayload()} is an instance of {@link ZMsg}.
	 * @param messageMapper the {@link OutboundMessageMapper} to use.
	 */
	public void setMessageMapper(OutboundMessageMapper<byte[]> messageMapper) {
		Assert.notNull(messageMapper, "'messageMapper' must not be null");
		this.messageMapper = messageMapper;
	}

	/**
	 * Provide a {@link MessageConverter} (as an alternative to {@link #messageMapper})
	 * for converting a request message into {@code byte[]} for sending into ZeroMq socket.
	 * Ignored when {@link Message#getPayload()} is an instance of {@link ZMsg}.
	 * @param messageConverter the {@link MessageConverter} to use.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		setMessageMapper(new ConvertingBytesMessageMapper(messageConverter));
	}

	/**
	 * Provide a {@link Consumer} to configure a socket with arbitrary options, like security.
	 * @param socketConfigurer the configurer for socket options.
	 */
	public void setSocketConfigurer(Consumer<ZMQ.Socket> socketConfigurer) {
		Assert.notNull(socketConfigurer, "'socketConfigurer' must not be null");
		this.socketConfigurer = socketConfigurer;
	}

	/**
	 * Specify a topic the {@link SocketType#PUB} socket is going to use for distributing messages into the
	 * subscriptions. It is ignored for all other {@link SocketType}s supported.
	 * @param topic the topic to use.
	 */
	public void setTopic(String topic) {
		setTopicExpression(new LiteralExpression(topic));
	}

	/**
	 * Specify a SpEL expression to evaluate a topic a {@link SocketType#PUB}
	 * is going to use for distributing messages into the
	 * subscriptions.It is ignored for all other {@link SocketType}s supported.
	 * @param topicExpression the expression to evaluate topic for publishing.
	 */
	public void setTopicExpression(Expression topicExpression) {
		Assert.notNull(topicExpression, "'topicExpression' must not be null");
		this.topicExpression = topicExpression;
	}

	/**
	 * Specify if the topic that {@link SocketType#PUB} socket is going to use for distributing messages into the
	 * subscriptions must be wrapped with an additional empty frame.
	 * It is ignored for all other {@link SocketType}s supported.
	 * This attribute is set to {@code true} by default.
	 * @param wrapTopic true if the topic must be wrapped with an additional empty frame.
	 * @since 6.2.6
	 */
	public void wrapTopic(boolean wrapTopic) {
		this.wrapTopic = wrapTopic;
	}

	/**
	 * Return the port a socket is bound or 0 if this message producer has not been started yet
	 * or the socket is connected - not bound.
	 * @return the port for a socket or 0.
	 * @since 6.4
	 */
	public int getBoundPort() {
		return this.bindPort.get();
	}

	@Override
	public String getComponentType() {
		return "zeromq:outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
		if (this.messageMapper == null) {
			ConfigurableCompositeMessageConverter messageConverter = new ConfigurableCompositeMessageConverter();
			messageConverter.setBeanFactory(beanFactory);
			messageConverter.afterPropertiesSet();
			this.messageMapper = new ConvertingBytesMessageMapper(messageConverter);
		}
		this.initialized = true;
	}

	@Override
	public void start() {
		if (!this.running.getAndSet(true)) {
			this.socketMono =
					Mono.just(this.context.createSocket(this.socketType))
							.publishOn(this.publisherScheduler)
							.doOnNext((socket) -> this.socketConfigurer.accept(socket))
							.doOnNext((socket) -> {
								if (this.connectUrl != null) {
									socket.connect(this.connectUrl.get());
								}
								else {
									this.bindPort.set(ZeroMqUtils.bindSocket(socket, this.bindPort.get()));
								}
							})
							.cache()
							.publishOn(this.publisherScheduler);
			this.socketMonoSubscriber = this.socketMono.subscribe();
		}
	}

	@Override
	public void stop() {
		if (this.running.getAndSet(false)) {
			this.socketMonoSubscriber.dispose();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {
		Assert.state(this.initialized, "the message handler is not initialized yet or already destroyed");
		return this.socketMono
				.doOnNext((socket) -> {
					ZMsg msg;
					if (message.getPayload() instanceof ZMsg) {
						msg = (ZMsg) message.getPayload();
					}
					else {
						msg = new ZMsg();
						msg.add(this.messageMapper.fromMessage(message));
						if (socket.base() instanceof Pub) {
							String topic = this.topicExpression.getValue(this.evaluationContext, message, String.class);
							if (topic != null) {
								ZFrame topicFrame = new ZFrame(topic);
								if (this.wrapTopic) {
									msg.wrap(topicFrame);
								}
								else {
									msg.push(topicFrame);
								}
							}
						}
					}
					msg.send(socket, false);
				})
				.then();
	}

	@Override
	public void destroy() {
		this.initialized = false;
		super.destroy();
		this.socketMono.doOnNext(ZMQ.Socket::close).block();
		this.socketMonoSubscriber.dispose();
		this.publisherScheduler.dispose();
	}

}
