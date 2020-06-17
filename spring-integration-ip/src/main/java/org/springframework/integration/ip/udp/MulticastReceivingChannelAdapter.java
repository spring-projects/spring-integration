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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import org.springframework.messaging.MessagingException;

/**
 * Channel adapter that joins a multicast group and receives incoming packets and
 * sends them to an output channel.
 *
 * @author Gary Russell
 * @author Marcin Pilaczynski
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MulticastReceivingChannelAdapter extends UnicastReceivingChannelAdapter {

	private final String group;


	/**
	 * Constructs a MulticastReceivingChannelAdapter that listens for packets on the
	 * specified multichannel address (group) and port.
	 * @param group The multichannel address.
	 * @param port The port.
	 */
	public MulticastReceivingChannelAdapter(String group, int port) {
		super(port);
		this.group = group;
	}

	/**
	 * Constructs a MulticastReceivingChannelAdapter that listens for packets on the
	 * specified multichannel address (group) and port. Enables setting the lengthCheck
	 * option, which expects a length to precede the incoming packets.
	 * @param group The multichannel address.
	 * @param port The port.
	 * @param lengthCheck If true, enables the lengthCheck Option.
	 */
	public MulticastReceivingChannelAdapter(String group, int port, boolean lengthCheck) {
		super(port, lengthCheck);
		this.group = group;
	}

	@Override
	public synchronized DatagramSocket getSocket() {
		if (getTheSocket() == null) {
			try {
				int port = getPort();
				MulticastSocket socket = port == 0 ? new MulticastSocket() : new MulticastSocket(port);
				String localAddress = getLocalAddress();
				if (localAddress != null) {
					socket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName(localAddress)));
				}
				setSocketAttributes(socket);
				socket.joinGroup(new InetSocketAddress(this.group, 0), null);
				setSocket(socket);
			}
			catch (IOException e) {
				throw new MessagingException("failed to create DatagramSocket", e);
			}
		}
		return super.getSocket();
	}

}
