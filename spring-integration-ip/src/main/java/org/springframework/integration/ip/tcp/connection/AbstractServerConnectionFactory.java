/*
 * Copyright 2001-2015 the original author or authors.
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

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * Base class for all server connection factories. Server connection factories
 * listen on a port for incoming connections and create new TcpConnection objects
 * for each new connection.
 *
 * @author Gary Russell
 * @since 2.0
 */
public abstract class AbstractServerConnectionFactory
		extends AbstractConnectionFactory implements SchedulingAwareRunnable, OrderlyShutdownCapable {

	private static final int DEFAULT_BACKLOG = 5;

	private volatile boolean listening;

	private volatile String localAddress;

	private volatile int backlog = DEFAULT_BACKLOG;

	private volatile boolean shuttingDown;


	/**
	 * The port on which the factory will listen.
	 *
	 * @param port The port.
	 */
	public AbstractServerConnectionFactory(int port) {
		super(port);
	}

	@Override
	public boolean isLongLived() {
		return true;
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isActive()) {
				this.setActive(true);
				this.shuttingDown = false;
				getTaskExecutor().execute(this);
			}
		}
		super.start();
	}

	/**
	 * Not supported because the factory manages multiple connections and this
	 * method cannot discriminate.
	 */
	@Override
	public TcpConnection getConnection() throws Exception {
		throw new UnsupportedOperationException("Getting a connection from a server factory is not supported");
	}

	/**
	 * @param listening the listening to set
	 */
	protected void setListening(boolean listening) {
		this.listening = listening;
	}


	/**
	 *
	 * @return true if the server is listening on the port.
	 */
	public boolean isListening() {
		return listening;
	}

	protected boolean isShuttingDown() {
		return shuttingDown;
	}

	/**
	 * Transfers attributes such as (de)serializer, singleUse etc to a new connection.
	 * For single use sockets, enforces a socket timeout (default 10 seconds).
	 * @param connection The new connection.
	 * @param socket The new socket.
	 */
	protected void initializeConnection(TcpConnectionSupport connection, Socket socket) {
		TcpListener listener = getListener();
		if (listener != null) {
			connection.registerListener(listener);
		}
		connection.registerSender(getSender());
		connection.setMapper(getMapper());
		connection.setDeserializer(getDeserializer());
		connection.setSerializer(getSerializer());
		connection.setSingleUse(isSingleUse());
		/*
		 * If we are configured
		 * for single use; need to enforce a timeout on the socket so we will close
		 * if the client connects, but sends nothing. (Protect against DoS).
		 * Behavior can be overridden by explicitly setting the timeout to zero.
		 */
		if (isSingleUse() && getSoTimeout() < 0) {
			try {
				socket.setSoTimeout(DEFAULT_REPLY_TIMEOUT);
			} catch (SocketException e) {
				logger.error("Error setting default reply timeout", e);
			}
		}

	}

	protected void postProcessServerSocket(ServerSocket serverSocket) {
		getTcpSocketSupport().postProcessServerSocket(serverSocket);
	}

	/**
	 *
	 * @return the localAddress
	 */
	public String getLocalAddress() {
		return localAddress;
	}

	/**
	 * Used on multi-homed systems to enforce the server to listen
	 * on a specfic network address instead of all network adapters.
	 * @param localAddress the ip address of the required adapter.
	 */
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}


	/**
	 * The number of sockets in the server connection backlog.
	 * @return The backlog.
	 */
	public int getBacklog() {
		return this.backlog;
	}

	/**
	 * The number of sockets in the connection backlog. Default 5;
	 * increase if you expect high connection rates.
	 * @param backlog The backlog to set.
	 */
	public void setBacklog(int backlog) {
		Assert.isTrue(backlog >= 0, "You cannot set backlog negative");
		this.backlog = backlog;
	}

	@Override
	public int beforeShutdown() {
		this.shuttingDown = true;
		return 0;
	}

	@Override
	public int afterShutdown() {
		stop();
		return 0;
	}

	protected void publishServerExceptionEvent(Exception e) {
		getApplicationEventPublisher().publishEvent(new TcpConnectionServerExceptionEvent(this, e));
	}

}
