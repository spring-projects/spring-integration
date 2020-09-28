/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.zeromq.dsl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ReactiveMessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.zeromq.outbound.ZeroMqMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;

/**
 * The {@link ReactiveMessageHandlerSpec} extension for {@link ZeroMqMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ZeroMqMessageHandlerSpec
		extends ReactiveMessageHandlerSpec<ZeroMqMessageHandlerSpec, ZeroMqMessageHandler> {

	/**
	 * Create an instance based on the provided {@link ZContext} and connection string.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param connectUrl the URL to connect the socket to.
	 */
	protected ZeroMqMessageHandlerSpec(ZContext context, String connectUrl) {
		super(new ZeroMqMessageHandler(context, connectUrl));
	}

	/**
	 * Create an instance based on the provided {@link ZContext}, connection string and {@link SocketType}.
	 * @param context the {@link ZContext} to use for creating sockets.
	 * @param connectUrl the URL to connect the socket to.
	 * @param socketType the {@link SocketType} to use;
	 *    only {@link SocketType#PAIR}, {@link SocketType#PUB} and {@link SocketType#PUSH} are supported.
	 */
	protected ZeroMqMessageHandlerSpec(ZContext context, String connectUrl, SocketType socketType) {
		super(new ZeroMqMessageHandler(context, connectUrl, socketType));
	}

	/**
	 * Provide an {@link OutboundMessageMapper} to convert a request message into {@code byte[]}
	 * for sending into ZeroMq socket.
	 * @param messageMapper the {@link OutboundMessageMapper} to use.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec messageMapper(OutboundMessageMapper<byte[]> messageMapper) {
		this.reactiveMessageHandler.setMessageMapper(messageMapper);
		return this;
	}

	/**
	 * Provide a {@link MessageConverter} (as an alternative to {@link #messageMapper})
	 * for converting a request message into {@code byte[]} for sending into ZeroMq socket.
	 * @param messageConverter the {@link MessageConverter} to use.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec messageConverter(MessageConverter messageConverter) {
		this.reactiveMessageHandler.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * Provide a {@link Consumer} to configure a socket with arbitrary options, like security.
	 * @param socketConfigurer the configurer for socket options.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec socketConfigurer(Consumer<ZMQ.Socket> socketConfigurer) {
		this.reactiveMessageHandler.setSocketConfigurer(socketConfigurer);
		return this;
	}

	/**
	 * Specify a topic the {@link SocketType#PUB} socket is going to use for distributing messages into the
	 * subscriptions. It is ignored for all other {@link SocketType}s supported.
	 * @param topic the topic to use.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec topic(String topic) {
		this.reactiveMessageHandler.setTopic(topic);
		return this;
	}

	/**
	 * Specify a {@link Function} to evaluate a topic a {@link SocketType#PUB}
	 * is going to use for distributing messages into the
	 * subscriptions.It is ignored for all other {@link SocketType}s supported.
	 * @param topicFunction the {@link Function} to evaluate topic for publishing.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec topicFunction(Function<Message<?>, String> topicFunction) {
		return topicExpression(new FunctionExpression<>(topicFunction));
	}

	/**
	 * Specify a SpEL expression to evaluate a topic a {@link SocketType#PUB}
	 * is going to use for distributing messages into the
	 * subscriptions.It is ignored for all other {@link SocketType}s supported.
	 * @param topicExpression the expression to evaluate topic for publishing.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec topicExpression(String topicExpression) {
		return topicExpression(PARSER.parseExpression(topicExpression));
	}

	/**
	 * Specify a SpEL expression to evaluate a topic a {@link SocketType#PUB}
	 * is going to use for distributing messages into the
	 * subscriptions.It is ignored for all other {@link SocketType}s supported.
	 * @param topicExpression the expression to evaluate topic for publishing.
	 * @return the spec
	 */
	public ZeroMqMessageHandlerSpec topicExpression(Expression topicExpression) {
		this.reactiveMessageHandler.setTopicExpression(topicExpression);
		return this;
	}

}
