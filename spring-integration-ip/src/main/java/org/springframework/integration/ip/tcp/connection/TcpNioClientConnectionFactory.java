/*
 * Copyright 2002-2011 the original author or authors.
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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * A client connection factory that creates {@link TcpNioConnection}s.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpNioClientConnectionFactory extends
		AbstractClientConnectionFactory {

	private boolean usingDirectBuffers;
	
	private Selector selector;
	
	private Map<SocketChannel, TcpNioConnection> channelMap = new ConcurrentHashMap<SocketChannel, TcpNioConnection>();
	
	private BlockingQueue<SocketChannel> newChannels = new LinkedBlockingQueue<SocketChannel>();

	
	/**
	 * Creates a TcpNioClientConnectionFactory for connections to the host and port.
	 * @param host the host
	 * @param port the port
	 */
	public TcpNioClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	/**
	 * @throws Exception
	 * @throws IOException
	 * @throws SocketException
	 */
	protected TcpConnection getOrMakeConnection() throws Exception {
		int n = 0;
		while (this.selector == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (n++ > 600) {
				throw new Exception("Factory failed to start");
			}
		}
		TcpConnection theConnection = this.getTheConnection();
		if (theConnection != null && theConnection.isOpen()) {
			return theConnection;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Opening new socket channel connection to " + this.getHost() + ":" + this.getPort());
		}
		SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(this.getHost(), this.getPort()));
		setSocketAttributes(socketChannel.socket());
		TcpNioConnection connection = new TcpNioConnection(socketChannel, false, this.isLookupHost());
		connection.setUsingDirectBuffers(this.usingDirectBuffers);
		connection.setTaskExecutor(this.getTaskExecutor());
		TcpConnection wrappedConnection = wrapConnection(connection);
		initializeConnection(wrappedConnection, socketChannel.socket());
		socketChannel.configureBlocking(false);
		if (this.getSoTimeout() > 0) {
			connection.setLastRead(System.currentTimeMillis());
		}
		this.channelMap.put(socketChannel, connection);
		newChannels.add(socketChannel);
		selector.wakeup();
		return wrappedConnection;
	}

	/**
	 * When set to true, connections created by this factory attempt
	 * to use direct buffers where possible.
	 * @param usingDirectBuffers
	 * @see ByteBuffer
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	public void close() {
		if (this.selector != null) {
			this.selector.wakeup();
		}
	}

	public void run() {
		if (logger.isDebugEnabled()) {
			logger.debug("Read selector running for connections to " + this.getHost() + ":" + this.getPort());
		}
		try {
			this.selector = Selector.open();
			while (this.isActive()) {
				SocketChannel newChannel;
				int soTimeout = this.getSoTimeout();
				int selectionCount = 0;
				try {
					selectionCount = selector.select(soTimeout < 0 ? 0 : soTimeout);
				} catch (CancelledKeyException cke) {
					if (logger.isDebugEnabled()) {
						logger.debug("CancelledKeyException during Selector.select()");
					}
				}
				while ((newChannel = newChannels.poll()) != null) {
					try {
						newChannel.register(this.selector, SelectionKey.OP_READ, channelMap.get(newChannel));
					} catch (ClosedChannelException cce) {
						if (logger.isDebugEnabled()) {
							logger.debug("Channel closed before registering with selector for reading");
						}
					}
				}
				this.processNioSelections(selectionCount, selector, null, this.channelMap);
			}
		} catch (Exception e) {
			logger.error("Exception in read selector thread", e);
			this.setActive(false);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Read selector exiting for connections to " + this.getHost() + ":" + this.getPort());
		}
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

	/**
	 * @return the newChannels
	 */
	protected BlockingQueue<SocketChannel> getNewChannels() {
		return newChannels;
	}

}
