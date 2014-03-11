/*
 * Copyright 2001-2013 the original author or authors.
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
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation that maps a Message into
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

	private final DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();

	private volatile DatagramSocket socket;


	/**
	 * If true adds headers to instruct receiving adapter to return an ack.
	 */
	private volatile boolean waitForAck = false;

	private volatile boolean acknowledge = false;

	private volatile int ackPort;

	private volatile int ackTimeout = 5000;

	private volatile int ackCounter = 1;

	private volatile Map<String, CountDownLatch> ackControl = Collections
			.synchronizedMap(new HashMap<String, CountDownLatch>());

	private volatile Exception fatalException;

	private volatile int soReceiveBufferSize = -1;

	private volatile String localAddress;

	private volatile CountDownLatch ackLatch;

	private volatile boolean ackThreadRunning;

	private volatile Executor taskExecutor;

	private volatile boolean taskExecutorSet;

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

	protected final void setReliabilityAttributes(boolean lengthCheck,
			boolean acknowledge, String ackHost, int ackPort, int ackTimeout) {
		this.mapper.setLengthCheck(lengthCheck);
		this.waitForAck = acknowledge;
		this.mapper.setAcknowledge(acknowledge);
		this.mapper.setAckAddress(ackHost + ":" + ackPort);
		this.ackPort = ackPort;
		if (ackTimeout > 0) {
			this.ackTimeout = ackTimeout;
		}
		this.acknowledge = acknowledge;
		if (this.acknowledge) {
			Assert.hasLength(ackHost);
		}
	}

	@Override
	public void doStart() {
		if (this.acknowledge) {
			if (this.taskExecutor == null) {
				Executor executor = Executors
						.newSingleThreadExecutor(new ThreadFactory() {
							private final AtomicInteger n = new AtomicInteger();
							@Override
							public Thread newThread(Runnable runner) {
								Thread thread = new Thread(runner);
								thread.setName("UDP-Ack-Handler-" + n.getAndIncrement());
								thread.setDaemon(true);
								return thread;
							}
						});
				this.taskExecutor = executor;
			}
		}
	}

	@Override
	protected void doStop() {
		this.closeSocketIfNeeded();
		if (!this.taskExecutorSet && this.taskExecutor != null) {
			((ExecutorService) this.taskExecutor).shutdown();
			this.taskExecutor = null;
		}
	}

	@Override
	public void handleMessageInternal(Message<?> message)
			throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		if (this.acknowledge) {
			Assert.state(this.isRunning(), "When 'acknowlege' is enabled, adapter must be running");
			if (!this.ackThreadRunning) {
				synchronized(this) {
					if (!this.ackThreadRunning) {
						ackLatch = new CountDownLatch(1);
						this.taskExecutor.execute(this);
						try {
							ackLatch.await(10000, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				}
			}
		}
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
			logger.debug("Sent packet for message " + message);
			if (this.waitForAck) {
				try {
					if (!countdownLatch.await(this.ackTimeout, TimeUnit.MILLISECONDS)) {
						throw new MessagingException(message, "Failed to receive UDP Ack in " + ackTimeout + " millis");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
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
			if (countdownLatch != null) {
				this.ackControl.remove(messageId);
			}
		}
	}

	protected void send(DatagramPacket packet) throws Exception {
		DatagramSocket socket = this.getSocket();
		packet.setSocketAddress(this.getDestinationAddress());
		socket.send(packet);
	}

	protected void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	protected DatagramSocket getTheSocket() {
		return this.socket;
	}

	protected synchronized DatagramSocket getSocket() throws IOException {
		if (this.socket == null) {
			if (acknowledge) {
				if (logger.isDebugEnabled()) {
					logger.debug("Listening for acks on port: " + ackPort);
				}
				if (localAddress == null) {
					this.socket = new DatagramSocket(this.ackPort);
				} else {
					InetAddress whichNic = InetAddress.getByName(this.localAddress);
					this.socket = new DatagramSocket(this.ackPort, whichNic);
				}
				if (this.soReceiveBufferSize > 0) {
					socket.setReceiveBufferSize(this.soReceiveBufferSize);
				}
			} else {
				this.socket = new DatagramSocket();
			}
			setSocketAttributes(this.socket);
		}
		return this.socket;
	}

	/**
	 * @see java.net.Socket#setReceiveBufferSize(int)
	 * @see DatagramSocket#setReceiveBufferSize(int)
	 */
	@Override
	public void setSoReceiveBufferSize(int size) {
		this.soReceiveBufferSize = size;
	}

	@Override
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		Assert.notNull(taskExecutor, "'taskExecutor' cannot be null");
		this.taskExecutor = taskExecutor;
		this.taskExecutorSet = true;
	}

	/**
	 * @param ackCounter the ackCounter to set
	 */
	public void setAckCounter(int ackCounter) {
		this.ackCounter = ackCounter;
	}

	@Override
	public String getComponentType(){
		return "ip:udp-outbound-channel-adapter";
	}

	/**
	 * @return the acknowledge
	 */
	public boolean isAcknowledge() {
		return acknowledge;
	}

	/**
	 * @return the ackPort
	 */
	public int getAckPort() {
		return ackPort;
	}

	/**
	 * @return the soReceiveBufferSize
	 */
	public int getSoReceiveBufferSize() {
		return soReceiveBufferSize;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.mapper.setBeanFactory(this.getBeanFactory());
	}

	protected void setSocketAttributes(DatagramSocket socket) throws SocketException {
		if (this.getSoTimeout() >= 0) {
			socket.setSoTimeout(this.getSoTimeout());
		}
		if (this.getSoSendBufferSize() > 0) {
			socket.setSendBufferSize(this.getSoSendBufferSize());
		}
	}

	/**
	 * Process acknowledgments, if requested.
	 */
	@Override
	public void run() {
		try {
			this.ackThreadRunning = true;
			ackLatch.countDown();
			DatagramPacket ackPack = new DatagramPacket(new byte[100], 100);
			while(true) {
				this.getSocket().receive(ackPack);
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
			if (this.socket != null && !this.socket.isClosed()) {
				logger.error("Error on UDP Acknowledge thread:" + e.getMessage());
			}
		}
		finally {
			this.ackThreadRunning = false;
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
		this.taskExecutor.execute(this);
	}

	/**
	 * @deprecated Use stop() instead.
	 */
	@Deprecated
	public void shutDown() {
		this.stop();
	}

	private void closeSocketIfNeeded() {
		if (socket != null) {
			socket.close();
			socket = null;
		}
	}

}
