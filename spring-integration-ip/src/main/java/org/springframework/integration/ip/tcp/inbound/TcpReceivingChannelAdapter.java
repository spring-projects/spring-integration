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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.ClientModeCapable;
import org.springframework.integration.ip.tcp.connection.ClientModeConnectionManager;
import org.springframework.integration.ip.tcp.connection.ConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Tcp inbound channel adapter using a TcpConnection to
 * receive data - if the connection factory is a server
 * factory, this Listener owns the connections. If it is
 * a client factory, the sender owns the connection.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpReceivingChannelAdapter
		extends MessageProducerSupport implements TcpListener, ClientModeCapable, OrderlyShutdownCapable {

	private @Nullable AbstractConnectionFactory clientConnectionFactory;

	private @Nullable AbstractConnectionFactory serverConnectionFactory;

	private volatile boolean isSingleUse;

	private volatile boolean isClientMode;

	private volatile long retryInterval = 60000; // NOSONAR magic number

	private volatile @Nullable ScheduledFuture<?> scheduledFuture;

	private volatile @Nullable ClientModeConnectionManager clientModeConnectionManager;

	private volatile boolean shuttingDown;

	private final AtomicInteger activeCount = new AtomicInteger();

	@Override
	public void onMessage(Message<?> message) {
		boolean isErrorMessage = message instanceof ErrorMessage;
		try {
			if (this.shuttingDown) {
				logger.info(() -> "Inbound message ignored; shutting down; " + message);
			}
			/*
			 * Socket errors are sent here so they can be conveyed to any waiting thread.
			 * There's not one here; simply ignore.
			 */
			else if (!isErrorMessage) {
				this.activeCount.incrementAndGet();
				try {
					sendMessage(message);
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
					// if there's no collaborating outbound adapter, close immediately, otherwise
					// it will close after sending the reply.
					if (this.serverConnectionFactory.getSender() == null) {
						this.serverConnectionFactory.closeConnection(connectionId);
					}
				}
				else if (this.clientConnectionFactory != null) {
					this.clientConnectionFactory.closeConnection(connectionId);
				}
			}
		}
	}

	@Override
	protected void onInit() {
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
		this.shuttingDown = false;
		if (this.serverConnectionFactory != null) {
			this.serverConnectionFactory.start();
		}
		if (this.clientConnectionFactory != null) {
			this.clientConnectionFactory.start();
		}
		if (this.isClientMode && this.clientConnectionFactory != null) {
			ClientModeConnectionManager manager = new ClientModeConnectionManager(this.clientConnectionFactory);
			this.clientModeConnectionManager = manager;
			this.scheduledFuture = getTaskScheduler().scheduleAtFixedRate(manager, Duration.ofMillis(this.retryInterval));
		}
	}

	@Override // protected by super#lifecycleLock
	protected void doStop() {
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
	 * Sets the client or server connection factory; for this (an inbound adapter), if
	 * the factory is a client connection factory, the sockets are owned by a sending
	 * channel adapter and this adapter is used to receive replies.
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		if (connectionFactory instanceof AbstractClientConnectionFactory) {
			this.clientConnectionFactory = connectionFactory;
		}
		else {
			this.serverConnectionFactory = connectionFactory;
		}
		connectionFactory.registerListener(this);
		this.isSingleUse = connectionFactory.isSingleUse();
	}

	public boolean isListening() {
		if (this.serverConnectionFactory == null) {
			return false;
		}
		if (this.serverConnectionFactory instanceof AbstractServerConnectionFactory) {
			return ((AbstractServerConnectionFactory) this.serverConnectionFactory).isListening();
		}
		return false;
	}

	@Override
	public String getComponentType() {
		return "ip:tcp-inbound-channel-adapter";
	}

	/**
	 * @return the clientConnectionFactory
	 */
	protected @Nullable ConnectionFactory getClientConnectionFactory() {
		return this.clientConnectionFactory;
	}

	/**
	 * @return the serverConnectionFactory
	 */
	protected @Nullable ConnectionFactory getServerConnectionFactory() {
		return this.serverConnectionFactory;
	}

	/**
	 * @return the isClientMode
	 */
	@Override
	public boolean isClientMode() {
		return this.isClientMode;
	}

	/**
	 * @param isClientMode the isClientMode to set
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
		stop();
		return this.activeCount.get();
	}

}
