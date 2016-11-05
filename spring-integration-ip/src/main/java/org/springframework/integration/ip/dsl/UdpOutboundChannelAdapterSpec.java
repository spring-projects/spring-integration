/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.dsl;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;

/**
 * A {@link MessageHandlerSpec} for {@link UnicastSendingMessageHandler}s.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class UdpOutboundChannelAdapterSpec
		extends MessageHandlerSpec<UdpOutboundChannelAdapterSpec, UnicastSendingMessageHandler> {

	protected UdpOutboundChannelAdapterSpec() {
		super();
	}

	UdpOutboundChannelAdapterSpec(String destinationExpression) {
		this.target = new UnicastSendingMessageHandler(destinationExpression);
	}

	/**
	 * @param timeout the timeout socket option.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSoTimeout(int)
	 */
	public UdpOutboundChannelAdapterSpec soTimeout(int timeout) {
		this.target.setSoTimeout(timeout);
		return _this();
	}

	/**
	 * @param size the send buffer size socket option.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSoSendBufferSize(int)
	 */
	public UdpOutboundChannelAdapterSpec soSendBufferSize(int size) {
		this.target.setSoSendBufferSize(size);
		return _this();
	}

	/**
	 * @param localAddress the local address.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setLocalAddress(String)
	 */
	public UdpOutboundChannelAdapterSpec localAddress(String localAddress) {
		this.target.setLocalAddress(localAddress);
		return _this();
	}

	/**
	 * @param lengthCheck the length check boolean.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setLengthCheck(boolean)
	 */
	public UdpOutboundChannelAdapterSpec lengthCheck(boolean lengthCheck) {
		this.target.setLengthCheck(lengthCheck);
		return _this();
	}

	/**
	 * @param size the receive buffer size socket option.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSoReceiveBufferSize(int)
	 */
	public UdpOutboundChannelAdapterSpec soReceiveBufferSize(int size) {
		this.target.setSoReceiveBufferSize(size);
		return _this();
	}

	/**
	 * @param ackCounter the ack counter.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setAckCounter(int)
	 */
	public UdpOutboundChannelAdapterSpec ackCounter(int ackCounter) {
		this.target.setAckCounter(ackCounter);
		return _this();
	}

	/**
	 * @param socketExpression the socket expression.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSocketExpression(Expression)
	 */
	public UdpOutboundChannelAdapterSpec SocketExpression(Expression socketExpression) {
		this.target.setSocketExpression(socketExpression);
		return _this();
	}

	/**
	 * @param socketExpression the socket expression.
	 * @return the spec.
	 * @see UnicastSendingMessageHandler#setSocketExpressionString(String)
	 */
	public UdpOutboundChannelAdapterSpec SocketExpression(String socketExpression) {
		this.target.setSocketExpressionString(socketExpression);
		return _this();
	}

}
