/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.net.DatagramSocket;
import java.util.function.Function;

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.ip.udp.SocketCustomizer;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} for UDP {@link org.springframework.messaging.MessageHandler}s.
 *
 * @param <S> the target {@link AbstractUdpOutboundChannelAdapterSpec} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public abstract class AbstractUdpOutboundChannelAdapterSpec<S extends AbstractUdpOutboundChannelAdapterSpec<S>>
		extends MessageHandlerSpec<S, UnicastSendingMessageHandler> {

	protected AbstractUdpOutboundChannelAdapterSpec() {
	}

	protected AbstractUdpOutboundChannelAdapterSpec(String host, int port) {
		this.target = new UnicastSendingMessageHandler(host, port);
	}

	protected AbstractUdpOutboundChannelAdapterSpec(String destinationExpression) {
		this.target = new UnicastSendingMessageHandler(destinationExpression);
	}

	protected AbstractUdpOutboundChannelAdapterSpec(Function<Message<?>, ?> destinationFunction) {
		this.target = new UnicastSendingMessageHandler(new FunctionExpression<>(destinationFunction));
	}

	/**
	 * @param timeout the timeout socket option.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSoTimeout(int)
	 */
	public S soTimeout(int timeout) {
		this.target.setSoTimeout(timeout);
		return _this();
	}

	/**
	 * @param size the send buffer size socket option.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSoSendBufferSize(int)
	 */
	public S soSendBufferSize(int size) {
		this.target.setSoSendBufferSize(size);
		return _this();
	}

	/**
	 * @param localAddress the local address.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setLocalAddress(String)
	 */
	public S localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return _this();
	}

	/**
	 * @param lengthCheck the length check boolean.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setLengthCheck(boolean)
	 */
	public S lengthCheck(boolean lengthCheck) {
		this.target.setLengthCheck(lengthCheck);
		return _this();
	}

	/**
	 * @param size the receive buffer size socket option.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSoReceiveBufferSize(int)
	 */
	public S soReceiveBufferSize(int size) {
		this.target.setSoReceiveBufferSize(size);
		return _this();
	}

	/**
	 * @param ackCounter the ack counter.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setAckCounter(int)
	 */
	public S ackCounter(int ackCounter) {
		this.target.setAckCounter(ackCounter);
		return _this();
	}

	/**
	 * @param socketFunction the socket function.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSocketExpression(org.springframework.expression.Expression)
	 */
	public S socketFunction(Function<Message<?>, DatagramSocket> socketFunction) {
		this.target.setSocketExpression(new FunctionExpression<>(socketFunction));
		return _this();
	}

	/**
	 * @param socketExpression the socket expression.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSocketExpressionString(String)
	 */
	public S socketExpression(String socketExpression) {
		this.target.setSocketExpressionString(socketExpression);
		return _this();
	}

	/**
	 * Configure the socket.
	 * @param customizer the customizer.
	 * @return the spec.
	 * @since 5.3.3
	 */
	public S configureSocket(SocketCustomizer customizer) {
		this.target.setSocketCustomizer(customizer);
		return _this();
	}

}
