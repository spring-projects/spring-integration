/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that maps a Message into
 * a UDP datagram packet and sends that to the specified host and port.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class DatagramPacketSendingHandler implements MessageHandler {

	private final SocketAddress socketAddress;

	private final OutboundMessageMapper<DatagramPacket> mapper = new DatagramPacketMessageMapper();


	public DatagramPacketSendingHandler(String host, int port) {
		Assert.notNull(host, "host must not be null");
		this.socketAddress = new InetSocketAddress(host, port);
	}


	public void handleMessage(Message<?> message) {
		try {
			DatagramPacket packet = this.mapper.fromMessage(message);
			this.send(packet);
		}
		catch (MessagingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, "failed to send UDP packet", e);
		}
	}

	private void send(DatagramPacket packet) throws Exception {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			packet.setSocketAddress(this.socketAddress);
			socket.send(packet);
		}
		finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

}
