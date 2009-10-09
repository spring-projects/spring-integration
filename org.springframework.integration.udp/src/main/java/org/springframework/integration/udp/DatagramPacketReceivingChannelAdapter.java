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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Channel Adapter that receives UDP datagram packets and maps them to Messages.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class DatagramPacketReceivingChannelAdapter extends MessageProducerSupport implements Runnable {

	private final int port;

	private volatile int timeout = 60 * 1000;

	private volatile int receiveBufferSize = 64 * 1024;

	private volatile boolean active;

	private volatile DatagramSocket socket;

	private final DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();


	public DatagramPacketReceivingChannelAdapter(int port) {
		this.port = port;
	}


	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
		if (this.socket != null) {
			try {
				this.socket.setSoTimeout(timeout);
			}
			catch (SocketException e) {
				throw new IllegalStateException("failed to set socket timeout", e);
			}
		}
	}

	@Override
	protected void doStart() {
		TaskScheduler taskScheduler = this.getTaskScheduler();
		Assert.state(taskScheduler != null, "taskScheduler is required");
		this.active = true;
		taskScheduler.schedule(this, new Date());
	}

	@Override
	protected void doStop() {
		this.active = false;
		try {
			this.socket.close();
		}
		catch (Exception e) {
			// ignore
		}
	}

	public void run() {
		while (this.active) {
			try {
				Message<byte[]> message = receive();
				if (message != null) {
					this.sendMessage(message);
				}
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
	}

	public Message<byte[]> receive() throws Exception {
		DatagramSocket socket = this.getSocket();
		final byte[] buffer = new byte[this.receiveBufferSize];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		return this.mapper.toMessage(packet);
	}

	private synchronized DatagramSocket getSocket() {
		if (this.socket == null) {
			try {
				this.socket = new DatagramSocket(this.port);
				this.socket.setSoTimeout(this.timeout);
			}
			catch (SocketException e) {
				throw new MessagingException("failed to create DatagramSocket", e);
			}
		}
		return this.socket;
	}

}
