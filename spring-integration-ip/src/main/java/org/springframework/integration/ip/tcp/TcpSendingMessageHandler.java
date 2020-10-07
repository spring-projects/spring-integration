/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.ClientModeCapable;
import org.springframework.integration.ip.tcp.connection.ClientModeConnectionManager;
import org.springframework.integration.ip.tcp.connection.ConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionFailedCorrelationEvent;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Tcp outbound channel adapter using a TcpConnection to
 * send data - if the connection factory is a server
 * factory, the TcpListener owns the connections. If it is
 * a client factory, this object owns the connection.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpSendingMessageHandler extends AbstractMessageHandler implements
		TcpSender, ManageableLifecycle, ClientModeCapable {

	/**
	 * A default retry interval for the {@link ClientModeConnectionManager} rescheduling.
	 */
	public static final long DEFAULT_RETRY_INTERVAL = 60000;

	protected final Object lifecycleMonitor = new Object(); // NOSONAR

	private final Map<String, TcpConnection> connections = new ConcurrentHashMap<>();

	private AbstractConnectionFactory clientConnectionFactory;

	private AbstractConnectionFactory serverConnectionFactory;

	private boolean isClientMode;

	private boolean isSingleUse;

	private long retryInterval = DEFAULT_RETRY_INTERVAL;

	private volatile ScheduledFuture<?> scheduledFuture;

	private volatile ClientModeConnectionManager clientModeConnectionManager;

	private volatile boolean active;

	protected TcpConnection obtainConnection(Message<?> message) {
		TcpConnection connection;
		Assert.notNull(this.clientConnectionFactory, "'clientConnectionFactory' cannot be null");
		try {
			connection = this.clientConnectionFactory.getConnection();
		}
		catch (Exception ex) {
			logger.error(ex, "Error creating connection");
			throw new MessageHandlingException(message, "Failed to obtain a connection in the [" + this + ']', ex);
		}
		return connection;
	}

	/**
	 * Writes the message payload to the underlying socket, using the specified
	 * message format.
	 * @see org.springframework.messaging.MessageHandler#handleMessage(org.springframework.messaging.Message)
	 */
	@Override
	public void handleMessageInternal(final Message<?> message) {
		if (this.serverConnectionFactory != null) {
			handleMessageAsServer(message);
		}
		else {
			handleMessageAsClient(message);
		}
	}

	private void handleMessageAsServer(Message<?> message) {
		// We don't own the connection, we are asynchronously replying
		String connectionId = message.getHeaders().get(IpHeaders.CONNECTION_ID, String.class);
		TcpConnection connection = null;
		if (connectionId != null) {
			connection = this.connections.get(connectionId);
		}
		if (connection != null) {
			try {
				connection.send(message);
			}
			catch (Exception ex) {
				logger.error(ex, "Error sending message");
				connection.close();
				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
						() -> "Error sending message in the [" + this + ']', ex);
			}
			finally {
				if (this.isSingleUse) { // close after replying
					connection.close();
				}
			}
		}
		else {
			logger.error(() -> "Unable to find outbound socket for " + message);
			MessageHandlingException messageHandlingException =
					new MessageHandlingException(message, "Unable to find outbound socket in the [" + this + ']');
			publishNoConnectionEvent(messageHandlingException, connectionId);
			throw messageHandlingException;
		}
	}

	private void handleMessageAsClient(Message<?> message) {
		// we own the connection
		TcpConnection connection = null;
		try {
			connection = doWrite(message);
		}
		catch (MessageHandlingException ex) {
			// retry - socket may have closed
			if (ex.getCause() instanceof IOException) {
				logger.debug(ex, "Fail on first write attempt");
				connection = doWrite(message);
			}
			else {
				throw ex;
			}
		}
		finally {
			if (connection != null && this.isSingleUse
					&& this.clientConnectionFactory.getListener() == null) {
				// if there's no collaborating inbound adapter, close immediately, otherwise
				// it will close after receiving the reply.
				connection.close();
			}
		}
	}

	/**
	 * Method that actually does the write.
	 * @param message The message to write.
	 * @return the connection.
	 */
	protected TcpConnection doWrite(Message<?> message) {
		TcpConnection connection = null;
		try {
			connection = obtainConnection(message);
			TcpConnection connectionToLog = connection;
			logger.debug(() -> "Got Connection " + connectionToLog.getConnectionId());
			connection.send(message);
		}
		catch (Exception ex) {
			final String connectionId;
			if (connection != null) {
				connectionId = connection.getConnectionId();
			}
			else {
				connectionId = null;
			}

			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
					() -> "Failed to handle message in the [" + this + "] using " + connectionId, ex);
		}
		return connection;
	}

	private void publishNoConnectionEvent(MessageHandlingException messageHandlingException, String connectionId) {
		AbstractConnectionFactory cf =
				this.serverConnectionFactory != null
						? this.serverConnectionFactory
						: this.clientConnectionFactory;
		ApplicationEventPublisher applicationEventPublisher = cf.getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(
					new TcpConnectionFailedCorrelationEvent(this, connectionId, messageHandlingException));
		}
	}

	/**
	 * Sets the client or server connection factory; for this (an outbound adapter), if
	 * the factory is a server connection factory, the sockets are owned by a receiving
	 * channel adapter and this adapter is used to send replies.
	 *
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory(AbstractConnectionFactory connectionFactory) {
		if (connectionFactory instanceof AbstractClientConnectionFactory) {
			this.clientConnectionFactory = connectionFactory;
		}
		else {
			this.serverConnectionFactory = connectionFactory;
			connectionFactory.registerSender(this);
		}
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
		return "ip:tcp-outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.isClientMode) {
			Assert.notNull(this.clientConnectionFactory, "For client-mode, connection factory must be type='client'");
			Assert.isTrue(!this.clientConnectionFactory.isSingleUse(),
					"For client-mode, connection factory must have single-use='false'");
		}
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.active) {
				this.active = true;
				if (this.clientConnectionFactory != null) {
					this.clientConnectionFactory.start();
				}
				if (this.serverConnectionFactory != null) {
					this.serverConnectionFactory.start();
				}
				if (this.isClientMode) {
					Assert.notNull(this.clientConnectionFactory,
							"For client-mode, connection factory must be type='client'");
					ClientModeConnectionManager manager =
							new ClientModeConnectionManager(this.clientConnectionFactory);
					this.clientModeConnectionManager = manager;
					TaskScheduler taskScheduler = getTaskScheduler();
					Assert.state(taskScheduler != null, "Client mode requires a task scheduler");
					this.scheduledFuture = taskScheduler.scheduleAtFixedRate(manager, this.retryInterval);
				}
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
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
	}

	@Override
	public boolean isRunning() {
		return this.active;
	}

	/**
	 * @return the clientConnectionFactory
	 */
	protected ConnectionFactory getClientConnectionFactory() {
		return this.clientConnectionFactory;
	}

	/**
	 * @return the serverConnectionFactory
	 */
	protected ConnectionFactory getServerConnectionFactory() {
		return this.serverConnectionFactory;
	}

	/**
	 * @return the connections
	 */
	protected Map<String, TcpConnection> getConnections() {
		return this.connections;
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
		if (this.isClientMode && this.clientModeConnectionManager != null) {
			return this.clientModeConnectionManager.isConnected();
		}
		else {
			return false;
		}
	}

	@Override
	public void retryConnection() {
		if (this.active && this.isClientMode && this.clientModeConnectionManager != null) {
			this.clientModeConnectionManager.run();
		}
	}

}
