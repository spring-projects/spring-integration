/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
/**
 * Implements a server connection factory that produces {@link TcpNioConnection}s using
 * a {@link ServerSocketChannel}. Must have a {@link TcpListener} registered.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 *
 */
public class TcpNioServerConnectionFactory extends AbstractServerConnectionFactory {

	private volatile ServerSocketChannel serverChannel;

	private volatile boolean usingDirectBuffers;

	private final Map<SocketChannel, TcpNioConnection> channelMap = new HashMap<SocketChannel, TcpNioConnection>();

	private volatile Selector selector;

	private volatile TcpNioConnectionSupport tcpNioConnectionSupport = new DefaultTcpNioConnectionSupport();

	/**
	 * Listens for incoming connections on the port.
	 * @param port The port.
	 */
	public TcpNioServerConnectionFactory(int port) {
		super(port);
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
		if (this.getListener() == null) {
			logger.info("No listener bound to server connection factory; will not read; exiting...");
			return;
		}
		try {
			this.serverChannel = ServerSocketChannel.open();
			int port = this.getPort();
			this.getTcpSocketSupport().postProcessServerSocket(this.serverChannel.socket());
			if (logger.isInfoEnabled()) {
				logger.info("Listening on port " + port);
			}
			this.serverChannel.configureBlocking(false);
			if (this.getLocalAddress() == null) {
				this.serverChannel.socket().bind(new InetSocketAddress(port),
					Math.abs(this.getBacklog()));
			}
			else {
				InetAddress whichNic = InetAddress.getByName(this.getLocalAddress());
				this.serverChannel.socket().bind(new InetSocketAddress(whichNic, port),
						Math.abs(this.getBacklog()));
			}
			final Selector selector = Selector.open();
			this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			this.setListening(true);
			this.selector = selector;
			doSelect(this.serverChannel, selector);

		}
		catch (IOException e) {
			this.stop();
		}
		finally {
			this.setListening(false);
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
	 * @param selector the Selector multiplexor
	 * @throws IOException
	 */
	private void doSelect(ServerSocketChannel server, final Selector selector) throws IOException {
		while (this.isActive()) {
			int soTimeout = this.getSoTimeout();
			int selectionCount = 0;
			try {
				long timeout = soTimeout < 0 ? 0 : soTimeout;
				if (getDelayedReads().size() > 0 && (timeout == 0 || getReadDelay() < timeout)) {
					timeout = getReadDelay();
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Delayed reads:" + getDelayedReads().size() + " timeout " + timeout);
				}
				selectionCount = selector.select(timeout);
				this.processNioSelections(selectionCount, selector, server, this.channelMap);
			}
			catch (CancelledKeyException cke) {
				if (logger.isDebugEnabled()) {
					logger.debug("CancelledKeyException during Selector.select()");
				}
			}
			catch (ClosedSelectorException cse) {
				if (this.isActive()) {
					logger.error("Selector closed", cse);
					break;
				}
			}
		}
	}

	/**
	 * @param selector The selector.
	 * @param server The server socket channel.
	 * @param now The current time.
	 * @throws IOException Any IOException.
	 */
	@Override
	protected void doAccept(final Selector selector, ServerSocketChannel server, long now) throws IOException {
		logger.debug("New accept");
		SocketChannel channel = server.accept();
		if (this.isShuttingDown()) {
			if (logger.isInfoEnabled()) {
				logger.info("New connection from " + channel.socket().getInetAddress().getHostAddress()
						+ " rejected; the server is in the process of shutting down.");
			}
			channel.close();
		}
		else {
			try {
				channel.configureBlocking(false);
				Socket socket = channel.socket();
				setSocketAttributes(socket);
				TcpNioConnection connection = createTcpNioConnection(channel);
				if (connection == null) {
					return;
				}
				connection.setTaskExecutor(this.getTaskExecutor());
				connection.setLastRead(now);
				this.channelMap.put(channel, connection);
				channel.register(selector, SelectionKey.OP_READ, connection);
				connection.publishConnectionOpenEvent();
			}
			catch (Exception e) {
				logger.error("Exception accepting new connection", e);
				channel.close();
			}
		}
	}

	private TcpNioConnection createTcpNioConnection(SocketChannel socketChannel) {
		try {
			TcpNioConnection connection = this.tcpNioConnectionSupport
					.createNewConnection(socketChannel, true,
							this.isLookupHost(), this.getApplicationEventPublisher(), this.getComponentName());
			connection.setUsingDirectBuffers(this.usingDirectBuffers);
			TcpConnectionSupport wrappedConnection = wrapConnection(connection);
			this.initializeConnection(wrappedConnection, socketChannel.socket());
			return connection;
		}
		catch (Exception e) {
			logger.error("Failed to establish new incoming connection", e);
			return null;
		}
	}

	@Override
	public void stop() {
		this.setActive(false);
		if (this.selector != null) {
			try {
				this.selector.close();
			}
			catch (Exception e) {
				logger.error("Error closing selector", e);
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
		return serverChannel;
	}

	/**
	 * @return the usingDirectBuffers
	 */
	protected boolean isUsingDirectBuffers() {
		return usingDirectBuffers;
	}

	/**
	 * @return the connections
	 */
	protected Map<SocketChannel, TcpNioConnection> getConnections() {
		return channelMap;
	}

}
