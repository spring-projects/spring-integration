/*
 * Copyright 2002-2020 the original author or authors.
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A channel adapter to receive incoming UDP packets. Packets can optionally be preceded by a
 * 4 byte length field, used to validate that all data was received. Packets may also contain
 * information indicating an acknowledgment needs to be sent.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class UnicastReceivingChannelAdapter extends AbstractInternetProtocolReceivingChannelAdapter {

	private static final Pattern ADDRESS_PATTERN = Pattern.compile("([^:]*):([0-9]*)");

	private final DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();

	private DatagramSocket socket;

	private boolean socketExplicitlySet;

	private int soSendBufferSize = -1;

	private SocketCustomizer socketCustomizer = socket -> { };

	/**
	 * Constructs a UnicastReceivingChannelAdapter that listens on the specified port.
	 * @param port The port.
	 */
	public UnicastReceivingChannelAdapter(int port) {
		super(port);
		this.mapper.setLengthCheck(false);
	}

	/**
	 * Constructs a UnicastReceivingChannelAdapter that listens for packets on
	 * the specified port. Enables setting the lengthCheck option, which expects
	 * a length to precede the incoming packets.
	 * @param port The port.
	 * @param lengthCheck If true, enables the lengthCheck Option.
	 */
	public UnicastReceivingChannelAdapter(int port, boolean lengthCheck) {
		super(port);
		this.mapper.setLengthCheck(lengthCheck);
	}

	/**
	 * @param lengthCheck if true, the incoming packet is expected to have a four
	 * byte binary length header.
	 * @since 5.0
	 */
	public void setLengthCheck(boolean lengthCheck) {
		this.mapper.setLengthCheck(lengthCheck);
	}

	/**
	 * Set a customizer to further configure the socket after creation.
	 * @param socketCustomizer the customizer.
	 * @since 5.3.3
	 */
	public void setSocketCustomizer(SocketCustomizer socketCustomizer) {
		Assert.notNull(socketCustomizer, "'socketCustomizer' cannot be null");
		this.socketCustomizer = socketCustomizer;
	}

	@Override
	public boolean isLongLived() {
		return true;
	}

	@Override
	public int getPort() {
		if (this.socket == null) {
			return super.getPort();
		}
		else {
			return this.socket.getLocalPort();
		}
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.mapper.setBeanFactory(getBeanFactory());
	}

	@Override
	public void run() {
		getSocket();

		ApplicationEventPublisher publisher = getApplicationEventPublisher();
		if (publisher != null) {
			publisher.publishEvent(new UdpServerListeningEvent(this, getPort()));
		}

		logger.debug(() -> "UDP Receiver running on port: " + getPort());

		setListening(true);

		// Do as little as possible here so we can loop around and catch the next packet.
		// Just schedule the packet for processing.
		while (isActive()) {
			try {
				asyncSendMessage(receive());
			}
			catch (SocketTimeoutException ex) {
				// continue
			}
			catch (SocketException ex) {
				stop();
			}
			catch (Exception ex) {
				if (ex instanceof MessagingException) { // NOSONAR flow control via exceptions
					throw (MessagingException) ex;
				}
				throw new MessagingException("failed to receive DatagramPacket", ex);
			}
		}
		setListening(false);
	}

	protected void sendAck(Message<byte[]> message) {
		MessageHeaders headers = message.getHeaders();
		Object id = headers.get(IpHeaders.ACK_ID);
		if (id == null) {
			logger.error(() -> "No " + IpHeaders.ACK_ID + " header; cannot send ack");
			return;
		}
		byte[] ack = id.toString().getBytes();
		String ackAddress = (headers.get(IpHeaders.ACK_ADDRESS, String.class)).trim(); // NOSONAR caller checks header
		Matcher mat = ADDRESS_PATTERN.matcher(ackAddress);
		if (!mat.matches()) {
			throw new MessagingException(message,
					"Ack requested but could not decode acknowledgment address: " + ackAddress);
		}
		String host = mat.group(1);
		int port = Integer.parseInt(mat.group(2));
		InetSocketAddress whereTo = new InetSocketAddress(host, port);
		logger.debug(() -> "Sending ack for " + id + " to " + ackAddress);
		try {
			DatagramPacket ackPack = new DatagramPacket(ack, ack.length, whereTo);
			DatagramSocket out = new DatagramSocket();
			if (this.soSendBufferSize > 0) {
				out.setSendBufferSize(this.soSendBufferSize);
			}
			this.socketCustomizer.configure(out);
			out.send(ackPack);
			out.close();
		}
		catch (IOException ex) {
			throw new MessagingException(message, "Failed to send acknowledgment to: " + ackAddress, ex);
		}
	}

	protected boolean asyncSendMessage(DatagramPacket packet) {
		Executor taskExecutor = getTaskExecutor();
		if (taskExecutor != null) {
			try {
				taskExecutor.execute(() -> doSend(packet));
			}
			catch (RejectedExecutionException e) {
				logger.debug("Adapter stopped, sending on main thread");
				doSend(packet);
			}
		}
		return true;
	}

	protected void doSend(final DatagramPacket packet) {
		Message<byte[]> message = null;
		try {
			message = this.mapper.toMessage(packet);
			Message<byte[]> messageToLog = message;
			logger.debug(() -> "Received: " + messageToLog);
		}
		catch (Exception ex) {
			logger.error(ex, "Failed to map packet to message ");
		}
		if (message != null) {
			if (message.getHeaders().containsKey(IpHeaders.ACK_ADDRESS)) {
				sendAck(message);
			}
			try {
				sendMessage(message);
			}
			catch (Exception ex) {
				this.logger.error(ex, "Failed to send message " + message);
			}
		}
	}

	protected DatagramPacket receive() throws IOException {
		final byte[] buffer = new byte[getReceiveBufferSize()];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		getSocket().receive(packet);
		return packet;
	}

	/**
	 * @param socket the socket to set
	 */
	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
		this.socketExplicitlySet = true;
	}

	@Nullable
	protected DatagramSocket getTheSocket() {
		return this.socket;
	}

	public synchronized DatagramSocket getSocket() {
		if (this.socket == null) {
			try {
				DatagramSocket datagramSocket;
				String localAddress = getLocalAddress();
				int port = super.getPort();
				if (localAddress == null) {
					datagramSocket = port == 0 ? new DatagramSocket() : new DatagramSocket(port);
				}
				else {
					InetAddress whichNic = InetAddress.getByName(localAddress);
					datagramSocket = new DatagramSocket(new InetSocketAddress(whichNic, port));
				}
				setSocketAttributes(datagramSocket);
				this.socket = datagramSocket;
			}
			catch (IOException e) {
				throw new MessagingException("failed to create DatagramSocket", e);
			}
		}
		return this.socket;
	}

	/**
	 * Sets timeout and receive buffer size; calls the socket customizer.
	 *
	 * @param socket The socket.
	 * @throws SocketException Any socket exception.
	 */
	protected void setSocketAttributes(DatagramSocket socket) throws SocketException {
		socket.setSoTimeout(getSoTimeout());
		int soReceiveBufferSize = getSoReceiveBufferSize();
		if (soReceiveBufferSize > 0) {
			socket.setReceiveBufferSize(soReceiveBufferSize);
		}
		this.socketCustomizer.configure(socket);
	}

	@Override
	protected void doStop() {
		super.doStop();
		try {
			DatagramSocket datagramSocket = this.socket;
			if (!this.socketExplicitlySet) {
				this.socket = null;
			}
			datagramSocket.close();
		}
		catch (Exception e) {
			// ignore
		}
	}

	@Override
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	public void setLookupHost(boolean lookupHost) {
		this.mapper.setLookupHost(lookupHost);
	}

	@Override
	public String getComponentType() {
		return "ip:udp-inbound-channel-adapter";
	}

}
