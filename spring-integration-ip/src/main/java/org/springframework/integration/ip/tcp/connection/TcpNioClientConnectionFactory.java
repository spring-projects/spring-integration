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
import java.net.InetSocketAddress;
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

import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;


/**
 * A client connection factory that creates {@link TcpNioConnection}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpNioClientConnectionFactory extends
		AbstractClientConnectionFactory implements SchedulingAwareRunnable {

	private final Map<SocketChannel, TcpNioConnection> channelMap = new ConcurrentHashMap<>();

	private final BlockingQueue<SocketChannel> newChannels = new LinkedBlockingQueue<>();

	private boolean usingDirectBuffers;

	private TcpNioConnectionSupport tcpNioConnectionSupport = new DefaultTcpNioConnectionSupport();

	private volatile Selector selector;

	/**
	 * Creates a TcpNioClientConnectionFactory for connections to the host and port.
	 * @param host the host
	 * @param port the port
	 */
	public TcpNioClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	@Override
	protected void checkActive() {
		super.checkActive();
		int n = 0;
		while (this.selector == null) {
			try {
				Thread.sleep(100); // NOSONAR magic number
			}
			catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (n++ > 600) { // NOSONAR magic number
				throw new UncheckedIOException(new IOException("Factory failed to start"));
			}
		}
	}

	@Override
	protected TcpConnectionSupport buildNewConnection() {
		try {
			SocketChannel socketChannel = SocketChannel.open();
			setSocketAttributes(socketChannel.socket());
			connect(socketChannel);
			TcpNioConnection connection =
					this.tcpNioConnectionSupport.createNewConnection(socketChannel, false, isLookupHost(),
							getApplicationEventPublisher(), getComponentName());
			connection.setUsingDirectBuffers(this.usingDirectBuffers);
			connection.setTaskExecutor(getTaskExecutor());
			Integer sslHandshakeTimeout = getSslHandshakeTimeout();
			if (sslHandshakeTimeout != null && connection instanceof TcpNioSSLConnection) {
				((TcpNioSSLConnection) connection).setHandshakeTimeout(sslHandshakeTimeout);
			}
			TcpConnectionSupport wrappedConnection = wrapConnection(connection);
			if (!wrappedConnection.equals(connection)) {
				connection.setSenders(getSenders());
			}
			initializeConnection(wrappedConnection, socketChannel.socket());
			if (getSoTimeout() > 0) {
				connection.setLastRead(System.currentTimeMillis());
			}
			this.channelMap.put(socketChannel, connection);
			wrappedConnection.publishConnectionOpenEvent();
			this.newChannels.add(socketChannel);
			this.selector.wakeup();
			return wrappedConnection;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UncheckedIOException(new IOException(e));
		}
	}

	private void connect(SocketChannel socketChannel) throws IOException, InterruptedException {
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(getHost(), getPort()));
		boolean connected = socketChannel.finishConnect();
		long timeLeft = getConnectTimeout().toMillis();
		while (!connected && timeLeft > 0) {
			Thread.sleep(5); // NOSONAR Magic #
			connected = socketChannel.finishConnect();
			timeLeft -= 5; // NOSONAR Magic #
		}
		if (!connected) {
			throw new IOException("Not connected after connectTimeout");
		}
	}

	/**
	 * When set to true, connections created by this factory attempt
	 * to use direct buffers where possible.
	 * @param usingDirectBuffers The usingDirectBuffers to set.
	 * @see java.nio.ByteBuffer
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	public void setTcpNioConnectionSupport(TcpNioConnectionSupport tcpNioSupport) {
		Assert.notNull(tcpNioSupport, "TcpNioSupport must not be null");
		this.tcpNioConnectionSupport = tcpNioSupport;
	}

	@Override
	public boolean isLongLived() {
		return true;
	}

	@Override
	public void stop() {
		if (this.selector != null) {
			try {
				this.selector.close();
			}
			catch (Exception ex) {
				logger.error(ex, "Error closing selector");
			}
		}
		super.stop();
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isActive()) {
				setActive(true);
				getTaskExecutor().execute(this);
			}
		}
		super.start();
	}

	@Override
	public void run() {
		logger.debug(() -> "Read selector running for connections to " + getHost() + ':' + getPort());
		try {
			this.selector = Selector.open();
			while (isActive()) {
				processSelectorWhileActive();
			}
		}
		catch (ClosedSelectorException cse) {
			if (isActive()) {
				logger.error(cse, "Selector closed");
			}
		}
		catch (Exception ex) {
			logger.error(ex, "Exception in read selector thread");
			setActive(false);
		}
		logger.debug(() -> "Read selector exiting for connections to " + getHost() + ':' + getPort());
	}

	private void processSelectorWhileActive() throws IOException {
		SocketChannel newChannel;
		int soTimeout = getSoTimeout();
		int selectionCount = 0;
		try {
			long timeout = Math.max(soTimeout, 0);
			if (getDelayedReads().size() > 0 && (timeout == 0 || getReadDelay() < timeout)) {
				timeout = getReadDelay();
			}
			selectionCount = this.selector.select(timeout);
		}
		catch (@SuppressWarnings("unused") CancelledKeyException cke) {
			logger.debug("CancelledKeyException during Selector.select()");
		}
		while ((newChannel = this.newChannels.poll()) != null) {
			try {
				newChannel.register(this.selector, SelectionKey.OP_READ, this.channelMap.get(newChannel));
			}
			catch (@SuppressWarnings("unused") ClosedChannelException cce) {
				logger.debug("Channel closed before registering with selector for reading");
			}
		}
		processNioSelections(selectionCount, this.selector, null, this.channelMap);
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

	/**
	 * @return the newChannels
	 */
	protected BlockingQueue<SocketChannel> getNewChannels() {
		return this.newChannels;
	}

}
