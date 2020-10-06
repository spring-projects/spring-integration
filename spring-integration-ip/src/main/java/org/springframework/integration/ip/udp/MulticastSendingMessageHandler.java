/*
 * Copyright 2001-2020 the original author or authors.
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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URISyntaxException;

import org.springframework.expression.Expression;
import org.springframework.messaging.Message;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation that maps a
 * Message into a UDP datagram packet and sends that to the specified multicast address
 * (224.0.0.0 to 239.255.255.255) and port.
 *
 * The only difference between this and its super class is the ability to specify how many
 * acknowledgments are required to determine success.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MulticastSendingMessageHandler extends UnicastSendingMessageHandler {

	private int timeToLive = -1;

	private String localAddress;

	private volatile MulticastSocket multicastSocket;

	/**
	 * Constructs a MulticastSendingMessageHandler to send data to the multicast address/port.
	 * @param address The multicast address.
	 * @param port The port.
	 */
	public MulticastSendingMessageHandler(String address, int port) {
		super(address, port);
	}

	/**
	 * Constructs a MulticastSendingMessageHandler to send data to the multicast address/port
	 * and enables setting the lengthCheck option (if set, a length is prepended to the packet and checked
	 * at the destination).
	 * @param address The multicast address.
	 * @param port The port.
	 * @param lengthCheck Enable the lengthCheck option.
	 */
	public MulticastSendingMessageHandler(String address, int port, boolean lengthCheck) {
		super(address, port, lengthCheck);
	}


	/**
	 * Constructs a MulticastSendingMessageHandler to send data to the multicast address/port
	 * and enables setting the acknowledge option, where the destination sends a receipt acknowledgment.
	 * @param address The multicast address.
	 * @param port The port.
	 * @param acknowledge Whether or not acknowledgments are required.
	 * @param ackHost The host to which acknowledgments should be sent; required if acknowledge is true.
	 * @param ackPort The port to which acknowledgments should be sent; required if acknowledge is true.
	 * @param ackTimeout How long to wait (milliseconds) for an acknowledgment.
	 */
	public MulticastSendingMessageHandler(String address, int port,
			boolean acknowledge, String ackHost, int ackPort, int ackTimeout) {

		super(address, port, acknowledge, ackHost, ackPort, ackTimeout);
	}

	/**
	 * Constructs a MulticastSendingMessageHandler to send data to the multicast address/port
	 * and enables setting the acknowledge option, where the destination sends a receipt acknowledgment.
	 * @param address The multicast address.
	 * @param port The port.
	 * @param lengthCheck Enable the lengthCheck option.
	 * @param acknowledge Whether or not acknowledgments are required.
	 * @param ackHost The host to which acknowledgments should be sent; required if acknowledge is true.
	 * @param ackPort The port to which acknowledgments should be sent; required if acknowledge is true.
	 * @param ackTimeout How long to wait (milliseconds) for an acknowledgment.
	 */
	public MulticastSendingMessageHandler(String address, int port,
			boolean lengthCheck, boolean acknowledge, String ackHost,
			int ackPort, int ackTimeout) {

		super(address, port, lengthCheck, acknowledge, ackHost, ackPort, ackTimeout);
	}

	/**
	 * Construct MulticastSendingMessageHandler based on the destination SpEL expression to
	 * determine the target destination at runtime against requestMessage.
	 * @param destinationExpression the SpEL expression to evaluate the target destination
	 * at runtime. Must evaluate to {@link String}, {@link java.net.URI} or
	 * {@link java.net.SocketAddress}.
	 * @since 5.0
	 */
	public MulticastSendingMessageHandler(Expression destinationExpression) {
		super(destinationExpression);
	}

	/**
	 * Construct MulticastSendingMessageHandler based on the destination SpEL expression to
	 * determine the target destination at runtime against requestMessage.
	 * @param destinationExpression the SpEL expression to evaluate the target destination
	 * at runtime. Must evaluate to {@link String}, {@link java.net.URI} or
	 * {@link java.net.SocketAddress}.
	 * @since 5.0
	 */
	public MulticastSendingMessageHandler(String destinationExpression) {
		super(destinationExpression);
	}

	@Override
	protected synchronized DatagramSocket getSocket() throws IOException {
		if (getTheSocket() == null) {
			createSocket();
		}
		return super.getSocket();
	}

	private void createSocket() throws IOException {
		if (getTheSocket() == null) {
			MulticastSocket socket;
			if (isAcknowledge()) {
				int ackPort = getAckPort();
				if (this.localAddress == null) {
					socket = ackPort == 0 ? new MulticastSocket() : new MulticastSocket(ackPort);
				}
				else {
					InetAddress whichNic = InetAddress.getByName(this.localAddress);
					socket = new MulticastSocket(new InetSocketAddress(whichNic, ackPort));
				}
				int soReceiveBufferSize = getSoReceiveBufferSize();
				if (soReceiveBufferSize > 0) {
					socket.setReceiveBufferSize(soReceiveBufferSize);
				}
				logger.debug(() -> "Listening for acks on port: " + socket.getLocalPort());
				setSocket(socket);
				updateAckAddress();
			}
			else {
				socket = new MulticastSocket();
				setSocket(socket);
			}
			if (this.timeToLive >= 0) {
				socket.setTimeToLive(this.timeToLive);
			}
			setSocketAttributes(socket);
			if (this.localAddress != null) {
				socket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName(this.localAddress)));
			}
			this.multicastSocket = socket;
		}
	}


	/**
	 * If acknowledge = true; how many acks needed for success.
	 * @param minAcksForSuccess The minimum number of acks that will represent success.
	 */
	public void setMinAcksForSuccess(int minAcksForSuccess) {
		this.setAckCounter(minAcksForSuccess);
	}

	/**
	 * Set the underlying {@link MulticastSocket} time to live property.
	 * @param timeToLive {@link MulticastSocket#setTimeToLive(int)}
	 */
	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}

	@Override
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	@Override
	protected void convertAndSend(Message<?> message) throws IOException, URISyntaxException {
		super.convertAndSend(message);
		if (logger.isDebugEnabled()) {
			logger.debug("Sent packet to " + this.multicastSocket.getNetworkInterface());
		}
	}

}
