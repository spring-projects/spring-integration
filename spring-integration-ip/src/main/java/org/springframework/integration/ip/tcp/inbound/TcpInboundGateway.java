/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ip.tcp.inbound;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.ClientModeCapable;
import org.springframework.integration.ip.tcp.connection.ClientModeConnectionManager;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionFailedCorrelationEvent;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Inbound Gateway using a server connection factory - threading is controlled by the
 * factory. For java.net connections, each socket can process only one message at a time.
 * For java.nio connections, messages may be multiplexed but the client will need to
 * provide correlation logic. If the client is a {@link TcpOutboundGateway} multiplexing
 * is not used, but multiple concurrent connections can be used if the connection factory uses
 * single-use connections. For true asynchronous bidirectional communication, a pair of
 * inbound / outbound channel adapters should be used.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class TcpInboundGateway extends MessagingGatewaySupport implements
		TcpListener, TcpSender, ClientModeCapable, OrderlyShutdownCapable {

	/**
	 * A default retry interval in milliseconds - {@value #DEFAULT_RETRY_INTERVAL}.
	 */
	public static final long DEFAULT_RETRY_INTERVAL = 60000;

	private final Map<String, TcpConnection> connections = new ConcurrentHashMap<>();

	private final AtomicInteger activeCount = new AtomicInteger();

	private @Nullable AbstractServerConnectionFactory serverConnectionFactory;

	private @Nullable AbstractClientConnectionFactory clientConnectionFactory;

	private boolean isClientMode;

	private boolean isSingleUse;

	private long retryInterval = DEFAULT_RETRY_INTERVAL;

	private volatile @Nullable ClientModeConnectionManager clientModeConnectionManager;

	private volatile @Nullable ScheduledFuture<?> scheduledFuture;

	private volatile boolean shuttingDown;

	@Override
	public void onMessage(Message<?> message) {
		boolean isErrorMessage = message instanceof ErrorMessage;
		try {
			if (this.shuttingDown) {
				logger.info(() -> "Inbound message ignored; shutting down; " + message);
			}
			else {
				if (isErrorMessage) {
					/*
					 * Socket errors are sent here, so they can be conveyed to any waiting thread.
					 * There's not one here; simply ignore.
					 */
					return;
				}
				this.activeCount.incrementAndGet();
				try {
					doOnMessage(message);
				}
				finally {
					this.activeCount.decrementAndGet();
				}
			}
		}
		finally {
			String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
			if (connectionId != null && !isErrorMessage && this.isSingleUse) {
				if (this.serverConnectionFactory != null) {
					this.serverConnectionFactory.closeConnection(connectionId);
				}
				else if (this.clientConnectionFactory != null) {
					this.clientConnectionFactory.closeConnection(connectionId);
				}
			}
		}
	}

	private void doOnMessage(Message<?> message) {
		Message<?> reply = sendAndReceiveMessage(message);
		if (reply == null) {
			logger.debug(() -> "null reply received for " + message + " nothing to send");
			return;
		}
		String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		if (connectionId != null) {
			TcpConnection connection = this.connections.get(connectionId);
			if (connection == null) {
				publishNoConnectionEvent(message, connectionId);
				logger.error(() -> "Connection not found when processing reply " + reply + " for " + message);
				return;
			}
			try {
				connection.send(reply);
			}
			catch (Exception ex) {
				logger.error(ex, "Failed to send reply");
			}
		}
	}

	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	private void publishNoConnectionEvent(Message<?> message, String connectionId) {
		AbstractConnectionFactory cf =
				this.serverConnectionFactory != null
						? this.serverConnectionFactory
						: this.clientConnectionFactory;

		cf.getApplicationEventPublisher().publishEvent(
				new TcpConnectionFailedCorrelationEvent(this, connectionId,
						new MessagingException(message, "Connection not found to process reply.")));
	}

	/**
	 * @return true if the associated connection factory is listening.
	 */
	public boolean isListening() {
		return this.serverConnectionFactory != null && this.serverConnectionFactory.isListening();
	}

	/**
	 * Must be {@link AbstractClientConnectionFactory} or {@link AbstractServerConnectionFactory}.
	 * @param connectionFactory the Connection Factory
	 */
	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "Connection factory must not be null");
		if (connectionFactory instanceof AbstractServerConnectionFactory) {
			this.serverConnectionFactory = (AbstractServerConnectionFactory) connectionFactory;
		}
		else if (connectionFactory instanceof AbstractClientConnectionFactory) {
			this.clientConnectionFactory = (AbstractClientConnectionFactory) connectionFactory;
		}
		else {
			throw new IllegalArgumentException("Connection factory must be either an " +
					"AbstractServerConnectionFactory or an AbstractClientConnectionFactory");
		}
		connectionFactory.registerListener(this);
		connectionFactory.registerSender(this);
		this.isSingleUse = connectionFactory.isSingleUse();
	}

	@Override
	public void addNewConnection(TcpConnection connection) {
		this.connections.put(connection.getConnectionId(), connection);
	}

	@Override
	public void removeDeadConnection(TcpConnection connection) {
		this.connections.remove(connection.getConnectionId());
	}

	@Override
	public String getComponentType() {
		return "ip:tcp-inbound-gateway";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(this.serverConnectionFactory != null || this.clientConnectionFactory != null,
				"An 'AbstractConnectionFactory' must not be provided.");
		if (this.isClientMode) {
			Assert.notNull(this.clientConnectionFactory, "For client-mode, connection factory must be type='client'");
			Assert.isTrue(!this.clientConnectionFactory.isSingleUse(),
					"For client-mode, connection factory must have single-use='false'");
		}
	}

	@Override // protected by super#lifecycleLock
	protected void doStart() {
		super.doStart();
		this.shuttingDown = false;
		if (this.serverConnectionFactory != null) {
			this.serverConnectionFactory.start();
		}
		if (this.clientConnectionFactory != null) {
			this.clientConnectionFactory.start();
		}
		if (this.isClientMode && this.clientConnectionFactory != null) {
			ClientModeConnectionManager manager =
					new ClientModeConnectionManager(this.clientConnectionFactory);
			this.clientModeConnectionManager = manager;
			this.scheduledFuture = getTaskScheduler().scheduleAtFixedRate(manager, Duration.ofMillis(this.retryInterval));
		}
	}

	@Override // protected by super#lifecycleLock
	protected void doStop() {
		super.doStop();
		ScheduledFuture<?> scheduledFutureToCancel = this.scheduledFuture;
		if (scheduledFutureToCancel != null) {
			scheduledFutureToCancel.cancel(true);
		}
		this.clientModeConnectionManager = null;
		if (this.clientConnectionFactory != null) {
			this.clientConnectionFactory.stop();
		}
		if (this.serverConnectionFactory != null) {
			this.serverConnectionFactory.stop();
		}
	}

	/**
	 * @return the isClientMode
	 */
	@Override
	public boolean isClientMode() {
		return this.isClientMode;
	}

	/**
	 * @param isClientMode
	 *            the isClientMode to set
	 */
	public void setClientMode(boolean isClientMode) {
		this.isClientMode = isClientMode;
	}

	/**
	 * @return the retryInterval
	 */
	public long getRetryInterval() {
		return this.retryInterval;
	}

	/**
	 * Configure a retry interval.
	 * Defaults to {@link #DEFAULT_RETRY_INTERVAL}.
	 * @param retryInterval the retryInterval to set
	 */
	public void setRetryInterval(long retryInterval) {
		this.retryInterval = retryInterval;
	}

	@Override
	public boolean isClientModeConnected() {
		ClientModeConnectionManager clientModeConnectionManagerToCheck = this.clientModeConnectionManager;
		if (this.isClientMode && clientModeConnectionManagerToCheck != null) {
			return clientModeConnectionManagerToCheck.isConnected();
		}
		else {
			return false;
		}
	}

	@Override
	public void retryConnection() {
		ClientModeConnectionManager clientModeConnectionManagerToRun = this.clientModeConnectionManager;
		if (isActive() && this.isClientMode && clientModeConnectionManagerToRun != null) {
			clientModeConnectionManagerToRun.run();
		}
	}

	@Override
	public int beforeShutdown() {
		this.shuttingDown = true;
		return this.activeCount.get();
	}

	@Override
	public int afterShutdown() {
		this.stop();
		return this.activeCount.get();
	}

}
