/*
 * Copyright 2001-2020 the original author or authors.
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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.ip.AbstractInternetProtocolSendingMessageHandler;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation that maps a Message into
 * a UDP datagram packet and sends that to the specified host and port.
 *
 * Messages can be basic, with no support for reliability, can be prefixed
 * by a length so the receiving end can detect truncation, and can require
 * a UDP acknowledgment to confirm delivery.
 *
 * @author Gary Russell
 * @author Marcin Pilaczynski
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class UnicastSendingMessageHandler extends
		AbstractInternetProtocolSendingMessageHandler implements Runnable {

	private static final int DEFAULT_ACK_TIMEOUT = 5000;

	private final DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();

	private final Map<String, CountDownLatch> ackControl = Collections.synchronizedMap(new HashMap<>());

	private final Expression destinationExpression;

	/**
	 * If true adds headers to instruct receiving adapter to return an ack.
	 */
	private boolean waitForAck = false;

	private boolean acknowledge = false;

	private String ackHost;

	private int ackPort;

	private int ackTimeout = DEFAULT_ACK_TIMEOUT;

	private int ackCounter = 1;

	private int soReceiveBufferSize = -1;

	private String localAddress;

	private DatagramSocket socket;

	private Executor taskExecutor;

	private boolean taskExecutorSet;

	private Expression socketExpression;

	private EvaluationContext evaluationContext;

	private SocketCustomizer socketCustomizer = socket -> { };

	private volatile CountDownLatch ackLatch;

	private volatile boolean ackThreadRunning;

	/**
	 * Basic constructor; no reliability; no acknowledgment.
	 * @param host Destination host.
	 * @param port Destination port.
	 */
	public UnicastSendingMessageHandler(String host, int port) {
		super(host, port);
		this.mapper.setLengthCheck(false);
		this.mapper.setAcknowledge(false);
		this.destinationExpression = null;
	}

	/**
	 * Construct UnicastSendingMessageHandler based on the destination SpEL expression to
	 * determine the target destination at runtime against requestMessage.
	 * @param destinationExpression the SpEL expression to evaluate the target destination
	 * at runtime. Must evaluate to {@link String}, {@link URI} or {@link SocketAddress}.
	 * @since 4.3
	 */
	public UnicastSendingMessageHandler(String destinationExpression) {
		super("", 0);
		Assert.hasText(destinationExpression, "'destinationExpression' cannot be null or empty");
		this.mapper.setLengthCheck(false);
		this.mapper.setAcknowledge(false);
		this.destinationExpression = EXPRESSION_PARSER.parseExpression(destinationExpression);
	}

	/**
	 * Construct UnicastSendingMessageHandler based on the destination SpEL expression to
	 * determine the target destination at runtime against requestMessage.
	 * @param destinationExpression the SpEL expression to evaluate the target destination
	 * at runtime. Must evaluate to {@link String}, {@link URI} or {@link SocketAddress}.
	 * @since 4.3
	 */
	public UnicastSendingMessageHandler(Expression destinationExpression) {
		super("", 0);
		Assert.notNull(destinationExpression, "'destinationExpression' cannot be null");
		this.mapper.setLengthCheck(false);
		this.mapper.setAcknowledge(false);
		this.destinationExpression = destinationExpression;
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
		this.destinationExpression = null;
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
		this.destinationExpression = null;
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
		this.destinationExpression = null;
		setReliabilityAttributes(lengthCheck, acknowledge, ackHost, ackPort,
				ackTimeout);
	}

	/**
	 * @param lengthCheck if true, a four byte binary length header is added to the
	 * packet, allowing the receiver to check for data truncation.
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

	protected final void setReliabilityAttributes(boolean lengthCheck,
			boolean acknowledge, String ackHost, int ackPort, int ackTimeout) {

		this.mapper.setLengthCheck(lengthCheck);
		this.waitForAck = acknowledge;
		this.mapper.setAcknowledge(acknowledge);
		this.mapper.setAckAddress(ackHost + ":" + ackPort);
		this.ackHost = ackHost;
		this.ackPort = ackPort;
		if (ackTimeout > 0) {
			this.ackTimeout = ackTimeout;
		}
		this.acknowledge = acknowledge;
		if (this.acknowledge) {
			Assert.state(StringUtils.hasText(ackHost), "'ackHost' must not be empty");
		}
	}

	@Override
	public void doStart() {
		if (this.acknowledge) {
			if (this.taskExecutor == null) {

				CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("UDP-Ack-Handler-");
				threadFactory.setDaemon(true);

				this.taskExecutor = Executors.newSingleThreadExecutor(threadFactory);
			}
			startAckThread();
		}
	}

	@Override
	protected void doStop() {
		closeSocketIfNeeded();
		if (!this.taskExecutorSet && this.taskExecutor != null) {
			((ExecutorService) this.taskExecutor).shutdown();
			this.taskExecutor = null;
		}
	}

	@Override
	public void handleMessageInternal(Message<?> message) {
		if (this.acknowledge) {
			Assert.state(this.isRunning(), "When 'acknowledge' is enabled, adapter must be running");
			startAckThread();
		}
		CountDownLatch countdownLatch = null;
		UUID id = message.getHeaders().getId();
		if (id == null) {
			id = UUID.randomUUID();
		}
		String messageId = id.toString();
		try {
			boolean waitAck = this.waitForAck;
			if (waitAck) {
				countdownLatch = new CountDownLatch(this.ackCounter);
				this.ackControl.put(messageId, countdownLatch);
			}
			convertAndSend(message);
			if (waitAck) {
				try {
					if (!countdownLatch.await(this.ackTimeout, TimeUnit.MILLISECONDS)) {
						throw new MessagingException(message, "Failed to receive UDP Ack in "
								+ this.ackTimeout + " millis");
					}
				}
				catch (@SuppressWarnings("unused") InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		catch (Exception ex) {
			if (!(ex instanceof MessagingException)) { // NOSONAR
				closeSocketIfNeeded();
			}
			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
					() -> "Failed to send UDP packet in the [" + this + ']', ex);
		}
		finally {
			if (countdownLatch != null) {
				this.ackControl.remove(messageId);
			}
		}
	}

	public void startAckThread() {
		if (!this.ackThreadRunning) {
			synchronized (this) {
				if (!this.ackThreadRunning) {
					try {
						getSocket();
					}
					catch (IOException ex) {
						logger.error(ex, "Error creating socket");
					}
					this.ackLatch = new CountDownLatch(1);
					this.taskExecutor.execute(this);
					try {
						this.ackLatch.await(10000, TimeUnit.MILLISECONDS);
					}
					catch (@SuppressWarnings("unused") InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

	protected void convertAndSend(Message<?> message) throws IOException, URISyntaxException {
		DatagramSocket datagramSocket;
		if (this.socketExpression != null) {
			datagramSocket = this.socketExpression.getValue(this.evaluationContext, message, DatagramSocket.class);
		}
		else {
			datagramSocket = getSocket();
		}
		SocketAddress destinationAddress;
		if (this.destinationExpression != null) {
			Object destination = this.destinationExpression.getValue(this.evaluationContext, message);
			if (destination instanceof String) {
				destination = new URI((String) destination);
			}
			if (destination instanceof URI) {
				URI uri = (URI) destination;
				destination = new InetSocketAddress(uri.getHost(), uri.getPort());
			}
			if (destination instanceof SocketAddress) {
				destinationAddress = (SocketAddress) destination;
			}
			else {
				throw new IllegalStateException("'destinationExpression' must evaluate to String, URI " +
						"or SocketAddress. Gotten [" + destination + "].");
			}
		}
		else {
			destinationAddress = getDestinationAddress();
		}
		DatagramPacket packet = this.mapper.fromMessage(message);
		if (packet != null) {
			packet.setSocketAddress(destinationAddress);
			datagramSocket.send(packet);
			logger.debug(() -> "Sent packet for message " + message + " to " + packet.getSocketAddress());
		}
		else {
			logger.debug(() -> "Mapper created no packet for message " + message);
		}
	}

	protected void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	@Nullable
	protected DatagramSocket getTheSocket() {
		return this.socket;
	}

	protected synchronized DatagramSocket getSocket() throws IOException {
		if (this.socket == null) {
			if (this.acknowledge) {
				if (this.localAddress == null) {
					this.socket = this.ackPort == 0 ? new DatagramSocket() : new DatagramSocket(this.ackPort);
				}
				else {
					InetAddress whichNic = InetAddress.getByName(this.localAddress);
					this.socket = new DatagramSocket(new InetSocketAddress(whichNic, this.ackPort));
				}
				if (this.soReceiveBufferSize > 0) {
					this.socket.setReceiveBufferSize(this.soReceiveBufferSize);
				}
				logger.debug(() -> "Listening for acks on port: " + getAckPort());
				updateAckAddress();
			}
			else {
				this.socket = new DatagramSocket();
			}
			setSocketAttributes(this.socket);
		}
		return this.socket;
	}

	protected void updateAckAddress() {
		this.mapper.setAckAddress(this.ackHost + ':' + getAckPort());
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
	public synchronized void setLocalAddress(String localAddress) {
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

	/**
	 * @param socketExpression the socket expression to determine the target socket at runtime.
	 * @since 4.3
	 */
	public void setSocketExpression(Expression socketExpression) {
		this.socketExpression = socketExpression;
	}

	/**
	 * @param socketExpression the socket SpEL expression to determine the target socket at runtime.
	 * @since 4.3
	 */
	public void setSocketExpressionString(String socketExpression) {
		this.socketExpression = EXPRESSION_PARSER.parseExpression(socketExpression);
	}

	@Override
	public String getComponentType() {
		return "ip:udp-outbound-channel-adapter";
	}

	/**
	 * @return the acknowledge
	 */
	public boolean isAcknowledge() {
		return this.acknowledge;
	}

	/**
	 * @return the ackPort
	 */
	public int getAckPort() {
		DatagramSocket datagramSocket = this.socket;
		if (this.ackPort == 0 && datagramSocket != null) {
			return datagramSocket.getLocalPort();
		}
		else {
			return this.ackPort;
		}
	}

	/**
	 * @return the soReceiveBufferSize
	 */
	public int getSoReceiveBufferSize() {
		return this.soReceiveBufferSize;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.mapper.setBeanFactory(getBeanFactory());
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
		if (this.socketExpression != null) {
			Assert.state(!this.acknowledge, "'acknowledge' must be false when using a socket expression");
		}
	}

	protected void setSocketAttributes(DatagramSocket socket) throws SocketException {
		int soTimeout = getSoTimeout();
		if (soTimeout >= 0) {
			socket.setSoTimeout(soTimeout);
		}
		int soSendBufferSize = getSoSendBufferSize();
		if (soSendBufferSize > 0) {
			socket.setSendBufferSize(soSendBufferSize);
		}
		this.socketCustomizer.configure(socket);
	}

	/**
	 * Process acknowledgments, if requested.
	 */
	@Override
	public void run() {
		try {
			this.ackThreadRunning = true;
			this.ackLatch.countDown();
			DatagramPacket ackPack = new DatagramPacket(new byte[100], 100);
			while (true) {
				getSocket().receive(ackPack);
				String id = new String(ackPack.getData(), ackPack.getOffset(), ackPack.getLength());
				logger.debug(() -> "Received ack for " + id + " from " + ackPack.getAddress().getHostAddress());
				CountDownLatch latch = this.ackControl.get(id);
				if (latch != null) {
					latch.countDown();
				}
			}
		}
		catch (IOException ex) {
			if (this.socket != null && !this.socket.isClosed()) {
				logger.error(() -> "Error on UDP Acknowledge thread: " + ex.getMessage());
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
		this.taskExecutor.execute(this);
	}

	private void closeSocketIfNeeded() {
		if (this.socket != null) {
			try {
				this.socket.close();
			}
			catch (Exception e) {
			}
			this.socket = null;
		}
	}

}
