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
package org.springframework.integration.ip.tcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;
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
 * single-use connections. For true asynchronous bi-directional communication, a pair of
 * inbound / outbound channel adapters should be used.
 * @author Gary Russell
 * @since 2.0
 *
 */
public class TcpInboundGateway extends MessagingGatewaySupport implements
		TcpListener, TcpSender, ClientModeCapable, OrderlyShutdownCapable {

	private volatile AbstractServerConnectionFactory serverConnectionFactory;

	private volatile AbstractClientConnectionFactory clientConnectionFactory;

	private final Map<String, TcpConnection> connections = new ConcurrentHashMap<String, TcpConnection>();

	private volatile boolean isClientMode;

	private volatile long retryInterval = 60000;

	private volatile ScheduledFuture<?> scheduledFuture;

	private volatile ClientModeConnectionManager clientModeConnectionManager;

	private volatile boolean active;

	private volatile boolean shuttingDown;

	private final AtomicInteger activeCount = new AtomicInteger();

	@Override
	public boolean onMessage(Message<?> message) {
		if (this.shuttingDown) {
			if (logger.isInfoEnabled()) {
				logger.info("Inbound message ignored; shutting down; " + message.toString());
			}
		}
		else {
			if (message instanceof ErrorMessage) {
				/*
				 * Socket errors are sent here so they can be conveyed to any waiting thread.
				 * There's not one here; simply ignore.
				 */
				return false;
			}
			this.activeCount.incrementAndGet();
			try {
				return doOnMessage(message);
			}
			finally {
				this.activeCount.decrementAndGet();
			}
		}
		return false;
	}

	private boolean doOnMessage(Message<?> message) {
		Message<?> reply = this.sendAndReceiveMessage(message);
		if (reply == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("null reply received for " + message + " nothing to send");
			}
			return false;
		}
		String connectionId = (String) message.getHeaders().get(IpHeaders.CONNECTION_ID);
		TcpConnection connection = null;
		if (connectionId != null) {
			connection = connections.get(connectionId);
		}
		if (connection == null) {
			publishNoConnectionEvent(message, connectionId);
			logger.error("Connection not found when processing reply " + reply + " for " + message);
			return false;
		}
		try {
			connection.send(reply);
		}
		catch (Exception e) {
			logger.error("Failed to send reply", e);
		}
		return false;
	}

	private void publishNoConnectionEvent(Message<?> message, String connectionId) {
		AbstractConnectionFactory cf = this.serverConnectionFactory != null ? this.serverConnectionFactory
				: this.clientConnectionFactory;
		ApplicationEventPublisher applicationEventPublisher = cf.getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(
				new TcpConnectionFailedCorrelationEvent(this, connectionId, new MessagingException(message)));
		}
	}

	/**
	 * @return true if the associated connection factory is listening.
	 */
	public boolean isListening() {
		return this.serverConnectionFactory == null ? false
				: this.serverConnectionFactory.isListening();
	}

	/**
	 * Must be {@link AbstractClientConnectionFactory} or {@link AbstractServerConnectionFactory}.
	 *
	 * @param connectionFactory the Connection Factory
	 */
	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "Connection factory must not be null");
		if (connectionFactory instanceof AbstractServerConnectionFactory) {
			this.serverConnectionFactory = (AbstractServerConnectionFactory) connectionFactory;
		} else if (connectionFactory instanceof AbstractClientConnectionFactory) {
			this.clientConnectionFactory = (AbstractClientConnectionFactory) connectionFactory;
		} else {
			throw new IllegalArgumentException("Connection factory must be either an " +
					"AbstractServerConnectionFactory or an AbstractClientConnectionFactory");
		}
		connectionFactory.registerListener(this);
		connectionFactory.registerSender(this);
	}

	@Override
	public void addNewConnection(TcpConnection connection) {
		connections.put(connection.getConnectionId(), connection);
	}

	@Override
	public void removeDeadConnection(TcpConnection connection) {
		connections.remove(connection.getConnectionId());
	}
	@Override
	public String getComponentType(){
		return "ip:tcp-inbound-gateway";
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.isClientMode) {
			Assert.notNull(this.clientConnectionFactory,
					"For client-mode, connection factory must be type='client'");
			Assert.isTrue(!this.clientConnectionFactory.isSingleUse(),
					"For client-mode, connection factory must have single-use='false'");
		}
	}

	@Override // protected by super#lifecycleLock
	protected void doStart() {
		super.doStart();
		if (!this.active) {
			this.active = true;
			this.shuttingDown = false;
			if (this.serverConnectionFactory != null) {
				this.serverConnectionFactory.start();
			}
			if (this.clientConnectionFactory != null) {
				this.clientConnectionFactory.start();
			}
			if (this.isClientMode) {
				ClientModeConnectionManager manager = new ClientModeConnectionManager(
						this.clientConnectionFactory);
				this.clientModeConnectionManager = manager;
				Assert.state(this.getTaskScheduler() != null, "Client mode requires a task scheduler");
				this.scheduledFuture = this.getTaskScheduler().scheduleAtFixedRate(manager, this.retryInterval);
			}
		}
	}

	@Override // protected by super#lifecycleLock
	protected void doStop() {
		super.doStop();
		if (this.active) {
			this.active = false;
			if (this.scheduledFuture != null) {
				this.scheduledFuture.cancel(true);
			}
			this.clientModeConnectionManager = null;
			if (this.clientConnectionFactory != null) {
				this.clientConnectionFactory.stop();
			}
			if (this.serverConnectionFactory != null) {
				this.serverConnectionFactory.stop();
			}
		}
	}

	/**
	 * @return the isClientMode
	 */
	@Override
	public boolean isClientMode() {
		return isClientMode;
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
		return retryInterval;
	}

	/**
	 * @param retryInterval
	 *            the retryInterval to set
	 */
	public void setRetryInterval(long retryInterval) {
		this.retryInterval = retryInterval;
	}

	@Override
	public boolean isClientModeConnected() {
		if (this.isClientMode && this.clientModeConnectionManager != null) {
			return this.clientModeConnectionManager.isConnected();
		} else {
			return false;
		}
	}

	@Override
	public void retryConnection() {
		if (this.active && this.isClientMode && this.clientModeConnectionManager != null) {
			this.clientModeConnectionManager.run();
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
