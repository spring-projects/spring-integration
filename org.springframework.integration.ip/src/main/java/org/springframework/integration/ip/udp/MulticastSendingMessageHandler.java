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
import java.net.DatagramSocket;
import java.net.MulticastSocket;

import org.springframework.integration.message.MessageHandler;

/**
 * A {@link MessageHandler} implementation that maps a Message into
 * a UDP datagram packet and sends that to the specified multicast address
 * (224.0.0.0 to 239.255.255.255) and port.
 * 
 * The only difference between this and its super class is the
 * ability to specify how many acknowledgments are required to
 * determine success.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class MulticastSendingMessageHandler extends UnicastSendingMessageHandler {

	protected int timeToLive = -1;


	public MulticastSendingMessageHandler(String address, int port) {
		super(address, port);
	}

	public MulticastSendingMessageHandler(String address, int port, boolean lengthCheck) {
		super(address, port, lengthCheck);
	}

	public MulticastSendingMessageHandler(String address, int port,
			boolean lengthCheck, boolean acknowledge, String ackHost,
			int ackPort, int ackTimeout) {
		super(address, port, lengthCheck, acknowledge, ackHost, ackPort, ackTimeout);
	}


	/**
	 * If acknowledge = true; how many acks needed for success.
	 * @param minAcksForSuccess
	 */
	public void setMinAcksForSuccess(int minAcksForSuccess) {
		this.ackCounter = minAcksForSuccess;
	}

	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}

	protected DatagramSocket getSocket() throws IOException {
		if (this.socket == null) {
			MulticastSocket socket = new MulticastSocket();
			if (this.timeToLive >= 0) {
				socket.setTimeToLive(this.timeToLive);
			}
			socket.setLoopbackMode(true); // disable loopback to the local port
			setSocketAttributes(socket);
			this.socket = socket;
		}
		return this.socket;
	}

}
