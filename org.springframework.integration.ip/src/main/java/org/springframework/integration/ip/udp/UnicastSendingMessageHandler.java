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
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that maps a Message into
 * a UDP datagram packet and sends that to the specified host and port.
 * 
 * Messages can be basic, with no support for reliability, can be prefixed
 * by a length so the receiving end can detect truncation, and can require 
 * a UDP acknowledgment to confirm delivery.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class UnicastSendingMessageHandler extends
		AbstractInternetProtocolSendingMessageHandler implements Runnable {

	protected final DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();

	protected volatile DatagramSocket socket;


	/**
	 * If true adds headers to instruct receiving adapter to return an ack.
	 */
	protected volatile boolean waitForAck = false;

	protected volatile int ackPort;

	protected volatile int ackTimeout = 5000;

	protected volatile int ackCounter = 1;

	protected volatile Map<String, CountDownLatch> ackControl = Collections
			.synchronizedMap(new HashMap<String, CountDownLatch>());

	protected volatile DatagramSocket ackSocket;

	protected volatile Exception fatalException;

	protected int soReceiveBufferSize = -1;


	/**
	 * Basic constructor; no reliability; no acknowledgment.
	 * @param host Destination host.
	 * @param port Destination port.
	 */
	public UnicastSendingMessageHandler(String host, int port) {
		super(host, port);
		this.mapper.setLengthCheck(false);
		this.mapper.setAcknowledge(false);
	}

	/**
	 * Can used to add a length to each packet which can be checked at the destination.
	 * @param host Destination Host.
	 * @param port Destination Port.
	 * @param lengthCheck If true, packets will contain a length.
	 */
	public UnicastSendingMessageHandler(String host, int port, boolean lengthCheck) {
		super(host, port);
		this.mapper.setLengthCheck(lengthCheck);
		this.mapper.setAcknowledge(false);
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
		super(host, port);
		setReliabilityAttributes(false, acknowledge, ackHost, ackPort,
				ackTimeout);
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
		super(host, port);
		setReliabilityAttributes(lengthCheck, acknowledge, ackHost, ackPort,
				ackTimeout);
	}

	protected void setReliabilityAttributes(boolean lengthCheck,
			boolean acknowledge, String ackHost, int ackPort, int ackTimeout) {
		this.mapper.setLengthCheck(lengthCheck);
		this.waitForAck = acknowledge;
		this.mapper.setAcknowledge(acknowledge);
		this.mapper.setAckAddress(ackHost + ":" + ackPort);
		this.ackPort = ackPort;
		if (ackTimeout > 0) {
			this.ackTimeout = ackTimeout;
		}
		if (acknowledge) {
			Assert.hasLength(ackHost);
			this.executorService = Executors
					.newSingleThreadExecutor(new ThreadFactory() {
						private AtomicInteger n = new AtomicInteger(); 
						public Thread newThread(Runnable runner) {
							Thread thread = new Thread(runner);
							thread.setName("UDP-Ack-Handler-" + n.getAndIncrement());
							thread.setDaemon(true);
							return thread;
						}
					});
			this.executorService.execute(this);
		}
	}

	public void handleMessage(Message<?> message)
			throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		CountDownLatch countdownLatch = null;
		String messageId = message.getHeaders().getId().toString();
		try {
			DatagramPacket packet;
			if (this.waitForAck) {
				if (this.fatalException != null) {
					throw new MessagingException(message, "Acknowledgment failure", fatalException);
				}
				countdownLatch = new CountDownLatch(ackCounter);
				this.ackControl.put(messageId, countdownLatch);
			}
			packet = this.mapper.fromMessage(message);
			this.send(packet);
			logger.debug("Sent packet for message id " + message.getHeaders().getId());
			if (this.waitForAck) {
				if (!countdownLatch.await(this.ackTimeout, TimeUnit.MILLISECONDS)) {
					throw new MessagingException(message, "Failed to receive UDP Ack in " + ackTimeout + " millis");
				}
			}
		}
		catch (MessagingException e) {
			throw e;
		}
		catch (Exception e) {
			try{
				socket.close();
			}
			catch (Exception e1) { }
			socket = null;
			throw new MessageHandlingException(message, "failed to send UDP packet", e);
		} 
		finally {
			if (countdownLatch != null)
				this.ackControl.remove(messageId);
		}
	}

	protected void send(DatagramPacket packet) throws Exception {
		DatagramSocket socket = this.getSocket();
		packet.setSocketAddress(this.destinationAddress);
		socket.send(packet);
	}

	protected synchronized DatagramSocket getSocket() throws IOException {
		if (this.socket == null) {
			this.socket = new DatagramSocket();
			setSocketAttributes(this.socket);
		}
		return this.socket;
	}

	protected void setSocketAttributes(DatagramSocket socket) throws SocketException {
		if (this.soTimeout >= 0) {
			socket.setSoTimeout(this.soTimeout);
		}
		if (this.soSendBufferSize > 0) {
			socket.setSendBufferSize(this.soSendBufferSize);
		}
	}

	/**
	 * Process acknowledgments, if requested.
	 */
	public void run() {
		Exception fatalException = null;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Listening for acks on port: " + ackPort);
			}
			this.ackSocket = new DatagramSocket(this.ackPort);
			if (this.soReceiveBufferSize > 0) {
				ackSocket.setReceiveBufferSize(this.soReceiveBufferSize);
			}
			DatagramPacket ackPack = new DatagramPacket(new byte[100], 100);
			while(true) {
				this.ackSocket.receive(ackPack);
				String id = new String(ackPack.getData(), ackPack.getOffset(), ackPack.getLength());
				if (logger.isDebugEnabled()) {
					logger.debug("Received ack for " + id + " from " + ackPack.getAddress().getHostAddress());
				}
				CountDownLatch latch = this.ackControl.get(id);
				if (latch != null) {
					latch.countDown();
				}
			}
		}
		catch (IOException e) {
			logger.error("Error on UDP Acknowledge thread" + e.getMessage());
			fatalException = e;
		}
		finally {
			if (this.ackSocket != null) {
				this.ackSocket.close();
			}
			if (fatalException instanceof BindException) {
				logger.fatal("Failed to bind to acknowledge port: " + ackPort);
				this.fatalException = fatalException;
			}
			else {
				this.executorService.execute(this);
			}
		}
	}

	/**
	 * If exposed as an MBean, can be used to restart the ack thread if a fatal 
	 * (bind) error occurred, without bouncing the JVM.
	 */
	public void restartAckThread() {
		if (fatalException == null) {
			return;
		}
		this.fatalException = null;
		this.executorService.execute(this);
	}

	public void shutDown() {
		DatagramSocket socket = this.ackSocket;
		this.ackSocket = null;
		if (socket != null) {
			socket.close();
		}
	}

	/**
	 * @see {@link Socket#setReceiveBufferSize(int)} and {@link DatagramSocket#setReceiveBufferSize(int)}
	 * @param size
	 */
	public void setSoReceiveBufferSize(int size) {
		this.soReceiveBufferSize = size;
	}

}
