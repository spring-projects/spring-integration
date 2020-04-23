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

package org.springframework.integration.ip.tcp.connection;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.serializer.AbstractByteArraySerializer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Base class for TcpConnections. TcpConnections are established by
 * client connection factories (outgoing) or server connection factories
 * (incoming).
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public abstract class TcpConnectionSupport implements TcpConnection {

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR final

	private final CountDownLatch listenerRegisteredLatch = new CountDownLatch(1);

	private final boolean server;

	private final AtomicLong sequence = new AtomicLong();

	private final ApplicationEventPublisher applicationEventPublisher;

	private final AtomicBoolean closePublished = new AtomicBoolean();

	private final AtomicBoolean exceptionSent = new AtomicBoolean();

	private final SocketInfo socketInfo;

	@SuppressWarnings("rawtypes")
	private Deserializer deserializer;

	@SuppressWarnings("rawtypes")
	private Serializer serializer;

	private TcpMessageMapper mapper;

	private TcpListener listener;

	private volatile TcpListener testListener;

	private TcpSender sender;

	private String connectionId;

	private String hostName = "unknown";

	private String hostAddress = "unknown";

	private String connectionFactoryName = "unknown";

	private boolean noReadErrorOnClose;

	private boolean manualListenerRegistration;

	/*
	 * This boolean is to avoid looking for a temporary listener when not needed
	 * to avoid a CPU cache flush. This does not have to be volatile because it
	 * is reset by the thread that checks for the temporary listener.
	 */
	private boolean needsTest;

	private volatile boolean testFailed;

	public TcpConnectionSupport() {
		this(null);
	}

	public TcpConnectionSupport(@Nullable ApplicationEventPublisher applicationEventPublisher) {
		this.server = false;
		this.applicationEventPublisher = applicationEventPublisher;
		this.socketInfo = null;
	}

	/**
	 * Creates a {@link TcpConnectionSupport} object and publishes a
	 * {@link TcpConnectionOpenEvent}, if an event publisher is provided.
	 * @param socket the underlying socket.
	 * @param server true if this connection is a server connection
	 * @param lookupHost true if reverse lookup of the host name should be performed,
	 * otherwise, the ip address will be used for identification purposes.
	 * @param applicationEventPublisher the publisher to which open, close and exception events will
	 * be sent; may be null if event publishing is not required.
	 * @param connectionFactoryName the name of the connection factory creating this connection; used
	 * during event publishing, may be null, in which case "unknown" will be used.
	 */
	public TcpConnectionSupport(Socket socket, boolean server, boolean lookupHost,
			@Nullable ApplicationEventPublisher applicationEventPublisher,
			@Nullable String connectionFactoryName) {

		this.socketInfo = new SocketInfo(socket);
		this.server = server;
		InetAddress inetAddress = socket.getInetAddress();
		if (inetAddress != null) {
			this.hostAddress = inetAddress.getHostAddress();
			if (lookupHost) {
				this.hostName = inetAddress.getHostName();
			}
			else {
				this.hostName = this.hostAddress;
			}
		}
		int port = socket.getPort();
		int localPort = socket.getLocalPort();
		this.connectionId = this.hostName + ":" + port + ":" + localPort + ":" + UUID.randomUUID().toString();
		this.applicationEventPublisher = applicationEventPublisher;
		if (connectionFactoryName != null) {
			this.connectionFactoryName = connectionFactoryName;
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("New connection " + this.connectionId);
		}
	}

	void setTestFailed(boolean testFailed) {
		this.testFailed = testFailed;
	}

	/**
	 * Closes this connection.
	 */
	@Override
	public void close() {
		if (this.sender != null) {
			this.sender.removeDeadConnection(this);
		}
		// close() may be called multiple times; only publish once
		if (!this.closePublished.getAndSet(true)) {
			this.publishConnectionCloseEvent();
		}
	}

	/**
	 * If we have been intercepted, propagate the close from the outermost interceptor;
	 * otherwise, just call close().
	 *
	 * @param isException true when this call is the result of an Exception.
	 */
	protected void closeConnection(boolean isException) {
		TcpListener tcpListener = getListener();
		if (!(tcpListener instanceof TcpConnectionInterceptor)) {
			close();
		}
		else {
			TcpConnectionInterceptor outerListener = (TcpConnectionInterceptor) tcpListener;
			while (outerListener.getListener() instanceof TcpConnectionInterceptor) {
				TcpConnectionInterceptor nextListener = (TcpConnectionInterceptor) outerListener.getListener();
				if (nextListener == null) {
					break;
				}
				outerListener = nextListener;
			}
			outerListener.close();
			if (isException) {
				// ensure physical close in case the interceptor did not close
				this.close();
			}
		}
	}

	/**
	 * @return the mapper
	 */
	public TcpMessageMapper getMapper() {
		return this.mapper;
	}

	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(TcpMessageMapper mapper) {
		Assert.notNull(mapper, this.getClass().getName() + " Mapper may not be null");
		this.mapper = mapper;
		if (this.serializer != null &&
				!(this.serializer instanceof AbstractByteArraySerializer)) {
			mapper.setStringToBytes(false);
		}
	}

	/**
	 *
	 * @return the deserializer
	 */
	@Override
	public Deserializer<?> getDeserializer() {
		return this.deserializer;
	}

	/**
	 * @param deserializer the deserializer to set
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 *
	 * @return the serializer
	 */
	@Override
	public Serializer<?> getSerializer() {
		return this.serializer;
	}

	/**
	 * @param serializer the serializer to set
	 */
	public void setSerializer(Serializer<?> serializer) {
		this.serializer = serializer;
		if (!(serializer instanceof AbstractByteArraySerializer)) {
			this.mapper.setStringToBytes(false);
		}
	}

	/**
	 * Set to true to use a temporary listener for just the first incoming message.
	 * @param needsTest true for a temporary listener.
	 * @since 5.3
	 */
	public void setNeedsTest(boolean needsTest) {
		this.needsTest = needsTest;
	}

	/**
	 * Set the listener that will receive incoming Messages.
	 * @param listener The listener.
	 */
	public void registerListener(@Nullable TcpListener listener) {
		this.listener = listener;
		this.listenerRegisteredLatch.countDown();
	}

	/**
	 * Set a temporary listener to receive just the first incoming message.
	 * Used in conjunction with a connectionTest in a client connection
	 * factory.
	 * @param tListener the test listener.
	 * @since 5.3
	 */
	public void registerTestListener(TcpListener tListener) {
		this.testListener = tListener;
	}

	/**
	 * Set whether or not automatic or manual registration of the {@link TcpListener} is to be
	 * used. (Default automatic). When manual registration is in place, incoming messages will
	 * be delayed until the listener is registered.
	 * @since 1.4.5
	 */
	public void enableManualListenerRegistration() {
		this.manualListenerRegistration = true;
		this.listener = message -> getListener().onMessage(message);
	}

	/**
	 * Registers a sender. Used on server side connections so a
	 * sender can determine which connection to send a reply
	 * to.
	 * @param sender the sender.
	 */
	public void registerSender(@Nullable TcpSender sender) {
		this.sender = sender;
		if (sender != null) {
			sender.addNewConnection(this);
		}
	}

	/**
	 * @return the listener
	 */
	@Override
	public TcpListener getListener() {
		if (this.needsTest && this.testListener != null) {
			this.needsTest = false;
			return this.testListener;
		}
		if (this.manualListenerRegistration && !this.testFailed) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(getConnectionId() + " Waiting for listener registration");
			}
			waitForListenerRegistration();
		}
		return this.listener;
	}

	private void waitForListenerRegistration() {
		try {
			Assert.state(this.listenerRegisteredLatch.await(1, TimeUnit.MINUTES), "TcpListener not registered");
			this.manualListenerRegistration = false;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Interrupted while waiting for listener registration", e);
		}
	}

	/**
	 * @return the sender
	 */
	public TcpSender getSender() {
		return this.sender;
	}

	@Override
	public boolean isServer() {
		return this.server;
	}

	@Override
	public long incrementAndGetConnectionSequence() {
		return this.sequence.incrementAndGet();
	}

	@Override
	public String getHostAddress() {
		return this.hostAddress;
	}

	@Override
	public String getHostName() {
		return this.hostName;
	}

	@Override
	public String getConnectionId() {
		return this.connectionId;
	}

	/**
	 * @since 4.2.5
	 */
	@Override
	public SocketInfo getSocketInfo() {
		return this.socketInfo;
	}

	public String getConnectionFactoryName() {
		return this.connectionFactoryName;
	}

	protected boolean isNoReadErrorOnClose() {
		return this.noReadErrorOnClose;
	}

	protected void setNoReadErrorOnClose(boolean noReadErrorOnClose) {
		this.noReadErrorOnClose = noReadErrorOnClose;
	}

	protected final void sendExceptionToListener(Exception e) {
		TcpListener listenerForException = getListener();
		if (!this.exceptionSent.getAndSet(true) && listenerForException != null) {
			Map<String, Object> headers = Collections.singletonMap(IpHeaders.CONNECTION_ID,
					(Object) this.getConnectionId());
			ErrorMessage errorMessage = new ErrorMessage(e, headers);
			listenerForException.onMessage(errorMessage);
		}
	}

	protected void publishConnectionOpenEvent() {
		doPublish(new TcpConnectionOpenEvent(this, getConnectionFactoryName()));
	}

	protected void publishConnectionCloseEvent() {
		doPublish(new TcpConnectionCloseEvent(this, getConnectionFactoryName()));
	}

	protected void publishConnectionExceptionEvent(Throwable t) {
		doPublish(new TcpConnectionExceptionEvent(this, getConnectionFactoryName(), t));
	}

	/**
	 * Allow interceptors etc to publish events, perhaps subclasses of
	 * TcpConnectionEvent. The event source must be this connection.
	 * @param event the event to publish.
	 */
	public void publishEvent(TcpConnectionEvent event) {
		Assert.isTrue(event.getSource() == this, "Can only publish events with this as the source");
		this.doPublish(event);
	}

	private void doPublish(TcpConnectionEvent event) {
		try {
			if (this.applicationEventPublisher == null) {
				this.logger.warn("No publisher available to publish " + event);
			}
			else {
				this.applicationEventPublisher.publishEvent(event);
				if (this.logger.isTraceEnabled()) {
					this.logger.trace("Published: " + event);
				}
			}
		}
		catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Failed to publish " + event, e);
			}
			else if (this.logger.isWarnEnabled()) {
				this.logger.warn("Failed to publish " + event + ":" + e.getMessage());
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + this.connectionId;
	}

}
