/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 /**
 * Implements a server connection factory that produces {@link TcpNioConnection}s using
 * a {@link ServerSocketChannel}. Must have a {@link TcpListener} registered.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpNioServerConnectionFactory extends AbstractServerConnectionFactory {

	private final Map<SocketChannel, TcpNioConnection> channelMap = new HashMap<>();

	private TcpNioConnectionSupport tcpNioConnectionSupport = new DefaultTcpNioConnectionSupport();

	private boolean multiAccept = true;

	private boolean usingDirectBuffers;

	private volatile ServerSocketChannel serverChannel;

	private volatile Selector selector;

	/**
	 * Listens for incoming connections on the port.
	 * @param port The port.
	 */
	public TcpNioServerConnectionFactory(int port) {
		super(port);
	}

	/**
	 * Set to false to only accept one connection per iteration over the
	 * selector keys. This might be necessary to avoid accepts overwhelming
	 * reads of existing sockets. By default when the {@code OP_ACCEPT} operation
	 * is ready, we will keep accepting connections in a loop until no more arrive.
	 * @param multiAccept false to accept connections one-at-a-time.
	 * @since 5.1.4
	 */
	public void setMultiAccept(boolean multiAccept) {
		this.multiAccept = multiAccept;
	}

	@Override
	public String getComponentType() {
		return "tcp-nio-server-connection-factory";
	}

	@Override
	public int getPort() {
		int port = super.getPort();
		ServerSocketChannel channel = this.serverChannel;
		if (port == 0 && channel != null) {
			try {
				SocketAddress address = channel.getLocalAddress();
				if (address instanceof InetSocketAddress) {
					port = ((InetSocketAddress) address).getPort();
				}
			}
			catch (IOException ex) {
				logger.error(ex, "Error getting port");
			}
		}
		return port;
	}

	@Override
	@Nullable
	public SocketAddress getServerSocketAddress() {
		if (this.serverChannel != null) {
			try {
				return this.serverChannel.getLocalAddress();
			}
			catch (IOException ex) {
				logger.error(ex, "Error getting local address");
			}
		}
		return null;
	}

	/**
	 * If no listener registers, exits.
	 * Accepts incoming connections and creates TcpConnections for each new connection.
	 * Invokes {{@link #initializeConnection(TcpConnectionSupport, Socket)} and executes the
	 * connection {@link TcpConnection#run()} using the task executor.
	 * I/O errors on the server socket/channel are logged and the factory is stopped.
	 */
	@Override
	public void run() {
		if (getListener() == null) {
			logger.info(() -> this + " No listener bound to server connection factory; will not read; exiting...");
			return;
		}
		try {
			this.serverChannel = ServerSocketChannel.open();
			int port = super.getPort();
			getTcpSocketSupport().postProcessServerSocket(this.serverChannel.socket());
			this.serverChannel.configureBlocking(false);
			String localAddress = getLocalAddress();
			if (localAddress == null) {
				this.serverChannel.socket().bind(new InetSocketAddress(port), Math.abs(getBacklog()));
			}
			else {
				InetAddress whichNic = InetAddress.getByName(localAddress);
				this.serverChannel.socket().bind(new InetSocketAddress(whichNic, port), Math.abs(getBacklog()));
			}
			logger.info(() -> this + " Listening");
			final Selector theSelector = Selector.open();
			if (this.serverChannel == null) {
				logger.debug(() -> this + " stopped before registering the server channel");
			}
			else {
				this.serverChannel.register(theSelector, SelectionKey.OP_ACCEPT);
				setListening(true);
				publishServerListeningEvent(getPort());
				this.selector = theSelector;
				doSelect(this.serverChannel, theSelector);
			}
		}
		catch (IOException ex) {
			if (isActive()) {
				logger.error(ex, "Error on ServerChannel; port = " + getPort());
				publishServerExceptionEvent(ex);
			}
			stop();
		}
		finally {
			setListening(false);
		}
	}

	/**
	 * Listens for incoming connections and for notifications that a connected
	 * socket is ready for reading.
	 * Accepts incoming connections, registers the new socket with the
	 * selector for reading.
	 * When a socket is ready for reading, unregisters the read interest and
	 * schedules a call to doRead which reads all available data. When the read
	 * is complete, the socket is again registered for read interest.
	 * @param server the ServerSocketChannel to select
	 * @param selectorToSelect the Selector multiplexor
	 * @throws IOException a thrown IO exception
	 */
	private void doSelect(ServerSocketChannel server, final Selector selectorToSelect) throws IOException {
		while (isActive()) {
			int soTimeout = getSoTimeout();
			int selectionCount;
			try {
				long timeout = Math.max(soTimeout, 0);
				if (getDelayedReads().size() > 0 && (timeout == 0 || getReadDelay() < timeout)) {
					timeout = getReadDelay();
				}
				long timeoutToLog = timeout;
				logger.trace(() -> "Delayed reads: " + getDelayedReads().size() + " timeout " + timeoutToLog);
				selectionCount = selectorToSelect.select(timeout);
				processNioSelections(selectionCount, selectorToSelect, server, this.channelMap);
			}
			catch (@SuppressWarnings("unused") CancelledKeyException cke) {
				logger.debug("CancelledKeyException during Selector.select()");
			}
			catch (ClosedSelectorException cse) {
				if (isActive()) {
					logger.error(cse, "Selector closed");
					publishServerExceptionEvent(cse);
					break;
				}
			}
		}
	}

	/**
	 * @param selectorForNewSocket The selector.
	 * @param server The server socket channel.
	 * @param now The current time.
	 */
	@Override
	protected void doAccept(final Selector selectorForNewSocket, ServerSocketChannel server, long now) {
		logger.debug("New accept");
		try {
			SocketChannel channel;
			do {
				SocketChannel theChannel = server.accept();
				if (theChannel != null) {
					if (isShuttingDown()) {
						logger.info(() ->
								"New connection from " + theChannel.socket().getInetAddress().getHostAddress()
										+ ":" + theChannel.socket().getPort()
										+ " rejected; the server is in the process of shutting down.");
						theChannel.close();
					}
					else if (createConnectionForAcceptedChannel(selectorForNewSocket, now, theChannel) == null) {
						return;
					}
				}
				channel = theChannel;
			}
			while (this.multiAccept && channel != null);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nullable
	private TcpNioConnection createConnectionForAcceptedChannel(Selector selectorForNewSocket, long now,
			SocketChannel channel) throws IOException {

		TcpNioConnection connection = null;
		try {
			channel.configureBlocking(false);
			Socket socket = channel.socket();
			setSocketAttributes(socket);
			connection = createTcpNioConnection(channel);
			if (connection != null) {
				connection.setTaskExecutor(getTaskExecutor());
				connection.setLastRead(now);
				Integer sslHandshakeTimeout = getSslHandshakeTimeout();
				if (sslHandshakeTimeout != null && connection instanceof TcpNioSSLConnection) {
					((TcpNioSSLConnection) connection).setHandshakeTimeout(sslHandshakeTimeout);
				}
				this.channelMap.put(channel, connection);
				channel.register(selectorForNewSocket, SelectionKey.OP_READ, connection);
			}
		}
		catch (IOException ex) {
			logger.error(ex, "Exception accepting new connection from "
					+ channel.socket().getInetAddress().getHostAddress()
					+ ":" + channel.socket().getPort());
			channel.close();
		}
		return connection;
	}

	@Nullable
	private TcpNioConnection createTcpNioConnection(SocketChannel socketChannel) {
		try {
			TcpNioConnection connection = this.tcpNioConnectionSupport.createNewConnection(socketChannel, true,
					isLookupHost(), getApplicationEventPublisher(), getComponentName());
			connection.setUsingDirectBuffers(this.usingDirectBuffers);
			TcpConnectionSupport wrappedConnection = wrapConnection(connection);
			if (!wrappedConnection.equals(connection)) {
				connection.setSenders(getSenders());
			}
			initializeConnection(wrappedConnection, socketChannel.socket());
			wrappedConnection.publishConnectionOpenEvent();
			return connection;
		}
		catch (Exception ex) {
			logger.error(ex, "Failed to establish new incoming connection");
			return null;
		}
	}

	@Override
	public void stop() {
		setActive(false);
		if (this.selector != null) {
			try {
				this.selector.close();
			}
			catch (Exception ex) {
				logger.error(ex, "Error closing selector");
			}
		}
		if (this.serverChannel != null) {
			try {
				this.serverChannel.close();
			}
			catch (IOException e) {
			}
			finally {
				this.serverChannel = null;
			}
		}

		super.stop();
	}

	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	public void setTcpNioConnectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		Assert.notNull(tcpNioSupport, "TcpNioSupport must not be null");
		this.tcpNioConnectionSupport = tcpNioSupport;
	}

	/**
	 * @return the serverChannel
	 */
	protected ServerSocketChannel getServerChannel() {
		return this.serverChannel;
	}

	/**
	 * @return the usingDirectBuffers
	 */
	protected boolean isUsingDirectBuffers() {
		return this.usingDirectBuffers;
	}

	/**
	 * @return the connections
	 */
	protected Map<SocketChannel, TcpNioConnection> getConnections() {
		return this.channelMap;
	}

}
