/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.ip.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter;
import org.springframework.integration.ip.IpHeaders;

/**
 * A channel adapter to receive incoming UDP packets. Packets can optionally be preceded by a 
 * 4 byte length field, used to validate that all data was received. Packets may also contain
 * information indicating an acknowledgment needs to be sent.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class UnicastReceivingChannelAdapter extends AbstractInternetProtocolReceivingChannelAdapter {

	protected volatile DatagramSocket socket;

	protected final DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();

	protected volatile int soSendBufferSize = -1;

	private static Pattern addressPattern = Pattern.compile("([^:]*):([0-9]*)");


	/**
	 * Constructs a UnicastReceivingChannelAdapter that listens on the specified port.
	 * @param port
	 */
	public UnicastReceivingChannelAdapter(int port) {
		super(port);
		mapper.setLengthCheck(false);
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
		mapper.setLengthCheck(lengthCheck);
	}


	public void run() {
		if (logger.isDebugEnabled()) {
			logger.debug("UDP Receiver running on port:" + port);
		}
		checkTaskExecutor("UDP-Incoming-Msg-Handler");

		listening = true;
		
		// Do as little as possible here so we can loop around and catch the next packet.
		// Just schedule the packet for processing.
		while (this.active) {
			try {
				asyncSendMessage(receive());
			}
			catch (SocketTimeoutException e) {
				// continue
			}
			catch (SocketException e) {
				doStop(); 
			}
			catch (Exception e) {
				if (e instanceof MessagingException) {
					throw (MessagingException) e;
				}
				throw new MessagingException("failed to receive DatagramPacket", e);
			}
		}
		listening = false;
	}

	protected void sendAck(Message<byte[]> message) {
		MessageHeaders headers = message.getHeaders();
		Object id = headers.get(IpHeaders.ACK_ID);
		byte[] ack = id.toString().getBytes();
		String ackAddress = ((String) headers.get(IpHeaders.ACK_ADDRESS)).trim();
		Matcher mat = addressPattern.matcher(ackAddress);
		if (!mat.matches()) {
			throw new MessagingException(message, "Ack requested but could not decode acknowledgment address:" + ackAddress);
		}
		String host = mat.group(1);
		int port = Integer.parseInt(mat.group(2));
		InetSocketAddress whereTo = new InetSocketAddress(host, port);
		if (logger.isDebugEnabled()) {
			logger.debug("Sending ack for " + id + " to " + ackAddress);
		}
		try {
			DatagramPacket ackPack = new DatagramPacket(ack, ack.length, whereTo);
			DatagramSocket out = new DatagramSocket();
			if (this.soSendBufferSize > 0) {
				out.setSendBufferSize(this.soSendBufferSize);
			}
			out.send(ackPack);
			out.close();
		}
		catch (IOException e) {
			throw new MessagingException(message, "Failed to send acknowledgment", e);
		}
	}

	protected boolean asyncSendMessage(final DatagramPacket packet) {
		this.taskExecutor.execute(new Runnable(){
			public void run() {
				Message<byte[]> message = null;
				try {
					message = mapper.toMessage(packet);
					if (logger.isDebugEnabled())
						logger.debug("Received:" + message);
				}
				catch (Exception e) {
					logger.error("Failed to map packet to message ", e);
				}
				if (message != null) {
					if (message.getHeaders().containsKey(IpHeaders.ACK_ADDRESS)) {
						sendAck(message);
					}
					sendMessage(message);
				}
			}});
		return true;
	}

	protected DatagramPacket receive() throws Exception {
		DatagramSocket socket = this.getSocket();
		final byte[] buffer = new byte[this.receiveBufferSize];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		return packet;
	}

	protected synchronized DatagramSocket getSocket() {
		if (this.socket == null) {
			try {
				if (localAddress == null) {
					this.socket = new DatagramSocket(this.port);
				} else {
					InetAddress whichNic = InetAddress.getByName(this.localAddress);
					this.socket = new DatagramSocket(this.port, whichNic);
				}
				
				this.socket.setSoTimeout(this.soTimeout);
				if (this.soReceiveBufferSize > 0) {
					this.socket.setReceiveBufferSize(this.soReceiveBufferSize);
				}
			}
			catch (IOException e) {
				throw new MessagingException("failed to create DatagramSocket", e);
			}
		}
		return this.socket;
	}

	@Override
	protected void doStop() {
		super.doStop();
		try {
			this.socket.close();
			this.socket = null;
		}
		catch (Exception e) {
			// ignore
		}
	}

	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}
	
	public void setLookupHost(boolean lookupHost) {
		this.mapper.setLookupHost(lookupHost);
	}
	
	public String getComponentType(){
		return "ip:udp-inbound-channel-adapter";
	}
}
