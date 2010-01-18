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

import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketException;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class UdpSocket {

	private DatagramSocket datagramSocket;

	private MulticastSocket multicastSocket;


	/**
	 * Creates a Unicast UdpSocket on the specified port.
	 * @param port The port.
	 */
	public UdpSocket(int port) {
		// TODO
	}

	/**
	 * Creates a multicast UdpSocket on the specified port which will join
	 * the specified group (multicast ip address).
	 * @param group The group (multicast ip address) to join.
	 * @param port The port.
	 */
	public UdpSocket(String group, int port) {
		// TODO
	}


	public void setReceiveBufferSize(int size) throws SocketException {
		if (datagramSocket != null) {
			datagramSocket.setReceiveBufferSize(size);
		}
		if (multicastSocket != null) {
			multicastSocket.setReceiveBufferSize(size);
		}
	}

	public void setSoTimeout(int timeout) throws SocketException {
		if (datagramSocket != null) {
			datagramSocket.setSoTimeout(timeout);
		}
		if (multicastSocket != null) {
			multicastSocket.setSoTimeout(timeout);
		}
	}

	public void close() {
		if (datagramSocket != null) {
			datagramSocket.close();
		}
		if (multicastSocket != null) {
			multicastSocket.close();
		}
	}

}
