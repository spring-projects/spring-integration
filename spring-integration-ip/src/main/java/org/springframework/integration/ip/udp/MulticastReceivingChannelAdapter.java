/*
 * Copyright 2002-2011 the original author or authors.
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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.springframework.messaging.MessagingException;

/**
 * Channel adapter that joins a multicast group and receives incoming packets and
 * sends them to an output channel.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class MulticastReceivingChannelAdapter extends UnicastReceivingChannelAdapter {

	private String group;


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
	protected synchronized DatagramSocket getSocket() {
		if (this.getTheSocket() == null) {
			try {
				MulticastSocket socket = new MulticastSocket(this.getPort());
				String localAddress = this.getLocalAddress();
				if (localAddress != null) {
					InetAddress whichNic = InetAddress.getByName(localAddress);
					socket.setInterface(whichNic);
				}
				this.setSocketAttributes(socket);
				socket.joinGroup(InetAddress.getByName(this.group));
				this.setSocket(socket);
			}
			catch (IOException e) {
				throw new MessagingException("failed to create DatagramSocket", e);
			}
		}
		return super.getSocket();
	}

}
