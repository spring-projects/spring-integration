/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.zeromq.dsl;

import java.time.Duration;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.zeromq.inbound.ZeroMqMessageProducer;
import org.springframework.messaging.converter.MessageConverter;

/**
 * @author Artem Bilan
 * @author Alessio Matricardi
 *
 * @since 5.4
 */
public class ZeroMqMessageProducerSpec
		extends MessageProducerSpec<ZeroMqMessageProducerSpec, ZeroMqMessageProducer> {

	protected ZeroMqMessageProducerSpec(ZContext context) {
		super(new ZeroMqMessageProducer(context));
	}

	protected ZeroMqMessageProducerSpec(ZContext context, SocketType socketType) {
		super(new ZeroMqMessageProducer(context, socketType));
	}

	/**
	 * Specify a {@link Duration} to delay consumption when no data received.
	 * @param consumeDelay the {@link Duration} to delay consumption when empty.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec consumeDelay(Duration consumeDelay) {
		this.target.setConsumeDelay(consumeDelay);
		return this;
	}

	/**
	 * Provide an {@link InboundMessageMapper} to convert a consumed data into a message to produce.
	 * @param messageMapper the {@link InboundMessageMapper} to use.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec messageMapper(InboundMessageMapper<byte[]> messageMapper) {
		this.target.setMessageMapper(messageMapper);
		return this;
	}

	/**
	 * Provide a {@link MessageConverter} (as an alternative to {@link #messageMapper})
	 * for converting a consumed data into a message to produce.
	 * @param messageConverter the {@link MessageConverter} to use.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return this;
	}

	/**
	 * Whether raw {@link org.zeromq.ZMsg} is present as a payload of message to produce or
	 * it is fully converted to a {@link org.springframework.messaging.Message} including
	 * {@link org.springframework.integration.zeromq.ZeroMqHeaders#TOPIC} header (if any).
	 * @param receiveRaw to convert from {@link org.zeromq.ZMsg} or not; defaults to convert.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec receiveRaw(boolean receiveRaw) {
		this.target.setReceiveRaw(receiveRaw);
		return this;
	}

	/**
	 * Provide a {@link Consumer} to configure a socket with arbitrary options, like security.
	 * @param socketConfigurer the configurer for socket options.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec socketConfigurer(Consumer<ZMQ.Socket> socketConfigurer) {
		this.target.setSocketConfigurer(socketConfigurer);
		return this;
	}

	/**
	 * Specify topics the {@link SocketType#SUB} socket is going to use for subscription.
	 * It is ignored for all other {@link SocketType}s supported.
	 * @param topics the topics to use.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec topics(String... topics) {
		this.target.setTopics(topics);
		return this;
	}

	/**
	 * Specify if the topic
	 * that {@link SocketType#SUB} socket is going to receive is wrapped with an additional empty frame.
	 * It is ignored for all other {@link SocketType}s supported.
	 * This attribute is set to {@code true} by default.
	 * @param unwrapTopic true if the received topic is wrapped with an additional empty frame.
	 * @return the spec
	 * @since 6.2.6
	 */
	public ZeroMqMessageProducerSpec unwrapTopic(boolean unwrapTopic) {
		this.target.unwrapTopic(unwrapTopic);
		return this;
	}

	/**
	 * Configure an URL for {@link org.zeromq.ZMQ.Socket#connect(String)}.
	 * @param connectUrl the URL to connect ZeroMq socket to.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec connectUrl(String connectUrl) {
		this.target.setConnectUrl(connectUrl);
		return this;
	}

	/**
	 * Configure a port for TCP protocol binding via {@link org.zeromq.ZMQ.Socket#bind(String)}.
	 * @param port the port to bind ZeroMq socket to over TCP.
	 * @return the spec
	 */
	public ZeroMqMessageProducerSpec bindPort(int port) {
		this.target.setBindPort(port);
		return this;
	}

}
