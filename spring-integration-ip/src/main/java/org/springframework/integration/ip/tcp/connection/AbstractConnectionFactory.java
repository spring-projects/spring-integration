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

package org.springframework.integration.ip.tcp.connection;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Base class for all connection factories.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public abstract class AbstractConnectionFactory extends IntegrationObjectSupport
		implements ConnectionFactory, ApplicationEventPublisherAware {

	private static final String UNUSED = "unused";

	protected static final int DEFAULT_REPLY_TIMEOUT = 10000;

	private static final int DEFAULT_NIO_HARVEST_INTERVAL = 2000;

	private static final int DEFAULT_READ_DELAY = 100;

	protected final Object lifecycleMonitor = new Object(); // NOSONAR final

	private final Map<String, TcpConnectionSupport> connections = new ConcurrentHashMap<>();

	private final BlockingQueue<PendingIO> delayedReads = new LinkedBlockingQueue<>();

	private String host;

	private int port;

	private TcpListener listener;

	private TcpSender sender;

	private int soTimeout = -1;

	private int soSendBufferSize;

	private int soReceiveBufferSize;

	private boolean soTcpNoDelay;

	private int soLinger = -1; // don't set by default

	private boolean soKeepAlive;

	private int soTrafficClass = -1; // don't set by default

	private Executor taskExecutor;

	private boolean privateExecutor;

	private Deserializer<?> deserializer = new ByteArrayCrLfSerializer();

	private boolean deserializerSet;

	private Serializer<?> serializer = new ByteArrayCrLfSerializer();

	private TcpMessageMapper mapper = new TcpMessageMapper();

	private boolean mapperSet;

	private boolean singleUse;

	private TcpConnectionInterceptorFactoryChain interceptorFactoryChain;

	private boolean lookupHost = true;

	private TcpSocketSupport tcpSocketSupport = new DefaultTcpSocketSupport();

	private long nextCheckForClosedNioConnections;

	private int nioHarvestInterval = DEFAULT_NIO_HARVEST_INTERVAL;

	private ApplicationEventPublisher applicationEventPublisher;

	private long readDelay = DEFAULT_READ_DELAY;

	private Integer sslHandshakeTimeout;

	private volatile boolean active;

	public AbstractConnectionFactory(int port) {
		this.port = port;
	}

	public AbstractConnectionFactory(String host, int port) {
		Assert.notNull(host, "host must not be null");
		this.host = host;
		this.port = port;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
		if (!this.deserializerSet && this.deserializer instanceof ApplicationEventPublisherAware) {
			((ApplicationEventPublisherAware) this.deserializer)
					.setApplicationEventPublisher(applicationEventPublisher);
		}
	}

	@Nullable
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	/**
	 * Sets socket attributes on the socket.
	 * @param socket The socket.
	 * @throws SocketException Any SocketException.
	 */
	protected void setSocketAttributes(Socket socket) throws SocketException {
		if (this.soTimeout >= 0) {
			socket.setSoTimeout(this.soTimeout);
		}
		if (this.soSendBufferSize > 0) {
			socket.setSendBufferSize(this.soSendBufferSize);
		}
		if (this.soReceiveBufferSize > 0) {
			socket.setReceiveBufferSize(this.soReceiveBufferSize);
		}
		socket.setTcpNoDelay(this.soTcpNoDelay);
		if (this.soLinger >= 0) {
			socket.setSoLinger(true, this.soLinger);
		}
		if (this.soTrafficClass >= 0) {
			socket.setTrafficClass(this.soTrafficClass);
		}
		socket.setKeepAlive(this.soKeepAlive);
		this.tcpSocketSupport.postProcessSocket(socket);
	}

	/**
	 * @return the soTimeout
	 */
	public int getSoTimeout() {
		return this.soTimeout;
	}

	/**
	 * @param soTimeout the soTimeout to set
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @return the soReceiveBufferSize
	 */
	public int getSoReceiveBufferSize() {
		return this.soReceiveBufferSize;
	}

	/**
	 * @param soReceiveBufferSize the soReceiveBufferSize to set
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @return the soSendBufferSize
	 */
	public int getSoSendBufferSize() {
		return this.soSendBufferSize;
	}

	/**
	 * @param soSendBufferSize the soSendBufferSize to set
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		this.soSendBufferSize = soSendBufferSize;
	}

	/**
	 * @return the soTcpNoDelay
	 */
	public boolean isSoTcpNoDelay() {
		return this.soTcpNoDelay;
	}

	/**
	 * @param soTcpNoDelay the soTcpNoDelay to set
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		this.soTcpNoDelay = soTcpNoDelay;
	}

	/**
	 * @return the soLinger
	 */
	public int getSoLinger() {
		return this.soLinger;
	}

	/**
	 * @param soLinger the soLinger to set
	 */
	public void setSoLinger(int soLinger) {
		this.soLinger = soLinger;
	}

	/**
	 * @return the soKeepAlive
	 */
	public boolean isSoKeepAlive() {
		return this.soKeepAlive;
	}

	/**
	 * @param soKeepAlive the soKeepAlive to set
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @return the soTrafficClass
	 */
	public int getSoTrafficClass() {
		return this.soTrafficClass;
	}

	/**
	 * @param soTrafficClass the soTrafficClass to set
	 */
	public void setSoTrafficClass(int soTrafficClass) {
		this.soTrafficClass = soTrafficClass;
	}

	/**
	 * Set the host; requires the factory to be stopped.
	 * @param host the host.
	 * @since 5.0
	 */
	public void setHost(String host) {
		Assert.state(!isRunning(), "Cannot change the host while running");
		this.host = host;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Set the port; requires the factory to be stopped.
	 * @param port the port.
	 * @since 5.0
	 */
	public void setPort(int port) {
		Assert.state(!isRunning(), "Cannot change the port while running");
		this.port = port;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @return the listener
	 */
	@Nullable
	public TcpListener getListener() {
		return this.listener;
	}

	/**
	 * @return the sender
	 */
	@Nullable
	public TcpSender getSender() {
		return this.sender;
	}

	/**
	 * @return the serializer
	 */
	public Serializer<?> getSerializer() {
		return this.serializer;
	}

	/**
	 * @return the deserializer
	 */
	public Deserializer<?> getDeserializer() {
		return this.deserializer;
	}

	/**
	 * @return the mapper
	 */
	public TcpMessageMapper getMapper() {
		return this.mapper;
	}

	/**
	 * Registers a TcpListener to receive messages after
	 * the payload has been converted from the input data.
	 * @param listenerToRegister the TcpListener.
	 */
	public void registerListener(TcpListener listenerToRegister) {
		Assert.isNull(this.listener, this.getClass().getName() +
				" may only be used by one inbound adapter");
		this.listener = listenerToRegister;
	}

	/**
	 * Registers a TcpSender; for server sockets, used to
	 * provide connection information so a sender can be used
	 * to reply to incoming messages.
	 * @param senderToRegister The sender
	 */
	public void registerSender(TcpSender senderToRegister) {
		Assert.isNull(this.sender, this.getClass().getName() + " may only be used by one outbound adapter");
		this.sender = senderToRegister;
	}

	/**
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 *
	 * @param deserializer the deserializer to set
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		this.deserializer = deserializer;
		this.deserializerSet = true;
	}

	/**
	 *
	 * @param serializer the serializer to set
	 */
	public void setSerializer(Serializer<?> serializer) {
		this.serializer = serializer;
	}

	/**
	 *
	 * @param mapper the mapper to set; defaults to a {@link TcpMessageMapper}
	 */
	public void setMapper(TcpMessageMapper mapper) {
		this.mapper = mapper;
		this.mapperSet = true;
	}

	/**
	 * @return the singleUse
	 */
	public boolean isSingleUse() {
		return this.singleUse;
	}

	/**
	 * If true, sockets created by this factory will be used once.
	 * @param singleUse The singleUse to set.
	 */
	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	/**
	 * If true, sockets created by this factory will be reused.
	 * Inverse of {@link #setSingleUse(boolean)}.
	 * @param leaveOpen The keepOpen to set.
	 * @since 5.0
	 */
	public void setLeaveOpen(boolean leaveOpen) {
		this.singleUse = !leaveOpen;
	}

	public void setInterceptorFactoryChain(TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		this.interceptorFactoryChain = interceptorFactoryChain;
	}

	/**
	 * If true, DNS reverse lookup is done on the remote ip address.
	 * Default true.
	 * @param lookupHost the lookupHost to set
	 */
	public void setLookupHost(boolean lookupHost) {
		this.lookupHost = lookupHost;
	}

	/**
	 * @return the lookupHost
	 */
	public boolean isLookupHost() {
		return this.lookupHost;
	}

	/**
	 * How often we clean up closed NIO connections if soTimeout is 0.
	 * Ignored when {@code soTimeout > 0} because the clean up
	 * process is run as part of the timeout handling.
	 * Default 2000 milliseconds.
	 * @param nioHarvestInterval The interval in milliseconds.
	 */
	public void setNioHarvestInterval(int nioHarvestInterval) {
		Assert.isTrue(nioHarvestInterval > 0, "NIO Harvest interval must be > 0");
		this.nioHarvestInterval = nioHarvestInterval;
	}

	/**
	 * Set the handshake timeout used when waiting for SSL handshake data; only applies
	 * to SSL connections, when using NIO.
	 * @param sslHandshakeTimeout the timeout.
	 * @since 4.3.6
	 */
	public void setSslHandshakeTimeout(int sslHandshakeTimeout) {
		this.sslHandshakeTimeout = sslHandshakeTimeout;
	}

	/**
	 * @return the handshake timeout.
	 * @see #setSslHandshakeTimeout(int)
	 * @since 4.3.6
	 */
	@Nullable
	protected Integer getSslHandshakeTimeout() {
		return this.sslHandshakeTimeout;
	}

	protected BlockingQueue<PendingIO> getDelayedReads() {
		return this.delayedReads;
	}

	protected long getReadDelay() {
		return this.readDelay;
	}

	/**
	 * The delay (in milliseconds) before retrying a read after the previous attempt
	 * failed due to insufficient threads. Default 100.
	 * @param readDelay the readDelay to set.
	 */
	public void setReadDelay(long readDelay) {
		Assert.isTrue(readDelay > 0, "'readDelay' must be positive");
		this.readDelay = readDelay;
	}

	protected Object getLifecycleMonitor() {
		return this.lifecycleMonitor;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (!this.mapperSet) {
			this.mapper.setBeanFactory(getBeanFactory());
		}
	}

	@Override
	public void start() {
		if (logger.isInfoEnabled()) {
			logger.info("started " + this);
		}
	}

	/**
	 * Creates a taskExecutor (if one was not provided).
	 * @return The executor.
	 */
	protected Executor getTaskExecutor() {
		if (!this.active) {
			throw new MessagingException("Connection Factory not started");
		}
		synchronized (this.lifecycleMonitor) {
			if (this.taskExecutor == null) {
				this.privateExecutor = true;
				this.taskExecutor = Executors.newCachedThreadPool();
			}
			return this.taskExecutor;
		}
	}

	/**
	 * Stops the server.
	 */
	@Override
	public void stop() {
		this.active = false;
		synchronized (this.connections) {
			Iterator<Entry<String, TcpConnectionSupport>> iterator = this.connections.entrySet().iterator();
			while (iterator.hasNext()) {
				TcpConnection connection = iterator.next().getValue();
				connection.close();
				iterator.remove();
			}
		}
		synchronized (this.lifecycleMonitor) {
			if (this.privateExecutor) {
				ExecutorService executorService = (ExecutorService) this.taskExecutor;
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
						logger.debug("Forcing executor shutdown");
						executorService.shutdownNow();
						if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
							logger.debug("Executor failed to shutdown");
						}
					}
				}
				catch (@SuppressWarnings(UNUSED) InterruptedException e) {
					executorService.shutdownNow();
					Thread.currentThread().interrupt();
				}
				finally {
					this.taskExecutor = null;
					this.privateExecutor = false;
				}
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("stopped " + this);
		}
	}

	protected TcpConnectionSupport wrapConnection(TcpConnectionSupport connectionArg) {
		TcpConnectionSupport connection = connectionArg;
		try {
			if (this.interceptorFactoryChain == null) {
				return connection;
			}
			TcpConnectionInterceptorFactory[] interceptorFactories =
					this.interceptorFactoryChain.getInterceptorFactories();
			if (interceptorFactories == null) {
				return connection;
			}
			for (TcpConnectionInterceptorFactory factory : interceptorFactories) {
				TcpConnectionInterceptorSupport wrapper = factory.getInterceptor();
				wrapper.setTheConnection(connection);
				// if no ultimate listener or sender, register each wrapper in turn
				if (this.listener == null) {
					connection.registerListener(wrapper);
				}
				if (this.sender == null) {
					connection.registerSender(wrapper);
				}
				connection = wrapper;
			}
			return connection;
		}
		finally {
			this.addConnection(connection);
		}
	}

	/**
	 *
	 * Times out any expired connections then, if {@code selectionCount > 0},
	 * processes the selected keys.
	 * Removes closed connections from the connections field, and from the connections parameter.
	 * @param selectionCount Number of IO Events, if 0 we were probably woken up by a close.
	 * @param selector The selector.
	 * @param server The server socket channel.
	 * @param connectionMap Map of connections.
	 */
	protected void processNioSelections(int selectionCount, final Selector selector,
			@Nullable ServerSocketChannel server,
			Map<SocketChannel, TcpNioConnection> connectionMap) {

		final long now = System.currentTimeMillis();
		rescheduleDelayedReads(selector, now);
		if (this.soTimeout > 0 ||
				now >= this.nextCheckForClosedNioConnections ||
				selectionCount == 0) {
			this.nextCheckForClosedNioConnections = now + this.nioHarvestInterval;
			Iterator<Entry<SocketChannel, TcpNioConnection>> it = connectionMap.entrySet().iterator();
			while (it.hasNext()) {
				SocketChannel channel = it.next().getKey();
				if (!channel.isOpen()) {
					logger.debug("Removing closed channel");
					it.remove();
				}
				else if (this.soTimeout > 0) {
					TcpNioConnection connection = connectionMap.get(channel);
					if (now - connection.getLastRead() >= this.soTimeout) {
						/*
						 * For client connections, we have to wait for 2 timeouts if the last
						 * send was within the current timeout.
						 */
						if (!connection.isServer() &&
								now - connection.getLastSend() < this.soTimeout &&
								now - connection.getLastRead() < this.soTimeout * 2) {
							if (logger.isDebugEnabled()) {
								logger.debug("Skipping a connection timeout because we have a recent send " +
										connection.getConnectionId());
							}
						}
						else {
							if (logger.isWarnEnabled()) {
								logger.warn("Timing out TcpNioConnection " + connection.getConnectionId());
							}
							Exception exception = new SocketTimeoutException("Timing out connection");
							connection.publishConnectionExceptionEvent(exception);
							connection.timeout();
							connection.sendExceptionToListener(exception);
						}
					}
				}
			}
		}
		harvestClosedConnections();
		if (logger.isTraceEnabled()) {
			if (this.host == null) {
				logger.trace("Port " + this.port + " SelectionCount: " + selectionCount);
			}
			else {
				logger.trace("Host " + this.host + " port " + this.port + " SelectionCount: " + selectionCount);
			}
		}
		if (selectionCount > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while (iterator.hasNext()) {
				final SelectionKey key = iterator.next();
				iterator.remove();
				try {
					if (!key.isValid()) {
						logger.debug("Selection key no longer valid");
					}
					else if (key.isReadable()) {
						key.interestOps(key.interestOps() - SelectionKey.OP_READ);
						final TcpNioConnection connection;
						connection = (TcpNioConnection) key.attachment();
						connection.setLastRead(System.currentTimeMillis());
						try {
							this.taskExecutor.execute(() -> {
								boolean delayed = false;
								try {
									connection.readPacket();
								}
								catch (@SuppressWarnings(UNUSED) RejectedExecutionException e1) {
									delayRead(selector, now, key);
									delayed = true;
								}
								catch (Exception e2) {
									if (connection.isOpen()) {
										logger.error("Exception on read " +
												connection.getConnectionId() + " " +
												e2.getMessage());
										connection.close();
									}
									else {
										logger.debug("Connection closed");
									}
								}
								if (!delayed) {
									if (key.channel().isOpen()) {
										key.interestOps(SelectionKey.OP_READ);
										selector.wakeup();
									}
									else {
										connection.sendExceptionToListener(new EOFException("Connection is closed"));
									}
								}
							});
						}
						catch (@SuppressWarnings(UNUSED) RejectedExecutionException e) {
							delayRead(selector, now, key);
						}
					}
					else if (key.isAcceptable()) {
						try {
							doAccept(selector, server, now);
						}
						catch (Exception e) {
							logger.error("Exception accepting new connection(s)", e);
						}
					}
					else {
						logger.error("Unexpected key: " + key);
					}
				}
				catch (@SuppressWarnings(UNUSED) CancelledKeyException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Selection key " + key + " cancelled");
					}
				}
				catch (Exception e) {
					logger.error("Exception on selection key " + key, e);
				}
			}
		}
	}

	protected void delayRead(Selector selector, long now, final SelectionKey key) {
		TcpNioConnection connection = (TcpNioConnection) key.attachment();
		if (!this.delayedReads.add(new PendingIO(now, key))) { // should never happen - unbounded queue
			logger.error("Failed to delay read; closing " + connection.getConnectionId());
			connection.close();
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No threads available, delaying read for " + connection.getConnectionId());
			}
			// wake the selector in case it is currently blocked, and waiting for longer than readDelay
			selector.wakeup();
		}
	}

	/**
	 * If any reads were delayed due to insufficient threads, reschedule them if
	 * the readDelay has passed.
	 * @param selector the selector to wake if necessary.
	 * @param now the current time.
	 */
	private void rescheduleDelayedReads(Selector selector, long now) {
		boolean wakeSelector = false;
		try {
			while (this.delayedReads.size() > 0) {
				if (this.delayedReads.peek().failedAt + this.readDelay < now) {
					PendingIO pendingRead = this.delayedReads.take();
					if (pendingRead.key.channel().isOpen()) {
						pendingRead.key.interestOps(SelectionKey.OP_READ);
						wakeSelector = true;
						if (logger.isDebugEnabled()) {
							logger.debug("Rescheduling delayed read for " +
									((TcpNioConnection) pendingRead.key.attachment()).getConnectionId());
						}
					}
					else {
						((TcpNioConnection) pendingRead.key.attachment())
								.sendExceptionToListener(new EOFException("Connection is closed"));
					}
				}
				else {
					// remaining delayed reads have not expired yet.
					break;
				}
			}
		}
		catch (@SuppressWarnings(UNUSED) InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		finally {
			if (wakeSelector) {
				selector.wakeup();
			}
		}
	}

	/**
	 * @param selector The selector.
	 * @param server The server socket channel.
	 * @param now The current time.
	 */
	protected void doAccept(final Selector selector, ServerSocketChannel server, long now) {
		throw new UnsupportedOperationException("Nio server factory must override this method");
	}

	protected void addConnection(TcpConnectionSupport connection) {
		synchronized (this.connections) {
			if (!this.active) {
				connection.close();
				return;
			}
			this.connections.put(connection.getConnectionId(), connection);
			if (logger.isDebugEnabled()) {
				logger.debug(getComponentName() + ": Added new connection: " + connection.getConnectionId());
			}
		}
	}

	/**
	 * Cleans up this.connections by removing any closed connections.
	 * @return a list of open connection ids.
	 */
	private List<String> removeClosedConnectionsAndReturnOpenConnectionIds() {
		synchronized (this.connections) {
			List<String> openConnectionIds = new ArrayList<>();
			Iterator<Entry<String, TcpConnectionSupport>> iterator = this.connections.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, TcpConnectionSupport> entry = iterator.next();
				TcpConnectionSupport connection = entry.getValue();
				if (!connection.isOpen()) {
					iterator.remove();
					if (logger.isDebugEnabled()) {
						logger.debug(getComponentName() + ": Removed closed connection: " +
								connection.getConnectionId());
					}
				}
				else {
					openConnectionIds.add(entry.getKey());
					if (logger.isTraceEnabled()) {
						logger.trace(getComponentName() + ": Connection is open: " + connection.getConnectionId());
					}
				}
			}
			return openConnectionIds;
		}
	}

	/**
	 * Cleans up this.connections by removing any closed connections.
	 */
	protected void harvestClosedConnections() {
		removeClosedConnectionsAndReturnOpenConnectionIds();
	}

	@Override
	public boolean isRunning() {
		return this.active;
	}

	/**
	 * @return the active
	 */
	protected boolean isActive() {
		return this.active;
	}

	/**
	 * @param active the active to set
	 */
	protected void setActive(boolean active) {
		this.active = active;
	}

	protected void checkActive() {
		if (!isActive()) {
			throw new UncheckedIOException(new IOException(this + " connection factory has not been started"));
		}
	}

	protected TcpSocketSupport getTcpSocketSupport() {
		return this.tcpSocketSupport;
	}

	public void setTcpSocketSupport(TcpSocketSupport tcpSocketSupport) {
		Assert.notNull(tcpSocketSupport, "TcpSocketSupport must not be null");
		this.tcpSocketSupport = tcpSocketSupport;
	}

	/**
	 * Returns a list of (currently) open {@link TcpConnection} connection ids; allows,
	 * for example, broadcast operations to all open connections.
	 * @return the list of connection ids.
	 */
	public List<String> getOpenConnectionIds() {
		return Collections.unmodifiableList(removeClosedConnectionsAndReturnOpenConnectionIds());
	}

	/**
	 * Close a connection with the specified connection id.
	 * @param connectionId the connection id.
	 * @return true if the connection was closed.
	 */
	public boolean closeConnection(String connectionId) {
		Assert.notNull(connectionId, "'connectionId' to close must not be null");
		// closed connections are removed from #connections in #harvestClosedConnections()
		synchronized (this.connections) {
			boolean closed = false;
			TcpConnectionSupport connection = this.connections.get(connectionId);
			if (connection != null) {
				try {
					connection.close();
					closed = true;
				}
				catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to close connection " + connectionId, e);
					}
					connection.publishConnectionExceptionEvent(e);
				}
			}
			return closed;
		}
	}

	@Override
	public String toString() {
		return super.toString()
				+ (this.host != null ? ", host=" + this.host : "")
				+ ", port=" + getPort();
	}

	private static final class PendingIO {

		private final long failedAt;

		private final SelectionKey key;

		private PendingIO(long failedAt, SelectionKey key) {
			this.failedAt = failedAt;
			this.key = key;
		}

	}

}
