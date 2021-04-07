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
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Abstract class for client connection factories; client connection factories
 * establish outgoing connections.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public abstract class AbstractClientConnectionFactory extends AbstractConnectionFactory {

	private static final long DEFAULT_CONNECT_TIMEOUT = 60L;

	private final ReadWriteLock theConnectionLock = new ReentrantReadWriteLock();

	private boolean manualListenerRegistration;

	private Duration connectTimeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT);

	@Nullable
	private Predicate<TcpConnectionSupport> connectionTest;

	private volatile TcpConnectionSupport theConnection;

	/**
	 * Construct a factory that will established connections to the host and port.
	 * @param host The host.
	 * @param port The port.
	 */
	public AbstractClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	/**
	 * Set the connection timeout in seconds. Defaults to 60.
	 * @param connectTimeout the timeout.
	 * @since 5.2
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = Duration.ofSeconds(connectTimeout);
	}

	protected Duration getConnectTimeout() {
		return this.connectTimeout;
	}


	/**
	 * Set whether to automatically (default) or manually add a {@link TcpListener} to the
	 * connections created by this factory. By default, the factory automatically configures
	 * the listener. When manual registration is in place, incoming messages will be delayed
	 * until the listener is registered.
	 * @since 1.4.5
	 */
	public void enableManualListenerRegistration() {
		this.manualListenerRegistration = true;
	}

	/**
	 * Get a {@link Predicate} that will be invoked to test a new connection; return true
	 * to accept the connection, false the reject.
	 * @return the predicate.
	 * @since 5.3
	 */
	@Nullable
	protected Predicate<TcpConnectionSupport> getConnectionTest() {
		return this.connectionTest;
	}

	/**
	 * Set a {@link Predicate} that will be invoked to test a new connection; return true
	 * to accept the connection, false the reject.
	 * @param connectionTest the predicate.
	 * @since 5.3
	 */
	public void setConnectionTest(@Nullable Predicate<TcpConnectionSupport> connectionTest) {
		this.connectionTest = connectionTest;
	}

	/**
	 * Obtain a connection - if {@link #setSingleUse(boolean)} was called with
	 * true, a new connection is returned; otherwise a single connection is
	 * reused for all requests while the connection remains open.
	 * @throws InterruptedException if interrupted.
	 */
	@Override
	public TcpConnectionSupport getConnection() throws InterruptedException {
		checkActive();
		return obtainConnection();
	}

	protected TcpConnectionSupport obtainConnection() throws InterruptedException {
		if (!isSingleUse()) {
			TcpConnectionSupport connection = obtainSharedConnection();
			if (connection != null) {
				return connection;
			}
		}
		return obtainNewConnection();
	}

	@Nullable
	protected final TcpConnectionSupport obtainSharedConnection() throws InterruptedException {
		this.theConnectionLock.readLock().lockInterruptibly();
		try {
			TcpConnectionSupport connection = getTheConnection();
			if (connection != null && connection.isOpen()) {
				return connection;
			}
		}
		finally {
			this.theConnectionLock.readLock().unlock();
		}
		return null;
	}

	protected final TcpConnectionSupport obtainNewConnection() throws InterruptedException {
		boolean singleUse = isSingleUse();
		if (!singleUse) {
			this.theConnectionLock.writeLock().lockInterruptibly();
		}
		try {
			TcpConnectionSupport connection;
			if (!singleUse) {
				// Another write lock holder might have created a new one by now.
				connection = obtainSharedConnection();
				if (connection != null && connection.isOpen()) {
					return connection;
				}
			}
			return doObtain(singleUse);
		}
		catch (RuntimeException e) {
			ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new TcpConnectionFailedEvent(this, e));
			}
			throw e;
		}
		finally {
			if (!singleUse) {
				this.theConnectionLock.writeLock().unlock();
			}
		}
	}

	private TcpConnectionSupport doObtain(boolean singleUse) {
		TcpConnectionSupport connection;
		if (logger.isDebugEnabled()) {
			logger.debug("Opening new socket connection to " + getHost() + ":" + getPort());
		}

		connection = buildNewConnection();
		if (this.connectionTest != null && !this.connectionTest.test(connection)) {
			connection.setTestFailed(true);
			connection.close();
			throw new UncheckedIOException(new IOException("Connection test failed for " + connection));
		}
		if (!singleUse) {
			setTheConnection(connection);
		}
		return connection;
	}

	protected TcpConnectionSupport buildNewConnection() {
		throw new UnsupportedOperationException(
				"Factories that don't override this class' obtainConnection() must implement this method");
	}

	/**
	 * Transfer attributes such as (de)serializers, singleUse etc to a new connection.
	 * When the connection factory has a reference to a TCPListener (to read
	 * responses), or for single use connections, the connection is executed.
	 * Single use connections need to read from the connection in order to
	 * close it after the socket timeout.
	 * @param connection The new connection.
	 * @param socket The new socket.
	 */
	protected void initializeConnection(TcpConnectionSupport connection, Socket socket) {
		if (this.manualListenerRegistration) {
			connection.enableManualListenerRegistration();
		}
		else {
			TcpListener listener = getListener();
			if (listener != null) {
				connection.registerListener(listener);
			}
		}
		connection.registerSenders(getSenders());
		connection.setMapper(getMapper());
		connection.setDeserializer(getDeserializer());
		connection.setSerializer(getSerializer());
		if (this.connectionTest != null) {
			connection.setNeedsTest(true);
		}
	}

	/**
	 * @param theConnection the theConnection to set
	 */
	protected void setTheConnection(TcpConnectionSupport theConnection) {
		this.theConnection = theConnection;
	}

	/**
	 * @return the theConnection
	 */
	@Nullable
	protected TcpConnectionSupport getTheConnection() {
		return this.theConnection;
	}

	/**
	 * Force close the connection and null the field if it's
	 * a shared connection.
	 * @param connection The connection.
	 */
	public void forceClose(TcpConnection connection) {
		if (Objects.equals(this.theConnection, connection)) {
			this.theConnection = null;
		}
		connection.close();
	}

}
