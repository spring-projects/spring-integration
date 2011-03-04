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

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
/**
 * Implements a server connection factory that produces {@link TcpNioConnection}s using
 * a {@link ServerSocketChannel}. Must have a {@link TcpListener} registered.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNioServerConnectionFactory extends AbstractServerConnectionFactory {

	protected ServerSocketChannel serverChannel;
	
	protected boolean usingDirectBuffers;
	
	protected Map<SocketChannel, TcpNioConnection> connections = new HashMap<SocketChannel, TcpNioConnection>();
	
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
	 * Invokes {{@link #initializeConnection(TcpConnection, Socket)} and executes the 
	 * connection {@link TcpConnection#run()} using the task executor.
	 * I/O errors on the server socket/channel are logged and the factory is stopped.
	 */
	public void run() {
		if (this.listener == null) {
			logger.info("No listener bound to server connection factory; will not read; exiting...");
			return;
		}
		try {
			this.serverChannel = ServerSocketChannel.open();
			logger.info("Listening on port " + this.port);
			this.serverChannel.configureBlocking(false);
			if (this.localAddress == null) {
				this.serverChannel.socket().bind(new InetSocketAddress(this.port),
					Math.abs(this.poolSize));
			} else {
				InetAddress whichNic = InetAddress.getByName(this.localAddress);
				this.serverChannel.socket().bind(new InetSocketAddress(whichNic, this.port),
						Math.abs(this.poolSize));
			}
			final Selector selector = Selector.open();
			this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			this.listening = true;
			doSelect(this.serverChannel, selector);

		} catch (IOException e) {
			this.close();
			this.listening = false;
			if (this.active) {
				logger.error("Error on ServerSocketChannel", e);
				this.active = false;
			}
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
	 * @param server
	 * @param selector
	 * @throws IOException
	 * @throws ClosedChannelException
	 * @throws SocketException
	 */
	private void doSelect(ServerSocketChannel server, final Selector selector)
			throws IOException, ClosedChannelException, SocketException {
		while (this.active) {
			int selectionCount = selector.select(this.soTimeout);
			this.processNioSelections(selectionCount, selector, server, this.connections);			
		}
	}

	/**
	 * @param selector
	 * @param connections
	 * @param server
	 * @param now
	 * @throws IOException
	 * @throws SocketException
	 * @throws ClosedChannelException
	 */
	@Override
	protected void doAccept(final Selector selector, ServerSocketChannel server, long now) throws IOException {
		logger.debug("New accept");
		SocketChannel channel = server.accept();
		channel.configureBlocking(false);
		Socket socket = channel.socket();
		setSocketAttributes(socket);
		TcpNioConnection connection = createTcpNioConnection(channel);
		if (connection == null) {
			return;
		}
		connection.setTaskExecutor(this.taskExecutor);
		connection.setLastRead(now);
		connections.put(channel, connection);
		channel.register(selector, SelectionKey.OP_READ, connection);
	}

	private TcpNioConnection createTcpNioConnection(SocketChannel socketChannel) {
		try {
			TcpNioConnection connection = new TcpNioConnection(socketChannel, true, this.isLookupHost());
			connection.setUsingDirectBuffers(this.usingDirectBuffers);
			TcpConnection wrappedConnection = wrapConnection(connection);
			this.initializeConnection(wrappedConnection, socketChannel.socket());
			return connection;
		} catch (Exception e) {
			logger.error("Failed to establish new incoming connection", e);
			return null;
		}
	}

	public boolean isRunning() {
		return this.active;
	}

	public void close() {
		if (this.serverChannel == null) {
			return;
		}
		try {
			this.serverChannel.close();
		} catch (IOException e) {}
		this.serverChannel = null;
	}

	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}
	

}
