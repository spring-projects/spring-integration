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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.util.Assert;


/**
 * A client connection factory that creates {@link TcpNioConnection}s.
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 *
 */
public class TcpNioClientConnectionFactory extends
		AbstractClientConnectionFactory implements Runnable {

	private volatile boolean usingDirectBuffers;

	private volatile Selector selector;

	private final Map<SocketChannel, TcpNioConnection> channelMap = new ConcurrentHashMap<SocketChannel, TcpNioConnection>();

	private final BlockingQueue<SocketChannel> newChannels = new LinkedBlockingQueue<SocketChannel>();

	private volatile TcpNioConnectionSupport tcpNioConnectionSupport = new DefaultTcpNioConnectionSupport();

	/**
	 * Creates a TcpNioClientConnectionFactory for connections to the host and port.
	 * @param host the host
	 * @param port the port
	 */
	public TcpNioClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	@Override
	protected void checkActive() throws IOException {
		super.checkActive();
		int n = 0;
		while (this.selector == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (n++ > 600) {
				throw new IOException("Factory failed to start");
			}
		}
	}

	@Override
	protected TcpConnectionSupport buildNewConnection() throws Exception {
		SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(this.getHost(), this.getPort()));
		setSocketAttributes(socketChannel.socket());
		TcpNioConnection connection = this.tcpNioConnectionSupport.createNewConnection(
				socketChannel, false, this.isLookupHost(), this.getApplicationEventPublisher(), this.getComponentName());
		connection.setUsingDirectBuffers(this.usingDirectBuffers);
		connection.setTaskExecutor(this.getTaskExecutor());
		TcpConnectionSupport wrappedConnection = wrapConnection(connection);
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
	 * @param usingDirectBuffers The usingDirectBuffers to set.
	 * @see ByteBuffer
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	public void setTcpNioConnectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		Assert.notNull(tcpNioSupport, "TcpNioSupport must not be null");
		this.tcpNioConnectionSupport = tcpNioSupport;
	}

	@Override
	public void stop() {
		if (this.selector != null) {
			try {
				this.selector.close();
			}
			catch (Exception e) {
				logger.error("Error closing selector", e);
			}
		}
		super.stop();
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.isActive()) {
				this.setActive(true);
				this.getTaskExecutor().execute(this);
			}
		}
		super.start();
	}

	@Override
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
					long timeout = soTimeout < 0 ? 0 : soTimeout;
					if (getDelayedReads().size() > 0 && (timeout == 0 || getReadDelay() < timeout)) {
						timeout = getReadDelay();
					}
					selectionCount = selector.select(timeout);
				}
				catch (CancelledKeyException cke) {
					if (logger.isDebugEnabled()) {
						logger.debug("CancelledKeyException during Selector.select()");
					}
				}
				while ((newChannel = newChannels.poll()) != null) {
					try {
						newChannel.register(this.selector, SelectionKey.OP_READ, channelMap.get(newChannel));
					}
					catch (ClosedChannelException cce) {
						if (logger.isDebugEnabled()) {
							logger.debug("Channel closed before registering with selector for reading");
						}
					}
				}
				this.processNioSelections(selectionCount, selector, null, this.channelMap);
			}
		}
		catch (ClosedSelectorException cse) {
			if (this.isActive()) {
				logger.error("Selector closed", cse);
			}
		}
		catch (Exception e) {
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
