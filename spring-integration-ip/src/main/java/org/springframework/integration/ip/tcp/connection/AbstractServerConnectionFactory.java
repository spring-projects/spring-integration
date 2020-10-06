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

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Date;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Base class for all server connection factories. Server connection factories
 * listen on a port for incoming connections and create new TcpConnection objects
 * for each new connection.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractServerConnectionFactory extends AbstractConnectionFactory
		implements TcpServerConnectionFactory, SchedulingAwareRunnable, OrderlyShutdownCapable {

	private static final int DEFAULT_BACKLOG = 5;

	private int backlog = DEFAULT_BACKLOG;

	private String localAddress;

	private volatile boolean listening;

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
	@Nullable
	public SocketAddress getServerSocketAddress() {
		return null;
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
	public TcpConnection getConnection() {
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
		return this.listening;
	}

	protected boolean isShuttingDown() {
		return this.shuttingDown;
	}

	/**
	 * Transfers attributes such as (de)serializer, mapper etc to a new connection.
	 * For single use sockets, enforces a socket timeout (default 10 seconds) to prevent
	 * DoS attacks.
	 * @param connection The new connection.
	 * @param socket The new socket.
	 */
	protected void initializeConnection(TcpConnectionSupport connection, Socket socket) {
		TcpListener listener = getListener();
		if (listener != null) {
			connection.registerListener(listener);
		}
		connection.registerSenders(getSenders());
		connection.setMapper(getMapper());
		connection.setDeserializer(getDeserializer());
		connection.setSerializer(getSerializer());
		/*
		 * If we are configured
		 * for single use; need to enforce a timeout on the socket so we will close
		 * if the client connects, but sends nothing. (Protect against DoS).
		 * Behavior can be overridden by explicitly setting the timeout to zero.
		 */
		if (isSingleUse() && getSoTimeout() < 0) {
			try {
				socket.setSoTimeout(DEFAULT_REPLY_TIMEOUT);
			}
			catch (SocketException ex) {
				logger.error(ex, "Error setting default reply timeout");
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
	@Nullable
	public String getLocalAddress() {
		return this.localAddress;
	}

	/**
	 * Used on multi-homed systems to enforce the server to listen
	 * on a specific network address instead of all network adapters.
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
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(new TcpConnectionServerExceptionEvent(this, e));
		}
	}

	protected void publishServerListeningEvent(int port) {
		final ApplicationEventPublisher eventPublisher = getApplicationEventPublisher();
		if (eventPublisher != null) {
			final TcpConnectionServerListeningEvent event = new TcpConnectionServerListeningEvent(this, port);
			TaskScheduler taskScheduler = getTaskScheduler();
			if (taskScheduler != null) {
				try {
					taskScheduler.schedule(() -> eventPublisher.publishEvent(event), new Date());
				}
				catch (@SuppressWarnings("unused") TaskRejectedException e) {
					eventPublisher.publishEvent(event);
				}
			}
			else {
				eventPublisher.publishEvent(event);
			}
		}
	}

}
