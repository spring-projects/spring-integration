/*
 * Copyright 2001-present the original author or authors.
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

package org.springframework.integration.ip.udp;

import java.net.SocketAddress;
import java.net.URI;

import org.springframework.expression.Expression;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation that maps a Message into
 * a UDP datagram packet and sends that to the specified host and port.
 * <p>
 * Messages can be basic, with no support for reliability, can be prefixed
 * by a length so the receiving end can detect truncation, and can require
 * a UDP acknowledgment to confirm delivery.
 *
 * @author Gary Russell
 * @author Marcin Pilaczynski
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Ngoc Nhan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.udp.outbound.UnicastSendingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class UnicastSendingMessageHandler
		extends org.springframework.integration.ip.udp.outbound.UnicastSendingMessageHandler {

	/**
	 * Basic constructor; no reliability; no acknowledgment.
	 * @param host Destination host.
	 * @param port Destination port.
	 */
	public UnicastSendingMessageHandler(String host, int port) {
		super(host, port);
	}

	/**
	 * Construct UnicastSendingMessageHandler based on the destination SpEL expression to
	 * determine the target destination at runtime against requestMessage.
	 * @param destinationExpression the SpEL expression to evaluate the target destination
	 * at runtime. Must evaluate to {@link String}, {@link URI} or {@link SocketAddress}.
	 * @since 4.3
	 */
	public UnicastSendingMessageHandler(String destinationExpression) {
		super(destinationExpression);
	}

	/**
	 * Construct UnicastSendingMessageHandler based on the destination SpEL expression to
	 * determine the target destination at runtime against requestMessage.
	 * @param destinationExpression the SpEL expression to evaluate the target destination
	 * at runtime. Must evaluate to {@link String}, {@link URI} or {@link SocketAddress}.
	 * @since 4.3
	 */
	public UnicastSendingMessageHandler(Expression destinationExpression) {
		super(destinationExpression);
	}

	/**
	 * Can be used to add a length to each packet which can be checked at the destination.
	 * @param host Destination Host.
	 * @param port Destination Port.
	 * @param lengthCheck If true, packets will contain a length.
	 */
	public UnicastSendingMessageHandler(String host, int port, boolean lengthCheck) {
		super(host, port, lengthCheck);
	}

	/**
	 * Add an acknowledgment request to packets.
	 * @param host Destination Host.
	 * @param port Destination Port.
	 * @param acknowledge If true, packets will request acknowledgment.
	 * @param ackHost The host to which acks should be sent. Required if ack true.
	 * @param ackPort The port to which acks should be sent.
	 * @param ackTimeout How long we will wait (milliseconds) for the ack.
	 */
	public UnicastSendingMessageHandler(String host,
			int port,
			boolean acknowledge,
			String ackHost,
			int ackPort,
			int ackTimeout) {

		super(host, port, acknowledge, ackHost, ackPort, ackTimeout);
	}

	/**
	 * Add a length and/or acknowledgment request to packets.
	 * @param host Destination Host.
	 * @param port Destination Port.
	 * @param lengthCheck If true, packets will contain a length.
	 * @param acknowledge If true, packets will request acknowledgment.
	 * @param ackHost The host to which acks should be sent. Required if ack true.
	 * @param ackPort The port to which acks should be sent.
	 * @param ackTimeout How long we will wait (milliseconds) for the ack.
	 */
	public UnicastSendingMessageHandler(String host,
			int port,
			boolean lengthCheck,
			boolean acknowledge,
			String ackHost,
			int ackPort,
			int ackTimeout) {

		super(host, port, lengthCheck, acknowledge, ackHost, ackPort, ackTimeout);
	}

}
